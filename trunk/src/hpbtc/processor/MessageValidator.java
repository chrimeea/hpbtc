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
 * @author Cristian Mocanu
 */
public class MessageValidator {

    private Map<byte[], Torrent> torrents;
    private byte[] protocol;

    public MessageValidator(final Map<byte[], Torrent> torrents,
            final byte[] protocol) {
        this.torrents = torrents;
        this.protocol = protocol;
    }

    public boolean validateHandshakeMessage(final HandshakeMessage message)
            throws UnsupportedEncodingException {
        if (message.getDestination().isHandshakeReceived() || !Arrays.equals(
                message.getProtocol(), protocol)) {
            return false;
        }
        byte[] ih = message.getInfoHash();
        for (byte[] b: torrents.keySet()) {
            if (Arrays.equals(b, ih)) {
                message.setInfoHash(b);
                return true;
            }
        }
        return false;
    }

    public boolean validateBitfieldMessage(final BitfieldMessage message) {
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

    public boolean validateCancelMessage(final BlockMessage message) {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces() && message.getBegin() < t.
                getPieceLength();
    }

    public boolean validateHaveMessage(final HaveMessage message) {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces();
    }

    public boolean validatePieceMessage(final PieceMessage message) {
        Peer peer = message.getDestination();
        Torrent t = torrents.get(peer.getInfoHash());
        return message.getIndex() < t.getNrPieces() && message.getBegin() < t.
                getPieceLength();
    }

    public boolean validateRequestMessage(final BlockMessage message) {
        Peer peer = message.getDestination();
        byte[] infoHash = peer.getInfoHash();
        Torrent t = torrents.get(infoHash);
        long pieceLength = t.getPieceLength();
        return message.getIndex() < t.getNrPieces() && message.getBegin() <
                pieceLength && message.getBegin() + message.getLength() >
                pieceLength && t.isPieceComplete(message.getIndex());
    }
}
