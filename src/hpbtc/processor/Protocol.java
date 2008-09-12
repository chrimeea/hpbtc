package hpbtc.processor;

import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
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
    private byte[] peerId;
    private Timer timer;
    private MessageWriter writer;
    private MessageReader processor;
    private int port;

    public Protocol() throws UnsupportedEncodingException {
        this.peerId = TorrentUtil.generateId();
        torrents = new HashMap<byte[], Torrent>();
        timer = new Timer(true);
        requests = new HashMap<byte[], BitSet[]>();
        writer = new PeerWriter();
        processor = new MessageProcessor(writer, torrents, peerId,
                requests);
    }

    public void download(File fileName, String rootFolder) throws IOException,
            NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        final Torrent ti = new Torrent(fis, rootFolder, peerId, port);
        byte[] infoHash = ti.getInfoHash();
        fis.close();
        torrents.put(infoHash, ti);
        int np = ti.getNrPieces();
        BitSet[] req = new BitSet[np];
        for (int i = 0; i < np; i++) {
            req[i] = new BitSet();
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

    private void beginPeers(Torrent ti) throws UnsupportedEncodingException,
            IOException {
        ti.beginTracker();
        for (Peer peer : ti.getPeers()) {
            if (!peer.isConnected()) {
                SimpleMessage m = new HandshakeMessage(peer.getInfoHash(),
                        peerId, getSupportedProtocol(), peer);
                writer.postMessage(m);
            }
        }
    }

    public void stopProtocol() {
        processor.disconnect();
        writer.disconnect();
    }

    public void startProtocol() throws IOException {
        port = processor.connect();
        writer.connect();
    }

    public static byte[] getSupportedProtocol() throws
            UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes("ISO-8859-1"));
        return protocol;
    }
}
