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
import java.util.List;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageValidator {

    private List<Torrent> torrents;
    private byte[] protocol;

    public MessageValidator(final List<Torrent> torrents, final byte[] protocol) {
        this.torrents = torrents;
        this.protocol = protocol;
    }

    public boolean validateHandshakeMessage(final HandshakeMessage message)
            throws UnsupportedEncodingException {
        if (message.getDestination().isHandshakeReceived() || !Arrays.equals(
                message.getProtocol(), protocol)) {
            return false;
        }
        for (Torrent t: torrents) {
            byte[] ih = t.getInfoHash();
            if (Arrays.equals(ih, message.getInfoHash())) {
            message.setInfoHash(ih);
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
            long nrPieces = peer.getTorrent().getNrPieces();
            if (message.getBitfield().length() > nrPieces ||
                    message.getMessageLength() != 1 + Math.ceil(nrPieces / 8.0)) {
                return false;
            }
        }
        return true;
    }

    public boolean validateCancelMessage(final BlockMessage message) {
        Torrent t = message.getDestination().getTorrent();
        return message.getIndex() < t.getNrPieces() && message.getBegin() < t.
                getPieceLength();
    }

    public boolean validateHaveMessage(final HaveMessage message) {
        Torrent t = message.getDestination().getTorrent();
        return message.getIndex() < t.getNrPieces();
    }

    public boolean validatePieceMessage(final PieceMessage message) {
        Torrent t = message.getDestination().getTorrent();
        return message.getIndex() < t.getNrPieces() && message.getBegin() < t.
                getPieceLength();
    }

    public boolean validateRequestMessage(final BlockMessage message) {
        Torrent t = message.getDestination().getTorrent();
        return message.getIndex() < t.getNrPieces() && message.getBegin() <
                t.getPieceLength() && message.getLength() <= t.getChunkSize()
                && t.isPieceComplete(message.getIndex());
    }
}
