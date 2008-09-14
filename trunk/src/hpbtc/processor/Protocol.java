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
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    private Map<byte[], Torrent> torrents;
    private Map<byte[], BitSet[]> requests;
    private Map<byte[], Tracker> trackers;
    private byte[] peerId;
    private Timer slowTimer;
    private Timer fastTimer;
    private MessageWriter writer;
    private MessageReader processor;
    private int port;
    private NetworkReader netReader;
    private NetworkWriter netWriter;
    private byte[] protocol;

    public Protocol() throws UnsupportedEncodingException {
        this.peerId = TorrentUtil.generateId();
        torrents = new Hashtable<byte[], Torrent>();
        slowTimer = new Timer(true);
        fastTimer = new Timer(true);
        requests = new Hashtable<byte[], BitSet[]>();
        trackers = new Hashtable<byte[], Tracker>();
        protocol = getSupportedProtocol();
    }

    public void download(final File fileName, final String rootFolder)
            throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        final Torrent ti = new Torrent(fis, rootFolder);
        byte[] infoHash = ti.getInfoHash();
        fis.close();
        torrents.put(infoHash, ti);
        int np = ti.getNrPieces();
        BitSet[] req = new BitSet[np];
        int cip = TorrentUtil.computeChunksInNotLastPiece(ti.getPieceLength(),
                ti.getChunkSize());
        for (int i = 0; i < np - 1; i++) {
            req[i] = new BitSet(cip);
        }
        req[np - 1] = new BitSet(TorrentUtil.computeChunksInLastPiece(ti.
                getFileLength(), np, ti.getChunkSize()));
        requests.put(infoHash, req);
        final Tracker tracker = new Tracker(ti.getInfoHash(), peerId, port,
                ti.getTrackers(), ti.getByteEncoding());
        trackers.put(ti.getInfoHash(), tracker);
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
                List<SimpleMessage> result = ti.decideChoking();
                for (SimpleMessage sm : result) {
                    Peer p = sm.getDestination();
                    try {
                        writer.postMessage(sm);
                        if (sm.getMessageType() == SimpleMessage.TYPE_UNCHOKE) {
                            p.setClientChoking(false);
                        } else if (sm.getMessageType() ==
                                SimpleMessage.TYPE_CHOKE) {
                            p.setClientChoking(true);
                            writer.cancelPieceMessage(p);
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
                        peerId, getSupportedProtocol(), peer);
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
        Register register = new Register();
        writer = new MessageWriterImpl(torrents, register);
        netWriter = new NetworkWriter(writer, register);
        processor = new MessageReaderImpl(torrents, peerId, requests, writer,
                trackers, protocol);
        netReader = new NetworkReader(processor, register);
        port = netReader.connect();
        netWriter.connect();
    }

    private static byte[] getSupportedProtocol() throws
            UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes("US-ASCII"));
        return protocol;
    }
}
