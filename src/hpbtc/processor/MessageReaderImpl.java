package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.NetworkReader;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageReaderImpl implements MessageReader {

    private static Logger logger = Logger.getLogger(MessageReaderImpl.class.
            getName());
    private MessageWriter writer;
    private byte[] peerId;
    private Map<byte[], Torrent> torrents;
    private MessageValidator validator;
    private Map<byte[], BitSet[]> requests;
    private List<byte[]> expectBody;

    public MessageReaderImpl(Map<byte[], Torrent> torrents, byte[] peerId,
            Map<byte[], BitSet[]> requests, MessageWriter writer) {
        this.writer = writer;
        this.peerId = peerId;
        this.torrents = torrents;
        this.requests = requests;
        validator = new MessageValidator(torrents);
        this.expectBody = new LinkedList<byte[]>();
    }
    
    public void readMessage(Peer peer) throws IOException,
            NoSuchAlgorithmException {
        if (peer.isHandshakeReceived()) {
            if (peer.getId() == null) {
                peer.setNextDataExpectation(20);
                if (peer.download()) {
                    peer.setId(peer.getData().array());
                    logger.info("Received id for " + peer);
                }
            } else {
                if (!expectBody.contains(peer.getInfoHash())) {
                    peer.setNextDataExpectation(4);
                    if (peer.download()) {
                        int len = peer.getData().getInt();
                        if (len < 0) {
                            throw new IOException();
                        } else {
                            expectBody.add(peer.getInfoHash());
                            peer.setNextDataExpectation(len);
                        }
                    } else {
                        return;
                    }
                }
                if (peer.download()) {
                    ByteBuffer data = peer.getData();
                    byte disc = data.get();
                    logger.info("Received message type " + disc + " from " +
                            peer);
                    switch (disc) {
                        case SimpleMessage.TYPE_BITFIELD:
                            BitfieldMessage mBit = new BitfieldMessage(
                                    data, peer);
                            processBitfield(mBit);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(data,
                                    SimpleMessage.TYPE_CANCEL, peer);
                            processCancel(mCan);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            processChoke(peer);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(data,
                                    peer);
                            processHave(mHave);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            processInterested(peer);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            processNotInterested(peer);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(data, peer);
                            processPiece(mPiece);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(data,
                                    SimpleMessage.TYPE_REQUEST, peer);
                            processRequest(mReq);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            processUnchoke(peer);
                    }
                    peer.setMessagesReceived();
                }
            }
        } else {
            peer.setNextDataExpectation(48);
            if (peer.download()) {
                logger.info("Received handshake from " + peer);
                HandshakeMessage mHand = new HandshakeMessage(peer.getData(),
                        peer);
                processHandshake(mHand);
            }
        }
    }

    private void processHandshake(HandshakeMessage message) throws
            IOException {
        Peer peer = message.getDestination();
        if (validator.validateHandshakeMessage(message)) {
            byte[] infoHash = message.getInfoHash();
            peer.setInfoHash(infoHash);
            peer.setHandshakeReceived();
            Torrent t = torrents.get(peer.getInfoHash());
            if (!peer.isHandshakeSent()) {
                HandshakeMessage reply = new HandshakeMessage(infoHash, peerId,
                        Protocol.getSupportedProtocol(), peer);
                writer.postMessage(reply);
            } else {
                t.addPeer(peer);
            }
            BitSet bs = t.getCompletePieces();
            if (bs.cardinality() > 0) {
                BitfieldMessage bmessage = new BitfieldMessage(bs,
                        t.getNrPieces(), peer);
                writer.postMessage(bmessage);
            }
        } else {
            writer.closeConnection(peer);
        }
    }

    private void processBitfield(BitfieldMessage message) throws
            IOException {
        if (validator.validateBitfieldMessage(message)) {
            Peer peer = message.getDestination();
            peer.setPieces(message.getBitfield());
            Torrent t = torrents.get(peer.getInfoHash());
            if (!peer.getOtherPieces(t.getCompletePieces()).isEmpty()) {
                SimpleMessage smessage = new SimpleMessage(
                        SimpleMessage.TYPE_INTERESTED, peer);
                writer.postMessage(smessage);
                peer.setClientInterested(true);
            }
        }
    }

    private void processCancel(BlockMessage message) {
        if (validator.validateCancelMessage(message)) {
            Peer peer = message.getDestination();
            writer.cancelPieceMessage(message.getBegin(),
                    message.getIndex(), message.getLength(), peer);
        }
    }

    private void processChoke(Peer peer) {
        peer.setPeerChoking(true);
    }

    private void processHave(HaveMessage message) throws IOException {
        if (validator.validateHaveMessage(message)) {
            Peer peer = message.getDestination();
            int index = message.getIndex();
            peer.setPiece(index);
            Torrent t = torrents.get(peer.getInfoHash());
            if (!peer.isClientInterested() && !t.isPieceComplete(index)) {
                SimpleMessage m = new SimpleMessage(
                        SimpleMessage.TYPE_INTERESTED, peer);
                writer.postMessage(m);
                peer.setClientInterested(true);
            }
        }
    }

    private void processInterested(Peer peer) {
        peer.setPeerInterested(true);
    }

    private void processNotInterested(Peer peer) {
        peer.setPeerInterested(false);
    }

    private void processPiece(PieceMessage message)
            throws NoSuchAlgorithmException, IOException {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        int index = message.getIndex();
        int begin = message.getBegin();
        if (validator.validatePieceMessage(message)) {
            if (t.savePiece(begin, index, message.getPiece())) {
                BitSet n = t.getCompletePieces();
                for (Peer p : t.getConnectedPeers()) {
                    SimpleMessage msg = new HaveMessage(index, p);
                    writer.postMessage(msg);
                    if (p.getOtherPieces(n).isEmpty()) {
                        SimpleMessage smessage = new SimpleMessage(
                                SimpleMessage.TYPE_NOT_INTERESTED, p);
                        writer.postMessage(smessage);
                        p.setClientInterested(false);
                    }
                }
            }
            if (t.isTorrentComplete()) {
                t.endTracker();
            }
        }
        if (!peer.isPeerChoking() && peer.isClientInterested()) {
            decideNextPiece(t, peer);
        }
    }

    private void processRequest(BlockMessage message) throws
            IOException {
        Peer peer = message.getDestination();
        if (validator.validateRequestMessage(message) &&
                !peer.isClientChoking() && peer.isPeerInterested()) {
            Torrent t = torrents.get(peer.getInfoHash());
            ByteBuffer piece = t.loadPiece(message.getBegin(),
                    message.getIndex(),
                    message.getLength());
            PieceMessage pm = new PieceMessage(message.getBegin(),
                    message.getIndex(), piece, message.getLength(), peer);
            writer.postMessage(pm);
        }
    }

    private void processUnchoke(Peer peer) throws IOException {
        peer.setPeerChoking(false);
        if (peer.isClientInterested()) {
            decideNextPiece(torrents.get(peer.getInfoHash()), peer);
        }

    }

    private void decideNextPiece(Torrent t, Peer peer) throws IOException {
        BitSet[] req = requests.get(t.getInfoHash());
        BlockMessage bm = t.decideNextPiece(peer, req);
        if (bm == null) {
            SimpleMessage smessage = new SimpleMessage(
                    SimpleMessage.TYPE_NOT_INTERESTED, peer);
            writer.postMessage(smessage);
            int cs = t.getChunkSize();
            req[bm.getIndex()].set(TorrentUtil.computeBeginIndex(bm.getBegin(),
                    cs),
                    TorrentUtil.computeEndIndex(bm.getBegin(), bm.getLength(),
                    cs));
            peer.setClientInterested(false);
        } else {
            writer.postMessage(bm);
        }
    }
}
