package hpbtc.processor;

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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageWriterImpl implements MessageWriter {

    private static Logger logger = Logger.getLogger(MessageWriterImpl.class.
            getName());
    private ByteBuffer currentWrite;
    private Register register;
    private Timer timer;
    private byte[] peerId;
    private byte[] protocol;
    private Random random;

    public MessageWriterImpl(final Register register, final Timer timer,
            final byte[] peerId, final byte[] protocol) {
        random = new Random();
        this.peerId = peerId;
        this.protocol = protocol;
        this.register = register;
        this.timer = timer;
    }

    public void disconnect(final Peer peer) throws IOException {
        register.disconnect(peer);
        Torrent torrent = peer.getTorrent();
        peer.disconnect();
        if (torrent.getRemainingPeers() < 3) {
            if (torrent.cancelTrackerTask()) {
                Tracker tracker = torrent.getTracker();
                long delay = tracker.getMinInterval() * 1000 -
                        System.currentTimeMillis() +
                        tracker.getLastTrackerContact();
                torrent.setTrackerTask(scheduleTrackerTask(torrent,
                        delay < 0L ? 0L : delay));
            }
        }
    }

    private TimerTask scheduleTrackerTask(final Torrent torrent,
            final long delay) {
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                torrent.updateTracker();
                contactFreshPeers(torrent);
            }
        };
        timer.schedule(tt, delay, torrent.getTracker().getInterval() * 1000);
        return tt;
    }

    public void download(final Torrent torrent) {
        torrent.beginTracker();
        contactFreshPeers(torrent);
        scheduleTrackerTask(torrent, torrent.getTracker().getInterval() * 1000);
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                List<SimpleMessage> result = decideChoking(torrent);
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
                        logger.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }, 10000L, 10000L);
    }

    private List<SimpleMessage> decideChoking(final Torrent torrent) {
        List<Peer> prs = new ArrayList<Peer>(torrent.getConnectedPeers());
        Comparator<Peer> comp = torrent.isTorrentComplete() ? new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countUploaded() - p1.countUploaded();
            }
        }
                : new Comparator<Peer>() {

            public int compare(Peer p1, Peer p2) {
                return p2.countDownloaded() - p1.countDownloaded();
            }
        };
        if (torrent.increaseOptimisticCounter() == 3 &&
                !prs.isEmpty()) {
            Peer optimisticPeer = prs.remove(random.nextInt(prs.size()));
            Collections.sort(prs, comp);
            prs.add(0, optimisticPeer);
            torrent.setOptimisticCounter(0);
        } else {
            Collections.sort(prs, comp);
        }
        int k = 0;
        List<SimpleMessage> result = new LinkedList<SimpleMessage>();
        for (Peer p : prs) {
            if (k < 4) {
                if (p.isClientChoking()) {
                    SimpleMessage mUnchoke = new SimpleMessage(
                            SimpleMessage.TYPE_UNCHOKE, p);
                    result.add(mUnchoke);
                }
                if (p.isPeerInterested()) {
                    k++;
                }
            } else if (!p.isClientChoking()) {
                SimpleMessage mChoke = new SimpleMessage(
                        SimpleMessage.TYPE_CHOKE, p);
                result.add(mChoke);
            }
            p.resetCounters();
        }
        return result;
    }

    private void keepAliveWrite(final Peer peer) {
        peer.cancelKeepAliveWrite();
        TimerTask tt = new TimerTask() {

            @Override
            public void run() {
                try {
                    postMessage(new LengthPrefixMessage(0, peer));
                } catch (IOException e) {
                    logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                }
            }
        };
        timer.schedule(tt, 90000);
        peer.setKeepAliveWrite(tt);
    }

    public void writeNext(final Peer peer) throws IOException {
        keepAliveWrite(peer);
        if (currentWrite == null || currentWrite.remaining() == 0) {
            LengthPrefixMessage sm = null;
            sm = peer.getMessageToSend();
            currentWrite = sm.send();
            currentWrite.rewind();
            logger.fine("Sending: " + sm);
        }
        peer.upload(currentWrite);
        if (currentWrite.remaining() > 0 || peer.isMessagesToSendEmpty()) {
            register.clearWrite(peer);
        }
    }

    public void cancelPieceMessage(final int begin, final int index,
            final int length, final Peer peer) {
        Iterable<LengthPrefixMessage> q = peer.listMessagesToSend();
        if (q != null) {
            Iterator<LengthPrefixMessage> i = q.iterator();
            while (i.hasNext()) {
                LengthPrefixMessage m = i.next();
                if (m instanceof PieceMessage) {
                    PieceMessage pm = (PieceMessage) m;
                    if (pm.getIndex() == index && pm.getBegin() == begin &&
                            pm.getLength() == length) {
                        i.remove();
                        Peer p = pm.getDestination();
                        Torrent t = p.getTorrent();
                        p.removeRequest(pm.getIndex(), pm.getBegin(),
                                t.getChunkSize());
                    }
                }
            }
        }
    }

    public void cancelPieceMessage(final Peer peer) {
        Iterable<LengthPrefixMessage> q = peer.listMessagesToSend();
        if (q != null) {
            Iterator<LengthPrefixMessage> i = q.iterator();
            while (i.hasNext()) {
                LengthPrefixMessage m = i.next();
                if (m instanceof PieceMessage) {
                    i.remove();
                    PieceMessage pm = (PieceMessage) m;
                    Peer p = pm.getDestination();
                    Torrent t = p.getTorrent();
                    peer.removeRequest(pm.getIndex(), pm.getBegin(),
                            t.getChunkSize());
                }
            }
        }
    }

    public void postMessage(final LengthPrefixMessage message) throws 
            IOException {
        message.getDestination().addMessageToSend(message);
        register.registerWrite(message.getDestination());
    }

    private void contactFreshPeers(final Torrent torrent) {
        Iterable<Peer> freshPeers = torrent.getFreshPeers();
        for (Peer peer : freshPeers) {
            try {
                LengthPrefixMessage m = new HandshakeMessage(peerId, protocol,
                        peer, torrent.getInfoHash());
                postMessage(m);
                peer.setHandshakeSent();
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }
    }

    public void connect(final Peer peer) throws IOException {
        register.registerRead(peer);
        register.registerWrite(peer);
        keepAliveRead(peer);
    }
    
    public void keepAliveRead(final Peer peer) {
        if (peer.cancelKeepAliveRead()) {
            TimerTask tt = new TimerTask() {

                @Override
                public void run() {
                    try {
                        disconnect(peer);
                        logger.info("Disconnect " + peer);
                    } catch (IOException e) {
                        logger.log(Level.WARNING, e.getLocalizedMessage(), e);
                    }
                }
            };
            timer.schedule(tt, 120000);
            peer.setKeepAliveRead(tt);
        }
    }
}
