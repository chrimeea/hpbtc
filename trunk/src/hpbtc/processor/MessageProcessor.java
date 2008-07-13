package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
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

    public void processHandshake(HandshakeMessage message, Peer peer) throws IOException {
        if (validator.validateHandshakeMessage(message, peer)) {
            peer.setId(message.getPeerId());
            byte[] infoHash = message.getInfoHash();
            peer.setInfoHash(infoHash);
            peer.setHandshakeReceived();
            HandshakeMessage reply = new HandshakeMessage(infoHash, peerId, protocol);
            network.postMessage(peer, reply);
            torrents.get(infoHash).addPeer(peer);
        } else {
            processDisconnect(peer);
            network.closeConnection(peer);
        }
    }

    public void processBitfield(BitfieldMessage message, Peer peer) {
        if (validator.validateBitfieldMessage(message, peer)) {
            peer.setPieces(message.getBitfield());
        }
    }

    public void processCancel(BlockMessage message, Peer peer) {
        if (validator.validateCancelMessage(message, peer)) {
            network.cancelPieceMessage(message.getBegin(),
                    message.getIndex(), message.getLength(), peer);
        }
    }

    public void processChoke(Peer peer) {
        peer.setPeerChoking(true);
    }

    public void processHave(HaveMessage message, Peer peer) {
        if (validator.validateHaveMessage(message, peer)) {
            peer.setPiece(message.getIndex());
        }
    }

    public void processInterested(Peer peer) {
        peer.setPeerInterested(true);
    }

    public void processNotInterested(Peer peer) {
        peer.setPeerInterested(false);
    }

    public void processPiece(PieceMessage message, Peer peer)
            throws NoSuchAlgorithmException, IOException {
        Torrent t = torrents.get(peer.getInfoHash());
        int index = message.getIndex();
        int begin = message.getBegin();
        if (validator.validatePieceMessage(message, peer)) {
            t.savePiece(begin, index, message.getPiece());
        }
    }

    public void processRequest(BlockMessage message, Peer peer) throws IOException {
        if (validator.validateRequestMessage(message, peer) &&
                !peer.isClientChoking()) {
            Torrent t = torrents.get(peer.getInfoHash());
            ByteBuffer piece = t.loadPiece(message.getBegin(), message.getIndex(),
                    message.getLength());
            PieceMessage pm = new PieceMessage(message.getBegin(),
                    message.getIndex(), piece, message.getLength());
            network.postMessage(peer, pm);
        }
    }

    public void processUnchoke(Peer peer) throws IOException {
        peer.setPeerChoking(false);
        if (peer.isClientInterested()) {
            Torrent t = torrents.get(peer.getInfoHash());
            t.decideNextPiece(peer);
        }
    }

    public void processDisconnect(Peer peer) {
        torrents.get(peer.getInfoHash()).removePeer(peer);
    }
}
