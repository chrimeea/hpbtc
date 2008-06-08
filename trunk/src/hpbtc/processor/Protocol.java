package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.RawMessage;
import hpbtc.protocol.torrent.FileStore;
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
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    private Map<byte[], Torrent> torrents;
    private Network network;
    private MessageValidator validator;
    private MessageProcessor processor;
    private FileStore fileStore;

    public Protocol() throws UnsupportedEncodingException {
        torrents = new HashMap<byte[], Torrent>();
        byte[] protocol = getSupportedProtocol();
        this.network = new Network();
        validator = new MessageValidator(torrents, protocol);
        processor = new MessageProcessor(network, protocol, torrents);
        this.fileStore = new FileStore();
    }

    public void download(File fileName, File rootFolder) throws IOException, NoSuchAlgorithmException {
        FileInputStream fis = new FileInputStream(fileName);
        Torrent ti = new Torrent(fileStore, fis, rootFolder);
        torrents.put(ti.getInfoHash(), ti);
        fis.close();
        torrents.put(ti.getInfoHash(), ti);
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

    private void process(RawMessage data) throws IOException {
        if (data.isDisconnect()) {
            return;
        }
        ByteBuffer current = ByteBuffer.wrap(data.getMessage());
        Peer peer = data.getPeer();
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
                            if (validator.validateBitfieldMessage(mBit, peer)) {
                                processor.processBitfield(mBit, peer);
                            }
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(current, SimpleMessage.TYPE_CANCEL);
                            if (validator.validateCancelMessage(mCan, peer)) {
                                processor.processCancel(mCan, peer);
                            }
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            processor.processChoke(peer);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(current);
                            if (validator.validateHaveMessage(mHave, peer)) {
                                processor.processHave(mHave, peer);
                            }
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            processor.processInterested(peer);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            processor.processNotInterested(peer);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(current, len);
                            if (validator.validatePieceMessage(mPiece, peer)) {
                                processor.processPiece(mPiece, peer);
                            }
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(current, SimpleMessage.TYPE_REQUEST);
                            if (validator.validateRequestMessage(mReq, peer)) {
                                processor.processRequest(mReq, peer);
                            }
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            processor.processUnchoke(peer);
                    }
                    peer.setMessagesReceived();
                }
            } else if (current.remaining() >= 47) {
                HandshakeMessage mHand = new HandshakeMessage(current);
                if (validator.validateHandshakeMessage(mHand, peer)) {
                    processor.processHandshake(mHand, peer);
                }
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
