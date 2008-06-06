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
import java.io.EOFException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
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
    private TorrentRepository torrentRep;
    private Network network;
    private Map<InetSocketAddress, Peer> peerInfo;
    private byte[] peerId;
    private PieceRepository pieceRep;

    public Protocol() {
        torrentRep = new TorrentRepository();
        network = new Network();
        peerInfo = new HashMap<InetSocketAddress, Peer>();
        pieceRep = new PieceRepository();
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
                                network.closeConnection(message.getPeerAddress());
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
            peerInfo.remove(data.getPeerAddress());
            return;
        }
        ByteBuffer current = ByteBuffer.wrap(data.getMessage());
        Peer peer = peerInfo.get(data.getPeerAddress());
        do {
            if (peer != null) {
                int len = current.getInt();
                if (len > 0) {
                    byte disc = current.get();
                    if (current.remaining() < len) {
                        throw new EOFException("wrong message");
                    }
                    switch (disc) {
                        case SimpleMessage.TYPE_BITFIELD:
                            BitfieldMessage mBit = new BitfieldMessage(current, len);
                            processBitfield(mBit.getBitfield(), len, peer);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(current, SimpleMessage.TYPE_CANCEL);
                            processCancel(mCan.getBegin(), mCan.getIndex(), mCan.getLength(), peer);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            processChoke(peer);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(current);
                            processHave(mHave.getIndex(), peer);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            processInterested(peer);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            processNotInterested(peer);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(current, len);
                            processPiece(mPiece.getBegin(), mPiece.getIndex(), mPiece.getPiece(), peer);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(current, SimpleMessage.TYPE_REQUEST);
                            processRequest(mReq.getBegin(), mReq.getIndex(), mReq.getLength(), peer);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            processUnchoke(peer);
                    }
                    peer.setMessagesReceived();
                }
            } else {
                HandshakeMessage mHand = new HandshakeMessage(current);
                peer = new Peer(data.getPeerAddress(), mHand.getPeerId());
                peer.setInfoHash(mHand.getInfoHash());
                processHandshake(mHand.getProtocol(), peer);
            }
        } while (current.remaining() > 0);
    }

    private void processHandshake(byte[] protocol, Peer peer) throws IOException {
        byte[] infoHash = peer.getInfoHash();
        if (Arrays.equals(protocol, getSupportedProtocol()) && torrentRep.haveTorrent(infoHash)) {
            peerInfo.put(peer.getAddress(), peer);
            HandshakeMessage reply = new HandshakeMessage(infoHash, peerId, protocol);
            network.postMessage(peer.getAddress(), reply);
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
    
    private void processBitfield(BitSet pieces, int len, Peer peer) throws IOException {
        if (peer.isMessagesReceived()) {
            throw new IOException("wrong message");
        } else {
            long nrPieces = torrentRep.getNrPieces(peer.getInfoHash());
            if (pieces.length() > nrPieces || len != Math.ceil(nrPieces / 8.0)) {
                throw new IOException("wrong message");
            }
        }
        peer.setPieces(pieces);
    }

    private void processCancel(int begin, int index, int length, Peer peer) throws IOException {
        byte[] infoHash = peer.getInfoHash();
        if (index >= torrentRep.getNrPieces(infoHash) ||
            begin >= torrentRep.getPieceLength(infoHash)) {
            throw new IOException("wrong message");
        }
        network.cancelPieceMessage(begin, index, length, peer.getAddress());
    }

    private void processChoke(Peer peer) {
        peer.setPeerChoking(true);
    }

    private void processHave(int index, Peer peer) throws IOException {
        if (index >= torrentRep.getNrPieces(peer.getInfoHash())) {
            throw new IOException("wrong message");
        }
        peer.setPiece(index);
    }

    private void processInterested(Peer peer) {
        peer.setPeerInterested(true);
    }

    private void processNotInterested(Peer peer) {
        peer.setPeerInterested(false);
    }

    private void processPiece(int begin, int index, ByteBuffer piece, Peer peer) throws IOException {
        byte[] infoHash = peer.getInfoHash();
        if (index >= torrentRep.getNrPieces(infoHash) ||
            begin >= torrentRep.getPieceLength(infoHash)) {
            throw new IOException("wrong message");
        }
        pieceRep.savePiece(infoHash, begin, index, piece);
    }
    
    private void processRequest(int begin, int index, int length, Peer peer) throws IOException {
        byte[] infoHash = peer.getInfoHash();
        long pieceLength = torrentRep.getPieceLength(infoHash);
        if (index >= torrentRep.getNrPieces(infoHash) ||
            begin >= pieceLength || begin + length > pieceLength ||
            !pieceRep.isPiece(infoHash, index)) {
            throw new IOException("wrong message");
        }
        ByteBuffer piece = pieceRep.getPiece(infoHash, begin, index, length);
        PieceMessage pm = new PieceMessage(begin, index, piece);
        network.postMessage(peer.getAddress(), pm);
    }

    private void processUnchoke(Peer peer) {
        peer.setPeerChoking(false);
    }
}
