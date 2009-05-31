package hpbtc.protocol.processor;

import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.protocol.torrent.Tracker;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
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
    protected Selector selector;
    private Selector selectorread;
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

    public void disconnect(final Peer peer) throws IOException {
        register.disconnect((SelectableChannel) peer.getChannel());
        Torrent torrent = peer.getTorrent();
        peer.disconnect();
        logger.info("Disconnected " + peer);
        if (torrent != null && torrent.getRemainingPeers() == 5) {
            if (torrent.cancelTrackerTask()) {
                Tracker tracker = torrent.getTracker();
                long delay = tracker.getMinInterval() * 1000 -
                        System.currentTimeMillis() +
                        tracker.getLastTrackerContact();
                scheduleTrackerTask(torrent, delay < 0L ? 0L : delay);
            }
        }
    }

    private void scheduleTrackerTask(final Torrent torrent, final long delay) {
        final TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                torrent.updateTracker();
                contactFreshPeers(torrent);
            }
        };
        timer.schedule(tt, delay, torrent.getTracker().getInterval() * 1000);
        torrent.setTrackerTask(tt);
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
                scheduleTrackerTask(torrent,
                        torrent.getTracker().getInterval() * 1000L);
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

    private void keepAliveWrite(final Peer peer) {
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

    public void setLimit(long l) {
        limit.set(2 * l);
    }

    public void writeNext(final Peer peer) throws IOException {
        long t = System.currentTimeMillis();
        if (t - timestamp > 2000L) {
            lastUploaded = uploaded;
            timestamp = t;
        }
        long l = limit.get() - uploaded + lastUploaded;
        if (l > 0) {
            if (currentWrite == null || currentWrite.remaining() == 0) {
                LengthPrefixMessage sm = peer.getMessageToSend();
                if (sm != null) {
                    if (sm instanceof PieceMessage) {
                        PieceMessage pm = (PieceMessage) sm;
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
                if (currentWrite.remaining() > l) {
                    currentWrite.limit(currentWrite.position() + (int) l);
                }
                uploaded += peer.upload(currentWrite);
                currentWrite.limit(currentWrite.capacity() - 1);
            }
            if (currentWrite != null && currentWrite.remaining() == 0 &&
                    peer.isMessagesToSendEmpty()) {
                register.registerNow((SelectableChannel) peer.getChannel(), selector,
                        0, peer);
            }
        }
    }

    public void postMessage(final LengthPrefixMessage message) throws
            IOException {
        final Peer peer = message.getDestination();
        peer.addMessageToSend(message);
        int op = SelectionKey.OP_WRITE;
        if (peer.getChannel() == null && !peer.connect()) {
            op = SelectionKey.OP_CONNECT | op;
        }
        register.registerNow((SelectableChannel) peer.getChannel(), selector,
                op, peer);
    }

    private void contactFreshPeers(final Torrent torrent) {
        final Iterable<Peer> freshPeers = torrent.getFreshPeers();
        synchronized (freshPeers) {
            for (Peer peer : freshPeers) {
                try {
                    peer.setHandshakeSent();
                    postMessage(new HandshakeMessage(peerId, protocol, peer,
                            torrent.getInfoHash()));
                } catch (Exception e) {
                    logger.log(Level.FINE, e.getLocalizedMessage(), e);
                }
            }
        }
    }

    public void connect(final Peer peer) throws IOException {
        register.registerNow((SelectableChannel) peer.getChannel(), selectorread,
                SelectionKey.OP_READ, peer);
        register.registerNow((SelectableChannel) peer.getChannel(), selector,
                SelectionKey.OP_WRITE, peer);
        keepAliveRead(peer);
    }

    public void keepAliveRead(final Peer peer) {
        if (peer.cancelKeepAliveRead()) {
            final TimerTask tt = new TimerTask() {

                @Override
                public void run() {
                    try {
                        disconnect(peer);
                    } catch (IOException e) {
                        logger.log(Level.FINE, e.getLocalizedMessage(), e);
                    }
                }
            };
            timer.schedule(tt, 120000);
            peer.setKeepAliveRead(tt);
        }
    }

    public void performWriteRegistration() {
        register.performRegistration(selector);
    }

    public void setWriteSelector(final Selector selector) {
        this.selector = selector;
    }

    public void setReadSelector(final Selector selector) {
        selectorread = selector;
    }
}
