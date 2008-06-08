package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.network.Network;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

/**
 *
 * @author Chris
 */
public class MessageProcessor {

    private byte[] protocol;
    private Network network;
    private byte[] peerId;
    private PieceRepository pieceRep;
    
    public MessageProcessor(Network network, byte[] protocol,
            PieceRepository pieceRep) {
        this.protocol = protocol;
        this.network = network;
        this.peerId = generateId();
        this.pieceRep = pieceRep;
    }
    
    public void processHandshake(HandshakeMessage message, Peer peer) throws IOException {
        peer.setId(message.getPeerId());
        peer.setInfoHash(message.getInfoHash());
        peer.setHandshakeReceived();
        HandshakeMessage reply = new HandshakeMessage(message.getInfoHash(),
                peerId, protocol);
        network.postMessage(peer, reply);
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

    public void processPiece(PieceMessage message, Peer peer) {
        pieceRep.savePiece(peer.getInfoHash(), message.getBegin(),
                message.getIndex(), message.getPiece());
    }

    public void processRequest(BlockMessage message, Peer peer) throws IOException {
        ByteBuffer piece = pieceRep.getPiece(peer.getInfoHash(),
                message.getBegin(), message.getIndex(),
                message.getLength());
        PieceMessage pm = new PieceMessage(message.getBegin(),
                message.getIndex(), piece, message.getLength());
        network.postMessage(peer, pm);
    }

    public void processUnchoke(Peer peer) {
        peer.setPeerChoking(false);
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
}