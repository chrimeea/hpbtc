package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.PeerNetwork;
import hpbtc.protocol.network.RawMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.EOFException;
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
    private Network network;
    private MessageProcessor processor;
    private byte[] peerId;
    private Timer timer;

    public Protocol() throws UnsupportedEncodingException {
        this.peerId = TorrentUtil.generateId();
        torrents = new HashMap<byte[], Torrent>();
        byte[] protocol = getSupportedProtocol();
        timer = new Timer(true);
        this.network = new PeerNetwork();
        requests = new HashMap<byte[], BitSet[]>();
        processor = new MessageProcessor(network, protocol, torrents, peerId,
                requests);
    }

    public void download(File fileName, String rootFolder) throws IOException,
            NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        final Torrent ti = new Torrent(fis, rootFolder, peerId, network.getPort());
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
                        network.postMessage(sm);
                        if (sm.getMessageType() == SimpleMessage.TYPE_UNCHOKE) {
                            p.setClientChoking(false);
                        } else if (sm.getMessageType() ==
                                SimpleMessage.TYPE_CHOKE) {
                            p.setClientChoking(true);
                        }
                    } catch (IOException ex) {
                        logger.warning(ex.getLocalizedMessage());
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
                        peerId, getSupportedProtocol());
                network.postMessage(m);
            }
        }
    }

    public void stopProtocol() {
        network.disconnect();
    }

    public void startProtocol() throws IOException {
        network.connect();
        new Thread(new Runnable() {

            public void run() {
                synchronized (network) {
                    do {
                        do {
                            try {
                                network.wait();
                            } catch (InterruptedException e) {
                            }
                            if (!network.isRunning()) {
                                return;
                            }
                        } while (!network.hasUnreadMessages());
                        RawMessage message = null;
                        try {
                            message = network.takeMessage();
                            process(message);
                        } catch (NoSuchAlgorithmException noe) {
                            logger.severe(noe.getLocalizedMessage());
                        } catch (IOException ioe) {
                            logger.warning(ioe.getLocalizedMessage());
                            try {
                                network.closeConnection(message.getPeer());
                            } catch (IOException e) {
                                logger.warning(e.getLocalizedMessage());
                            }
                        }
                    } while (network.hasUnreadMessages());
                }
            }
        }).start();
    }

    private void process(RawMessage data) throws IOException,
            NoSuchAlgorithmException {
        Peer peer = data.getPeer();
        if (data.isDisconnect()) {
            processor.processDisconnect(peer);
        }
        ByteBuffer current = ByteBuffer.wrap(data.getMessage());
        do {
            if (peer.isHandshakeReceived()) {
                int len = current.getInt();
                if (len > 0) {
                    byte disc = current.get();
                    if (current.remaining() < len) {
                        throw new EOFException("wrong message");
                    }
                    switch (disc) {
                        case SimpleMessage.TYPE_BITFIELD:
                            BitfieldMessage mBit = new BitfieldMessage(current,
                                    len, peer);
                            processor.processBitfield(mBit);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(current,
                                    SimpleMessage.TYPE_CANCEL, peer);
                            processor.processCancel(mCan);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            processor.processChoke(peer);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(current, peer);
                            processor.processHave(mHave);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            processor.processInterested(peer);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            processor.processNotInterested(peer);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(current, len,
                                    peer);
                            processor.processPiece(mPiece);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(current,
                                    SimpleMessage.TYPE_REQUEST, peer);
                            processor.processRequest(mReq);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            processor.processUnchoke(peer);
                    }
                    peer.setMessagesReceived();
                }
            } else if (current.remaining() >= 47) {
                HandshakeMessage mHand = new HandshakeMessage(current, peer);
                processor.processHandshake(mHand);
            }
        } while (current.remaining() > 0);
    }

    private byte[] getSupportedProtocol() throws UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes("US-ASCII"));
        return protocol;
    }
}
