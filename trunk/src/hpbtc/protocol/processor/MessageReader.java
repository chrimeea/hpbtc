package hpbtc.protocol.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Register;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;
import hpbtc.util.TorrentUtil;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageReader {

    private static Logger logger = Logger.getLogger(MessageReader.class.
            getName());
    private MessageValidator validator;
    protected Register register;
    private MessageWriter writer;
    private List<Torrent> torrents;
    private byte[] peerId;
    private byte[] protocol;
    protected Selector selector;

    public MessageReader(final Register register, final byte[] protocol,
            final MessageWriter writer, final List<Torrent> torrents,
            final byte[] peerId) {
        this.peerId = peerId;
        this.writer = writer;
        this.register = register;
        this.torrents = torrents;
        this.protocol = protocol;
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
        writer.keepAliveRead(peer);
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
                        } else if (len > 0) {
                            peer.setExpectBody(true);
                            peer.setNextDataExpectation(len);
                        } else {
                            return;
                        }
                    } else {
                        return;
                    }
                }
                if (peer.download()) {
                    final ByteBuffer data = peer.getData();
                    peer.setExpectBody(false);
                    final byte disc = data.get();
                    switch (disc) {
                        case SimpleMessage.TYPE_BITFIELD:
                            final BitfieldMessage mBit = new BitfieldMessage(
                                    data, peer);
                            logger.fine("Received " + mBit);
                            processBitfield(mBit);
                            break;
                        case SimpleMessage.TYPE_CANCEL:
                            final BlockMessage mCan = new BlockMessage(data,
                                    disc, peer);
                            logger.fine("Received " + mCan);
                            processCancel(mCan);
                            break;
                        case SimpleMessage.TYPE_CHOKE:
                            final SimpleMessage mChoke = new SimpleMessage(disc,
                                    peer);
                            logger.fine("Received " + mChoke);
                            processChoke(mChoke);
                            break;
                        case SimpleMessage.TYPE_HAVE:
                            final HaveMessage mHave =
                                    new HaveMessage(data, peer);
                            logger.fine("Received " + mHave);
                            processHave(mHave);
                            break;
                        case SimpleMessage.TYPE_INTERESTED:
                            final SimpleMessage mInt = new SimpleMessage(disc,
                                    peer);
                            logger.fine("Received " + mInt);
                            processInterested(mInt);
                            break;
                        case SimpleMessage.TYPE_NOT_INTERESTED:
                            final SimpleMessage mNot = new SimpleMessage(disc,
                                    peer);
                            logger.fine("Received " + mNot);
                            processNotInterested(mNot);
                            break;
                        case SimpleMessage.TYPE_PIECE:
                            final PieceMessage mPiece = new PieceMessage(data,
                                    peer);
                            logger.fine("Received " + mPiece);
                            processPiece(mPiece);
                            break;
                        case SimpleMessage.TYPE_REQUEST:
                            final BlockMessage mReq = new BlockMessage(data,
                                    disc, peer);
                            logger.fine("Received " + mReq);
                            processRequest(mReq);
                            break;
                        case SimpleMessage.TYPE_UNCHOKE:
                            final SimpleMessage mUn = new SimpleMessage(disc,
                                    peer);
                            logger.fine("Received " + mUn);
                            processUnchoke(mUn);
                    }
                    peer.setMessagesReceived();
                }
            }
        } else {
            peer.setNextDataExpectation(48);
            if (peer.download()) {
                final HandshakeMessage mHand = new HandshakeMessage(
                        peer.getData(), peer);
                logger.fine("Received " + mHand);
                processHandshake(mHand);
                peer.setNextDataExpectation(20);
                checkPeerId(peer);
            }
        }
    }

    private void processHandshake(final HandshakeMessage message) throws
            IOException {
        final Peer peer = message.getDestination();
        if (validator.validateHandshakeMessage(message)) {
            peer.setHandshakeReceived();
            final byte[] infoHash = message.getInfoHash();
            Torrent t = peer.getTorrent();
            if (t == null) {
                for (Torrent tor : torrents) {
                    if (tor.getInfoHash() == infoHash) {
                        peer.setTorrent(tor);
                        t = tor;
                        break;
                    }
                }
            }
            boolean isIncoming = false;
            if (!peer.isHandshakeSent()) {
                writer.postMessage(new HandshakeMessage(peerId, protocol, peer,
                        t.getInfoHash()));
                peer.setHandshakeSent();
                isIncoming = true;
            }
            t.addPeer(peer, isIncoming);
            final BitSet bs = t.getCompletePieces();
            if (bs.cardinality() > 0) {
                writer.postMessage(new BitfieldMessage(bs, t.getNrPieces(),
                        peer));
            }
        } else {
            logger.warning("Invalid message: " + message);
            disconnect(peer);
        }
    }

    private void processBitfield(final BitfieldMessage message) throws
            IOException {
        if (validator.validateBitfieldMessage(message)) {
            final Peer peer = message.getDestination();
            BitSet b = message.getBitfield();
            peer.setPieces(b);
            final Torrent t = peer.getTorrent();
            t.updateAvailability(peer.getPieces());
            if (t.countRemainingPieces() == 0 &&
                    b.cardinality() == t.getNrPieces()) {
                writer.disconnect(peer);
            } else if (!t.getOtherPieces(peer).isEmpty()) {
                peer.setClientInterested(true);
                writer.postMessage(new SimpleMessage(
                        SimpleMessage.TYPE_INTERESTED, peer));
            }
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processCancel(final BlockMessage message) {
        if (validator.validateCancelMessage(message)) {
            message.getDestination().cancelPieceMessage(message.getBegin(),
                    message.getIndex(), message.getLength());
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processChoke(final SimpleMessage message) {
        final Peer peer = message.getDestination();
        peer.setPeerChoking(true);
        peer.cancelPieceMessage();
    }

    private void processHave(final HaveMessage message) throws IOException {
        if (validator.validateHaveMessage(message)) {
            final Peer peer = message.getDestination();
            final int index = message.getIndex();
            peer.setPiece(index);
            final Torrent t = peer.getTorrent();
            t.updateAvailability(index);
            if (t.countRemainingPieces() == 0 &&
                    peer.getPieces().cardinality() == t.getNrPieces()) {
                writer.disconnect(peer);
            } else if (!peer.isClientInterested() && !t.isPieceComplete(index)) {
                peer.setClientInterested(true);
                writer.postMessage(new SimpleMessage(
                        SimpleMessage.TYPE_INTERESTED, peer));
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
        final Peer peer = message.getDestination();
        final Torrent t = peer.getTorrent();
        final int index = message.getIndex();
        final int begin = message.getBegin();
        if (validator.validatePieceMessage(message) &&
                peer.removeRequest(index, begin)) {
            if (t.savePiece(begin, index, message.getPiece())) {
                final Set<Peer> peers = t.getConnectedPeers();
                synchronized (peers) {
                    for (Peer p : peers) {
                        if (!p.getPieces().get(index)) {
                            writer.postMessage(new HaveMessage(index, p));
                        } else {
                            if (t.getOtherPieces(p).isEmpty()) {
                                p.setClientInterested(false);
                                writer.postMessage(
                                        new SimpleMessage(
                                        SimpleMessage.TYPE_NOT_INTERESTED, p));
                            }
                            if (p.removeRequest(index, begin)) {
                                writer.postMessage(createBlockMessage(begin,
                                        index, p, SimpleMessage.TYPE_CANCEL));
                            }
                        }
                    }
                }
                if (t.countRemainingPieces() == 0) {
                    t.endTracker();
                    logger.info("Torrent complete !");
                    return;
                }
            }
            if (!peer.isPeerChoking() && peer.isClientInterested()) {
                decideNextPieces(peer);
            }
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processRequest(final BlockMessage message) throws
            IOException {
        final Peer peer = message.getDestination();
        if (validator.validateRequestMessage(message) &&
                !peer.isClientChoking() && peer.isPeerInterested()) {
            final PieceMessage pm = new PieceMessage(message.getBegin(),
                    message.getIndex(), message.getLength(), peer);
            writer.postMessage(pm);
        } else {
            logger.warning("Invalid message: " + message);
        }
    }

    private void processUnchoke(final SimpleMessage message) throws IOException {
        final Peer peer = message.getDestination();
        peer.setPeerChoking(false);
        if (peer.isClientInterested()) {
            decideNextPieces(peer);
        }
    }

    private void decideNextPieces(final Peer peer)
            throws IOException {
        for (int i = peer.countTotalRequests(); i < 5; i++) {
            final BlockMessage bm = decideNextPiece(peer);
            if (bm != null) {
                peer.addRequest(bm.getIndex(), bm.getBegin());
                writer.postMessage(bm);
            } else {
                break;
            }
        }
    }

    private BlockMessage decideNextPiece(final Peer peer) {
        final Torrent torrent = peer.getTorrent();
        final BitSet peerPieces = (BitSet) peer.getPieces().clone();
        peerPieces.andNot(torrent.getCompletePieces());
        int max = 0;
        int index = -1;
        int beginIndex = 0;
        int min = -1;
        for (int i = peerPieces.nextSetBit(0); i >= 0;
                i = peerPieces.nextSetBit(i + 1)) {
            final BitSet sar = (BitSet) torrent.getChunksSaved(i).clone();
            sar.or(torrent.getChunksRequested(i));
            final int card = sar.cardinality();
            final int ch = sar.nextClearBit(0);
            final int a = torrent.getAvailability(i);
            if (ch < torrent.computeChunksInPiece(i)) {
                if (card > max) {
                    max = card;
                    index = i;
                    beginIndex = ch;
                } else if (card == max && (a < min || min == -1)) {
                    min = a;
                    index = i;
                    beginIndex = ch;
                }
            }
        }
        if (index == -1) {
            if (torrent.countRemainingPieces() < 3) {
                BitSet r = null;
                do {
                    index = peerPieces.nextSetBit(index + 1);
                    if (index != -1) {
                        r = (BitSet) peer.getRequests(index);
                        final BitSet x = torrent.getChunksSaved(index);
                        if (r == null) {
                            r = x;
                        } else {
                            r = (BitSet) r.clone();
                            r.or(x);
                        }
                    } else {
                        break;
                    }
                } while (r.cardinality() == torrent.computeChunksInPiece(index));
                if (index != -1) {
                    beginIndex = r.nextClearBit(0);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        }
        final int begin = TorrentUtil.computeBeginPosition(beginIndex,
                torrent.getChunkSize());
        return createBlockMessage(begin, index, peer, SimpleMessage.TYPE_REQUEST);
    }

    private BlockMessage createBlockMessage(final int begin, final int index,
            final Peer peer, final byte disc) {
        Torrent torrent = peer.getTorrent();
        return new BlockMessage(begin, index,
                TorrentUtil.computeChunkSize(index, begin,
                torrent.getChunkSize(),
                torrent.getFileLength(), torrent.getPieceLength()), disc, peer);
    }

    public void connect(final Peer peer) throws IOException {
        register.registerNow((SelectableChannel) peer.getChannel(), selector,
                SelectionKey.OP_READ, peer);
        writer.keepAliveRead(peer);
    }

    public void setReadSelector(final Selector selector) {
        this.selector = selector;
        writer.setReadSelector(selector);
    }
}
