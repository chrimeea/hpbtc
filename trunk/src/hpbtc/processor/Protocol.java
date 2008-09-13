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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private Timer timer;
    private MessageWriter writer;
    private MessageReader processor;
    private int port;
    private NetworkReader netReader;
    private NetworkWriter netWriter;
    private byte[] protocol;

    public Protocol() throws UnsupportedEncodingException {
        this.peerId = TorrentUtil.generateId();
        torrents = new HashMap<byte[], Torrent>();
        timer = new Timer(true);
        requests = new HashMap<byte[], BitSet[]>();
        trackers = new HashMap<byte[], Tracker>();
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
        for (int i = 0; i < np; i++) {
            req[i] = new BitSet(ti.getChunksInPiece());
        }
        requests.put(infoHash, req);
        beginPeers(ti);
        timer.schedule(new TimerTask() {

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
                        }
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, ex.getLocalizedMessage(), ex);
                    }
                }
            }
        }, 10000);
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

    private void beginPeers(final Torrent ti)
            throws UnsupportedEncodingException, IOException {
        final Tracker tracker = new Tracker(ti.getInfoHash(), peerId, port,
                ti.getTrackers(), ti.getByteEncoding());
        trackers.put(ti.getInfoHash(), tracker);
        contactFreshPeers(tracker.beginTracker(ti.getFileLength()));
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Set<Peer> p = tracker.updateTracker(null, ti.getUploaded(),
                        ti.getDownloaded(),
                        ti.getFileLength() - ti.getDownloaded(), true);
                p.removeAll(ti.getConnectedPeers());
                contactFreshPeers(p);
            }
        }, tracker.getInterval() * 1000);
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
