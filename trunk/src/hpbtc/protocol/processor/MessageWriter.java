package hpbtc.protocol.processor;

import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageWriter {

    private static Logger logger = Logger.getLogger(
            MessageWriter.class.getName());
    protected Register register;
    private Timer timer;
    private byte[] peerId;
    private byte[] protocol;
    private Random random;

    public MessageWriter(final Register register, final Timer timer,
            final byte[] peerId, final byte[] protocol) {
        random = new Random();
        this.peerId = peerId;
        this.protocol = protocol;
        this.register = register;
        this.timer = timer;
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

    public void keepAliveWrite(final Peer peer) throws InvalidPeerException {
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
