package hpbtc.protocol.processor;

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
import hpbtc.util.TorrentUtil;

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
            final byte[] ih = t.getInfoHash();
            if (Arrays.equals(ih, message.getInfoHash())) {
            message.setInfoHash(ih);
            return true;
            }
        }
        return false;        
    }

    public boolean validateBitfieldMessage(final BitfieldMessage message) {
        final Peer peer = message.getDestination();
        if (peer.isMessagesReceived()) {
            return false;
        } else {
            final long nrPieces = peer.getTorrent().getNrPieces();
            if (message.getBitfield().length() > nrPieces ||
                    message.getMessageLength() != 1 + Math.ceil(nrPieces / 8.0)) {
                return false;
            }
        }
        return true;
    }

    public boolean validateCancelMessage(final BlockMessage message) {
        final Torrent t = message.getDestination().getTorrent();
        return t != null && message.getIndex() < t.getNrPieces() &&
                message.getBegin() < t.getPieceLength();
    }

    public boolean validateHaveMessage(final HaveMessage message) {
        final Torrent t = message.getDestination().getTorrent();
        return t != null && message.getIndex() < t.getNrPieces();
    }

    public boolean validatePieceMessage(final PieceMessage message) {
        final Torrent t = message.getDestination().getTorrent();
        final int i = message.getIndex();
        final int b = message.getBegin();
        return t != null && i >= 0 && i < t.getNrPieces() && b >= 0 &&
                b < t.getPieceLength() &&
                message.getLength() == TorrentUtil.computeChunkSize(i, b,
                t.getChunkSize(), t.getFileLength(), t.getPieceLength());
    }

    public boolean validateRequestMessage(final BlockMessage message) {
        final Torrent t = message.getDestination().getTorrent();
        final int i = message.getIndex();
        final int b = message.getBegin();
        final int l = message.getLength();
        return t != null && i >= 0 && i < t.getNrPieces() && b >= 0 &&
                b < t.getPieceLength() && l <= t.getChunkSize() && l > 0 &&
                t.isPieceComplete(i);
    }
}
