package hpbtc.processor;

import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.NetworkReader;
import hpbtc.protocol.network.NetworkWriter;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.protocol.torrent.Tracker;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    private Timer slowTimer;
    private Timer fastTimer;
    private MessageWriter writer;
    private MessageReader processor;
    private int port;
    private NetworkReader netReader;
    private NetworkWriter netWriter;
    private State state;
    private Random random;

    public Protocol() throws UnsupportedEncodingException {
        slowTimer = new Timer(true);
        fastTimer = new Timer(true);
        state = new State();
        random = new Random();
    }

    public void download(final File fileName, final String rootFolder)
            throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        final Torrent ti = new Torrent(fis, rootFolder);
        fis.close();
        final Tracker tracker = new Tracker(ti.getInfoHash(), state.getPeerId(),
                port, ti.getTrackers(), ti.getByteEncoding());
        state.addTorrent(ti, tracker);
        ti.addFreshPeers(tracker.beginTracker(ti.getFileLength()));
        long d = tracker.getInterval() * 1000;
        slowTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                contactFreshPeers(ti.getFreshPeers());
            }
        }, 0, d);
        fastTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                ti.addFreshPeers(tracker.updateTracker(null, ti.getUploaded(),
                        ti.getDownloaded(),
                        ti.getFileLength() - ti.getDownloaded(), true));
            }
        }, d, d);
        fastTimer.schedule(new TimerTask() {
            
            @Override
            public void run() {
                List<SimpleMessage> result = decideChoking(ti);
                for (SimpleMessage sm : result) {
                    Peer p = sm.getDestination();
                    try {
                        writer.postMessage(sm);
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
        }, 10000, 10000);
    }

    private void contactFreshPeers(final Iterable<Peer> freshPeers) {
        for (Peer peer : freshPeers) {
            try {
                SimpleMessage m = new HandshakeMessage(peer.getInfoHash(),
                        state.getPeerId(), state.getProtocol(), peer);
                writer.postMessage(m);
                peer.setHandshakeSent();
            } catch (Exception e) {
                logger.log(Level.WARNING, e.getLocalizedMessage(), e);
            }
        }
    }

    public void stopProtocol() {
        netReader.disconnect();
        netWriter.disconnect();
    }

    public void startProtocol() throws IOException {
        Register register = new Register(fastTimer);
        writer = new MessageWriterImpl(state, register);
        netWriter = new NetworkWriter(writer, register);
        processor = new MessageReaderImpl(state, register, writer);
        netReader = new NetworkReader(processor, register);
        port = netReader.connect();
        netWriter.connect();
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
        if (state.increaseOptimisticCounter(torrent) == 3 && !prs.isEmpty()) {
            Peer optimisticPeer = prs.remove(random.nextInt(prs.size()));
            Collections.sort(prs, comp);
            prs.add(0, optimisticPeer);
            state.resetOptimisticCounter(torrent);
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
}
