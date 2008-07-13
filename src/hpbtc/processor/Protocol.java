package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    private Map<byte[], Torrent> torrents;
    private Network network;
    private MessageProcessor processor;
    private byte[] peerId;

    public Protocol() throws UnsupportedEncodingException {
        this.peerId = generateId();
        torrents = new HashMap<byte[], Torrent>();
        byte[] protocol = getSupportedProtocol();
        this.network = new Network();
        processor = new MessageProcessor(network, protocol, torrents, peerId);
    }

    public void download(File fileName, String rootFolder) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        Torrent ti = new Torrent(fis, rootFolder, peerId, network);
        byte[] infoHash = ti.getInfoHash();
        torrents.put(infoHash, ti);
        fis.close();
        torrents.put(infoHash, ti);
        beginPeers(ti);
    }

    private void beginPeers(Torrent ti) throws UnsupportedEncodingException, IOException {
        ti.beginTracker();
        for (Peer peer : ti.getPeers()) {
            if (!peer.isConnected()) {
                SimpleMessage m = new HandshakeMessage(peer.getInfoHash(), peerId, getSupportedProtocol());
                network.postMessage(peer, m);
                if (!peer.getOtherPieces(ti.getCompletePieces()).isEmpty()) {
                    m = new SimpleMessage(SimpleMessage.TYPE_INTERESTED);
                    network.postMessage(peer, m);
                }
            }
        }
    }

    private byte[] generateId() {
        byte[] pid = new byte[20];
        pid[0] = 'C';
        pid[1] = 'M';
        pid[2] = '-';
        pid[3] = '2';
        pid[4] = '.';
        pid[5] = '0';
        Random r = new Random();
        r.nextBytes(pid);
        return pid;
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

    private void process(RawMessage data) throws IOException, NoSuchAlgorithmException {
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
                            BitfieldMessage mBit = new BitfieldMessage(current, len);
                                processor.processBitfield(mBit, peer);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(current, SimpleMessage.TYPE_CANCEL);
                                processor.processCancel(mCan, peer);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            processor.processChoke(peer);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(current);
                                processor.processHave(mHave, peer);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            processor.processInterested(peer);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            processor.processNotInterested(peer);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(current, len);
                                processor.processPiece(mPiece, peer);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(current, SimpleMessage.TYPE_REQUEST);
                                processor.processRequest(mReq, peer);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            processor.processUnchoke(peer);
                    }
                    peer.setMessagesReceived();
                }
            } else if (current.remaining() >= 47) {
                HandshakeMessage mHand = new HandshakeMessage(current);
                    processor.processHandshake(mHand, peer);
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
