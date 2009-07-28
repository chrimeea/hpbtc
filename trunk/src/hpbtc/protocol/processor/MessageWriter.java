package hpbtc.protocol.processor;

import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageWriter {

    private static Logger logger = Logger.getLogger(
            MessageWriter.class.getName());
    private ByteBuffer currentWrite;
    protected Register register;
    private Timer timer;
    private byte[] peerId;
    private byte[] protocol;
    private Random random;
    private long uploaded;
    private AtomicLong limit;
    private long timestamp;
    private long lastUploaded;

    public MessageWriter(final Register register, final Timer timer,
            final byte[] peerId, final byte[] protocol) {
        random = new Random();
        this.peerId = peerId;
        this.protocol = protocol;
        this.register = register;
        this.timer = timer;
        this.timestamp = System.currentTimeMillis();
        this.limit = new AtomicLong(Long.MAX_VALUE);
    }

    public void stopTorrent(final Torrent torrent) throws IOException {
        torrent.cancelTrackerTask();
        Set<Peer> peers = new HashSet<Peer>(torrent.getConnectedPeers());
        for (Peer peer : peers) {
            register.disconnect((SelectableChannel) peer.getChannel());
            peer.disconnect();
        }
    }

    public void disconnect(final Peer peer)
            throws IOException, InvalidPeerException {
        register.disconnect((SelectableChannel) peer.getChannel());
        final Torrent torrent = peer.getTorrent();
        peer.disconnect();
        logger.info("Disconnected " + peer);
        if (torrent != null && torrent.getRemainingPeers() < 20 &&
                !torrent.hasTrackerTask()) {
            scheduleTrackerTask(torrent,
                    torrent.getTracker().getRemainingMillisUntilUpdate());
        }
    }

    private void scheduleTrackerTask(final Torrent torrent, final long delay) {
        final TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                torrent.updateTracker();
                contactFreshPeers(torrent);
                torrent.setTrackerTask(null);
            }
        };
        torrent.setTrackerTask(tt);
        timer.schedule(tt, delay);
    }

    public void download(final Torrent torrent) {
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                final List<SimpleMessage> result = decideChoking(torrent);
                for (SimpleMessage sm : result) {
                    Peer p = sm.getDestination();
                    try {
                        postMessage(sm);
                        if (sm.getMessageType() == SimpleMessage.TYPE_UNCHOKE) {
                            p.setClientChoking(false);
                        } else if (sm.getMessageType() ==
                                SimpleMessage.TYPE_CHOKE) {
                            p.setClientChoking(true);
                        }
                    } catch (IOException ex) {
                        logger.log(Level.FINE, ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }, 10000L, 10000L);
        final TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                torrent.beginTracker();
                contactFreshPeers(torrent);
            }
        };
        timer.schedule(tt, 0L);
    }

    private List<SimpleMessage> decideChoking(final Torrent torrent) {
        Set<Peer> peers = torrent.getConnectedPeers();
        List<Peer> prs;
        synchronized (peers) {
            prs = new ArrayList<Peer>(peers);
        }
        final Comparator<Peer> comp = torrent.countRemainingPieces() == 0 ? new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countUploaded() - p1.countUploaded();
            }
        }
                : new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countDownloaded() - p1.countDownloaded();
            }
        };
        if (torrent.increaseOptimisticCounter() == 3 && !prs.isEmpty()) {
            final Peer optimisticPeer = prs.remove(random.nextInt(prs.size()));
            Collections.sort(prs, comp);
            prs.add(0, optimisticPeer);
            torrent.setOptimisticCounter(0);
        } else {
            Collections.sort(prs, comp);
        }
        int k = 0;
        final List<SimpleMessage> result = new LinkedList<SimpleMessage>();
        for (Peer p : prs) {
            if (k < 4) {
                if (p.isClientChoking()) {
                    result.add(new SimpleMessage(SimpleMessage.TYPE_UNCHOKE, p));
                }
                if (p.isPeerInterested()) {
                    k++;
                }
            } else if (!p.isClientChoking()) {
                result.add(new SimpleMessage(SimpleMessage.TYPE_CHOKE, p));
            }
            p.resetCounters();
        }
        return result;
    }

    private void keepAliveWrite(final Peer peer) throws InvalidPeerException {
        peer.cancelKeepAliveWrite();
        final TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                try {
                    postMessage(new LengthPrefixMessage(0, peer));
                } catch (IOException e) {
                    logger.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        };
        timer.schedule(tt, 90000L);
        peer.setKeepAliveWrite(tt);
    }

    /**
     * Set the maximum number of uploaded bytes per second
     */
    public void setLimit(long l) {
        limit.set(l);
    }

    public boolean writeNext(final Peer peer)
            throws IOException, InvalidPeerException {
        int i = 0;
        final long t = System.currentTimeMillis();

        //check if more than 1 second passed since we measured
        // the bytes uploaded
        if (t - timestamp > 1000L) {

            //memorize the bytes uploaded so far
            lastUploaded = uploaded;
            timestamp = t;
        }

        // limit - (uploaded - lastUploaded)
        // computes how many bytes we can still upload this second without
        // going over the limit
        final long l = limit.get() - uploaded + lastUploaded;

        // ignore the call to writeNext if the bytes uploaded in this second
        // are more than the upload limit
        // we will continue to ignore the writeNext calls until
        // a second has passed and we can upload again
        if (l > 0) {
            //we still may upload maximum l bytes until we reach the limit
            if (currentWrite == null || currentWrite.remaining() == 0) {
                final LengthPrefixMessage sm = peer.getMessageToSend();
                if (sm != null) {
                    if (sm instanceof PieceMessage) {
                        final PieceMessage pm = (PieceMessage) sm;
                        pm.setPiece(peer.getTorrent().loadPiece(pm.getBegin(),
                                pm.getIndex(), pm.getLength()));
                    }
                    currentWrite = sm.send();
                    currentWrite.rewind();
                    logger.fine("Sending: " + sm);
                }
            }
            if (currentWrite != null && currentWrite.remaining() > 0) {
                keepAliveWrite(peer);

                //limit the message so that we don't go over the upload limit
                if (currentWrite.remaining() > l) {
                    currentWrite.limit(currentWrite.position() + (int) l);
                }

                //upload the message
                i = peer.upload(currentWrite);
                uploaded += i;

                //clear the limit
                currentWrite.limit(currentWrite.capacity() - 1);
            }
            if (currentWrite != null && currentWrite.remaining() == 0 &&
                    peer.isMessagesToSendEmpty()) {
                register.registerNow((SelectableChannel) peer.getChannel(),
                        Register.SELECTOR_TYPE.TCP_WRITE, 0, peer);
                return false;
            }
        }
        return i > 0;
    }

    public void postMessage(final LengthPrefixMessage message) throws
            IOException {
        final Peer peer = message.getDestination();
        peer.addMessageToSend(message);
        int op = SelectionKey.OP_WRITE;
        if (peer.getChannel() == null && !peer.connect()) {
            op = SelectionKey.OP_CONNECT | op;
        }
        register.registerNow((SelectableChannel) peer.getChannel(),
                Register.SELECTOR_TYPE.TCP_WRITE, op, peer);
    }

    public void contactFreshPeers(final Torrent torrent) {
        final Iterable<Peer> freshPeers = torrent.getFreshPeers();
        synchronized (freshPeers) {
            for (Peer peer : freshPeers) {
                try {
                    postMessage(new HandshakeMessage(peerId, protocol, peer,
                            torrent.getInfoHash()));
                    peer.setHandshakeSent();
                } catch (Exception e) {
                    logger.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
    }

    public void connect(final Peer peer) throws IOException {
        try {
            keepAliveRead(peer);
        } catch (InvalidPeerException ex) {
            throw new IOException(ex);
        }
    }

    public void keepAliveRead(final Peer peer) throws InvalidPeerException {
        if (peer.cancelKeepAliveRead()) {
            final TimerTask tt = new TimerTask() {

                @Override
                public void run() {
                    try {
                        disconnect(peer);
                    } catch (Exception e) {
                        logger.log(Level.FINE, e.getLocalizedMessage(), e);
                    }
                }
            };
            timer.schedule(tt, 120000L);
            peer.setKeepAliveRead(tt);
        }
    }
}
