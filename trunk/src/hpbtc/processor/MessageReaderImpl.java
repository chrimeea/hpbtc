package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.protocol.torrent.Tracker;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
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
    private Map<byte[], Tracker> trackers;
    private byte[] protocol;

    public MessageReaderImpl(final Map<byte[], Torrent> torrents,
            final byte[] peerId, final Map<byte[], BitSet[]> requests,
            final MessageWriter writer, final Map<byte[], Tracker> trackers,
            final byte[] protocol) {
        this.protocol = protocol;
        this.trackers = trackers;
        this.writer = writer;
        this.peerId = peerId;
        this.torrents = torrents;
        this.requests = requests;
        validator = new MessageValidator(torrents, protocol);
    }

    public void disconnect(final Peer peer) throws IOException {
        writer.disconnect(peer);
    }

    private void checkPeerId(final Peer peer) throws IOException {
        if (peer.download()) {
            peer.setId(peer.getData().array());
            logger.fine("Received id for " + peer);
        }
    }

    public void readMessage(final Peer peer) throws IOException,
            NoSuchAlgorithmException {
        if (peer.isHandshakeReceived()) {
            if (peer.getId() == null) {
                checkPeerId(peer);
            } else {
                if (!peer.isExpectBody()) {
                    peer.setNextDataExpectation(4);
                    if (peer.download()) {
                        int len = peer.getData().getInt();
                        if (len < 0) {
                            throw new EOFException();
                        } else {
                            peer.setExpectBody(true);
                            peer.setNextDataExpectation(len);
                        }
                    } else {
                        return;
                    }
                }
                if (peer.download()) {
                    ByteBuffer data = peer.getData();
                    peer.setExpectBody(false);
                    byte disc = data.get();
                    switch (disc) {
                        case SimpleMessage.TYPE_BITFIELD:
                            BitfieldMessage mBit = new BitfieldMessage(data,
                                    peer);
                            logger.fine("Received " + mBit);
                            processBitfield(mBit);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            BlockMessage mCan = new BlockMessage(data, disc,
                                    peer);
                            logger.fine("Received " + mCan);
                            processCancel(mCan);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            SimpleMessage mChoke = new SimpleMessage(disc, peer);
                            logger.fine("Received " + mChoke);
                            processChoke(mChoke);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            HaveMessage mHave = new HaveMessage(data, peer);
                            logger.fine("Received " + mHave);
                            processHave(mHave);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            SimpleMessage mInt = new SimpleMessage(disc, peer);
                            logger.fine("Received " + mInt);
                            processInterested(mInt);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            SimpleMessage mNot = new SimpleMessage(disc, peer);
                            logger.fine("Received " + mNot);
                            processNotInterested(mNot);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            PieceMessage mPiece = new PieceMessage(data, peer);
                            logger.fine("Received " + mPiece);
                            processPiece(mPiece);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            BlockMessage mReq = new BlockMessage(data,
                                    SimpleMessage.TYPE_REQUEST, peer);
                            logger.fine("Received " + mReq);
                            processRequest(mReq);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            SimpleMessage mUn = new SimpleMessage(disc, peer);
                            logger.fine("Received " + mUn);
                            processUnchoke(mUn);
                    }
                    peer.setMessagesReceived();
                }
            }
        } else {
            peer.setNextDataExpectation(48);
            if (peer.download()) {
                HandshakeMessage mHand = new HandshakeMessage(peer.getData(),
                        peer);
                logger.fine("Received " + mHand);
                processHandshake(mHand);
                peer.setNextDataExpectation(20);
                checkPeerId(peer);
            }
        }
    }

    private void processHandshake(final HandshakeMessage message) throws
            IOException {
        Peer peer = message.getDestination();
        if (validator.validateHandshakeMessage(message)) {
            peer.setHandshakeReceived();
            byte[] infoHash = message.getInfoHash();
            Torrent t = torrents.get(infoHash);
            if (!peer.isHandshakeSent()) {
                peer.setInfoHash(infoHash);
                HandshakeMessage reply = new HandshakeMessage(infoHash, peerId,
                        protocol, peer);
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
            logger.warning("Invalid message: " + message);
            writer.disconnect(peer);
        }
    }

    private void processBitfield(final BitfieldMessage message) throws
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
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processCancel(final BlockMessage message) {
        if (validator.validateCancelMessage(message)) {
            Peer peer = message.getDestination();
            writer.cancelPieceMessage(message.getBegin(),
                    message.getIndex(), message.getLength(), peer);
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processChoke(final SimpleMessage message) {
        message.getDestination().setPeerChoking(true);
    }

    private void processHave(final HaveMessage message) throws IOException {
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
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processInterested(final SimpleMessage message) {
        message.getDestination().setPeerInterested(true);
    }

    private void processNotInterested(final SimpleMessage message) {
        message.getDestination().setPeerInterested(false);
    }

    private void processPiece(final PieceMessage message)
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
                trackers.get(t.getInfoHash()).endTracker(t.getUploaded(),
                        t.getDownloaded());
            }
        } else {
            logger.warning("Invalid message: " + message);
        }
        if (!peer.isPeerChoking() && peer.isClientInterested()) {
            decideNextPiece(t, peer);
        }
    }

    private void processRequest(final BlockMessage message) throws
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
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processUnchoke(final SimpleMessage message) throws IOException {
        Peer peer = message.getDestination();
        peer.setPeerChoking(false);
        if (peer.isClientInterested()) {
            decideNextPiece(torrents.get(peer.getInfoHash()), peer);
        }

    }

    private void decideNextPiece(final Torrent t, final Peer peer)
            throws IOException {
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
