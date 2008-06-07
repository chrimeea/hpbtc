package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author Chris
 */
public class MessageValidator {

    private Map<byte[], Torrent> torrents;
    private byte[] protocol;
    private PieceRepository pieceRep;
    
    public MessageValidator(Map<byte[], Torrent> torrents,
            byte[] protocol, PieceRepository pieceRep) {
        this.torrents = torrents;
        this.protocol = protocol;
        this.pieceRep = pieceRep;
    }
    
    public boolean validateHandshakeMessage(HandshakeMessage message, Peer peer) {
        return Arrays.equals(message.getProtocol(), protocol)
                && torrents.containsKey(message.getInfoHash());
    }
    
    public boolean validateBitfieldMessage(BitfieldMessage message, Peer peer) {
        if (peer.isMessagesReceived()) {
            return false;
        } else {
            long nrPieces = torrents.get(peer.getInfoHash()).getNrPieces();
            if (message.getBitfield().length() > nrPieces || 
                    message.getMessageLength() != 1 + Math.ceil(nrPieces / 8.0)) {
                return false;
            }
        }
        return true;
    }
    
    public boolean validateCancelMessage(BlockMessage message, Peer peer) {
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces()
                && message.getBegin() < t.getPieceLength();
    }
    
    public boolean validateHaveMessage(HaveMessage message, Peer peer) {
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces();
    }
    
    public boolean validatePieceMessage(PieceMessage message, Peer peer) {
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces()
                && message.getBegin() < t.getPieceLength();
    }
    
    public boolean validateRequestMessage(BlockMessage message, Peer peer) {
        byte[] infoHash = peer.getInfoHash();
        Torrent t = torrents.get(infoHash); 
        long pieceLength = t.getPieceLength();
        return message.getIndex() < t.getNrPieces()
                && message.getBegin() < pieceLength
                && message.getBegin() + message.getLength() > pieceLength
                && pieceRep.isPiece(infoHash, message.getIndex());
    }
}
