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
    
    public MessageProcessor(Network network, byte[] protocol,
            Map<byte[], Torrent> torrents, byte[] peerId) {
        this.protocol = protocol;
        this.network = network;
        this.peerId = peerId;
        this.torrents = torrents;
    }
    
    public void processHandshake(HandshakeMessage message, Peer peer) throws IOException {
        peer.setId(message.getPeerId());
        byte[] infoHash = message.getInfoHash();
        peer.setInfoHash(infoHash);
        peer.setHandshakeReceived();
        HandshakeMessage reply = new HandshakeMessage(infoHash, peerId, protocol);
        network.postMessage(peer, reply);
        torrents.get(infoHash).addPeer(peer);
    }

    public void processBitfield(BitfieldMessage message, Peer peer) {
        peer.setPieces(message.getBitfield());
    }

    public void processCancel(BlockMessage message, Peer peer) {
        network.cancelPieceMessage(message.getBegin(),
                message.getIndex(), message.getLength(), peer);
    }

    public void processChoke(Peer peer) {
        peer.setPeerChoking(true);
    }

    public void processHave(HaveMessage message, Peer peer) {
        peer.setPiece(message.getIndex());
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
        t.savePiece(message.getBegin(), message.getIndex(), message.getPiece());
    }

    public void processRequest(BlockMessage message, Peer peer) throws IOException {
        Torrent t = torrents.get(peer.getInfoHash());
        ByteBuffer piece = t.loadPiece(message.getBegin(), message.getIndex(),
                message.getLength());
        PieceMessage pm = new PieceMessage(message.getBegin(),
                message.getIndex(), piece, message.getLength());
        network.postMessage(peer, pm);
    }

    public void processUnchoke(Peer peer) {
        peer.setPeerChoking(false);
    }
}
