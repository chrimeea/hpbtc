package hpbtc.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Map;

/**
 *
 * @author Chris
 */
public class MessageValidator {

    private Map<byte[], Torrent> torrents;

    public MessageValidator(Map<byte[], Torrent> torrents) {
        this.torrents = torrents;
    }

    public boolean validateHandshakeMessage(HandshakeMessage message) throws 
            UnsupportedEncodingException {
        return Arrays.equals(message.getProtocol(), Protocol.
                getSupportedProtocol()) && torrents.containsKey(message.
                getInfoHash());
    }

    public boolean validateBitfieldMessage(BitfieldMessage message) {
        Peer peer = message.getDestination();
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

    public boolean validateCancelMessage(BlockMessage message) {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces() && message.getBegin() < t.
                getPieceLength();
    }

    public boolean validateHaveMessage(HaveMessage message) {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces();
    }

    public boolean validatePieceMessage(PieceMessage message) {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces() && message.getBegin() < t.
                getPieceLength();
    }

    public boolean validateRequestMessage(BlockMessage message) {
        Peer peer = message.getDestination();
        byte[] infoHash = peer.getInfoHash();
        Torrent t = torrents.get(infoHash);
        long pieceLength = t.getPieceLength();
        return message.getIndex() < t.getNrPieces() && message.getBegin() <
                pieceLength && message.getBegin() + message.getLength() >
                pieceLength && t.isPieceComplete(message.getIndex());
    }
}
