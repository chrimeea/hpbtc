package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.network.RawMessage;
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class Protocol {

    private static Logger logger = Logger.getLogger(Protocol.class.getName());
    private TorrentRepository torrentRep;
    private Network network;
    private PeerRepository peerRep;
    private byte[] peerId;

    public Protocol() {
        torrentRep = new TorrentRepository();
        network = new Network();
        peerRep = new PeerRepository();
        generateId();
    }

    private void generateId() {
        peerId = new byte[20];
        peerId[0] = 'C';
        peerId[1] = 'M';
        peerId[2] = '-';
        peerId[3] = '2';
        peerId[4] = '.';
        peerId[5] = '0';
        Random r = new Random();
        r.nextBytes(peerId);
    }

    public void download(String fileName) throws IOException, NoSuchAlgorithmException {
        torrentRep.addTorrent(fileName);
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
            peerRep.removePeer(data.getPeer());
            return;
        }
        ByteBuffer current = ByteBuffer.wrap(data.getMessage());
        InetSocketAddress address = data.getPeer();
        do {
            if (peerRep.isHandshakeReceived(address)) {
                int len = current.getInt();
                if (len > 0) {
                    byte disc = current.get();
                    if (current.remaining() < len) {
                        throw new EOFException("wrong message");
                    }
                    switch (disc) {
                        case SimpleMessage.TYPE_BITFIELD:
                            BitfieldMessage mBit = new BitfieldMessage(current, len);
                            processBitfield(mBit.getBitfield(), len, address);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(current, SimpleMessage.TYPE_CANCEL);
                            processCancel(mCan.getBegin(), mCan.getIndex(), mCan.getLength(), address);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            processChoke(address);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(current);
                            processHave(mHave.getIndex(), address);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            processInterested(address);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            processNotInterested(address);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(current, len);
                            processPiece(mPiece.getBegin(), mPiece.getIndex(), mPiece.getPiece(), address);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(current, SimpleMessage.TYPE_REQUEST);
                            processRequest(mReq.getBegin(), mReq.getIndex(), mReq.getLength(), address);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            processUnchoke(address);
                    }
                    peerRep.setMessagesReceived(address);
                }
            } else {
                HandshakeMessage mHand = new HandshakeMessage(current);
                processHandshake(mHand.getInfoHash(), mHand.getPeerId(), mHand.getProtocol(), data.getPeer());
            }
        } while (current.remaining() > 0);
    }

    private void processHandshake(byte[] infoHash, byte[] pid, byte[] protocol, InetSocketAddress address) throws IOException {
        if (Arrays.equals(protocol, getSupportedProtocol()) && torrentRep.haveTorrent(infoHash)) {
            peerRep.addPeer(address, pid, infoHash);
            HandshakeMessage reply = new HandshakeMessage(infoHash, peerId, protocol);
            network.postMessage(address, reply);
        } else {
            throw new IOException("wrong message");
        }
    }

    private byte[] getSupportedProtocol() throws UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes("US-ASCII"));
        return protocol;
    }
    
    private void processBitfield(BitSet pieces, int len, InetSocketAddress address) throws IOException {
        if (peerRep.isMessagesReceived(address)) {
            throw new IOException("wrong message");
        } else {
            long nrPieces = torrentRep.getNrPieces(peerRep.getInfoHash(address));
            if (pieces.length() > nrPieces || len != Math.ceil(nrPieces / 8.0)) {
                throw new IOException("wrong message");
            }
        }
        peerRep.setPieces(address, pieces);
    }

    private void processCancel(int begin, int index, int length, InetSocketAddress address) throws IOException {
        byte[] infoHash = peerRep.getInfoHash(address);
        if (index >= torrentRep.getNrPieces(infoHash) ||
            begin >= torrentRep.getPieceLength(infoHash)) {
            throw new IOException("wrong message");
        }
        network.cancelPieceMessage(begin, index, length, address);
    }

    private void processChoke(InetSocketAddress address) {
        peerRep.setPeerChoking(address, true);
    }

    private void processHave(int index, InetSocketAddress address) {
    }

    private void processInterested(InetSocketAddress address) {
        peerRep.setPeerInterested(address, true);
    }

    private void processNotInterested(InetSocketAddress address) {
        peerRep.setPeerInterested(address, false);
    }

    private void processPiece(int begin, int index, ByteBuffer piece, InetSocketAddress address) {
    }

    private void processRequest(int begin, int index, int length, InetSocketAddress address) {
    }

    private void processUnchoke(InetSocketAddress address) {
        peerRep.setPeerChoking(address, false);
    }
}
