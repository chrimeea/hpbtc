package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Map;

/**
 *
 * @author Chris
 */
public class MessageProcessor {

    private byte[] protocol;
    private Network network;
    private byte[] peerId;
    private Map<byte[], Torrent> torrents;
    private MessageValidator validator;

    public MessageProcessor(Network network, byte[] protocol,
            Map<byte[], Torrent> torrents, byte[] peerId) {
        this.protocol = protocol;
        this.network = network;
        this.peerId = peerId;
        this.torrents = torrents;
        validator = new MessageValidator(torrents, protocol);
    }

    public void processHandshake(HandshakeMessage message) throws
            IOException {
        Peer peer = message.getDestination();
        if (validator.validateHandshakeMessage(message)) {
            peer.setId(message.getPeerId());
            byte[] infoHash = message.getInfoHash();
            peer.setInfoHash(infoHash);
            peer.setHandshakeReceived();
            HandshakeMessage reply = new HandshakeMessage(infoHash, peerId,
                    protocol);
            network.postMessage(reply);
            Torrent t = torrents.get(peer.getInfoHash());
            BitSet bs = t.getCompletePieces();
            if (bs.cardinality() > 0) {
                BitfieldMessage bmessage = new BitfieldMessage(bs,
                        t.getNrPieces(), peer);
                network.postMessage(bmessage);
            }
            t.addPeer(peer);
        } else {
            processDisconnect(peer);
            network.closeConnection(peer);
        }
    }

    public void processBitfield(BitfieldMessage message) throws
            IOException {
        if (validator.validateBitfieldMessage(message)) {
            Peer peer = message.getDestination();
            peer.setPieces(message.getBitfield());
            Torrent t = torrents.get(peer.getInfoHash());
            if (!peer.getOtherPieces(t.getCompletePieces()).isEmpty()) {
                SimpleMessage smessage = new SimpleMessage(
                        SimpleMessage.TYPE_INTERESTED, peer);
                network.postMessage(smessage);
                peer.setClientInterested(true);
            }
        }
    }

    public void processCancel(BlockMessage message) {
        if (validator.validateCancelMessage(message)) {
            Peer peer = message.getDestination();
            network.cancelPieceMessage(message.getBegin(),
                    message.getIndex(), message.getLength(), peer);
        }
    }

    public void processChoke(Peer peer) {
        peer.setPeerChoking(true);
    }

    public void processHave(HaveMessage message) throws IOException {
        if (validator.validateHaveMessage(message)) {
            Peer peer = message.getDestination();
            int index = message.getIndex();
            peer.setPiece(index);
            Torrent t = torrents.get(peer.getInfoHash());
            if (!peer.isClientInterested() && !t.isPieceComplete(index)) {
                SimpleMessage m = new SimpleMessage(
                        SimpleMessage.TYPE_INTERESTED, peer);
                network.postMessage(m);
                peer.setClientInterested(true);
            }
        }
    }

    public void processInterested(Peer peer) {
        peer.setPeerInterested(true);
    }

    public void processNotInterested(Peer peer) {
        peer.setPeerInterested(false);
    }

    public void processPiece(PieceMessage message)
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
                    network.postMessage(msg);
                    if (p.getOtherPieces(n).isEmpty()) {
                        SimpleMessage smessage = new SimpleMessage(
                                SimpleMessage.TYPE_NOT_INTERESTED, p);
                        network.postMessage(smessage);
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

    public void processRequest(BlockMessage message) throws
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
            network.postMessage(pm);
        }
    }

    public void processUnchoke(Peer peer) throws IOException {
        peer.setPeerChoking(false);
        if (peer.isClientInterested()) {
            decideNextPiece(torrents.get(peer.getInfoHash()), peer);
        }

    }

    private void decideNextPiece(Torrent t, Peer peer) throws IOException {
        BlockMessage bm = t.decideNextPiece(peer);
        if (bm == null) {
            SimpleMessage smessage = new SimpleMessage(
                    SimpleMessage.TYPE_NOT_INTERESTED, peer);
            network.postMessage(smessage);
            peer.setClientInterested(false);
        } else {
            network.postMessage(bm);
        }
    }
    
    public void processDisconnect(Peer peer) {
        torrents.get(peer.getInfoHash()).removePeer(peer);
    }
}
