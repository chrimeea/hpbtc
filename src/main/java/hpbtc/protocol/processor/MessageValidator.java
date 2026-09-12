package hpbtc.protocol.processor;

import hpbtc.protocol.message.BitfieldMessage;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.PieceMessage;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import hpbtc.util.TorrentUtil;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageValidator {

    private static Logger logger = Logger.getLogger(MessageValidator.class.getName());
    private List<Torrent> torrents;
    private byte[] protocol;
    private byte[] peerId;

    public MessageValidator(final List<Torrent> torrents, final byte[] protocol,
            final byte[] peerId) {
        this.torrents = torrents;
        this.protocol = protocol;
        this.peerId = peerId;
    }

    public boolean validateHandshakeMessage(final HandshakeMessage message)
            throws UnsupportedEncodingException {
        final Peer peer = message.getDestination();
        if (peer.isHandshakeReceived() || !Arrays.equals(
                message.getProtocol(), protocol)) {
            return false;
        }
        try {
            if (Arrays.equals(peer.getId(), peerId) &&
                    InetAddress.getLocalHost().equals((
                    (InetSocketAddress) peer.getAddress()).getAddress())) {
                return false;
            }
        } catch (UnknownHostException e) {
            logger.log(Level.WARNING, e.getLocalizedMessage(), e);
        }
        for (Torrent t : torrents) {
            final byte[] ih = t.getInfoHash();
            if (Arrays.equals(ih, message.getInfoHash())) {
                message.setInfoHash(ih);
                return true;
            }
        }
        return false;
    }

    public boolean validateBitfieldMessage(final BitfieldMessage message)
            throws InvalidPeerException {
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

    public boolean validateCancelMessage(final BlockMessage message)
            throws InvalidPeerException {
        final Torrent t = message.getDestination().getTorrent();
        return t != null && message.getIndex() < t.getNrPieces() &&
                message.getBegin() < t.getPieceLength();
    }

    public boolean validateHaveMessage(final HaveMessage message)
            throws InvalidPeerException {
        final Torrent t = message.getDestination().getTorrent();
        return t != null && message.getIndex() < t.getNrPieces();
    }

    public boolean validatePieceMessage(final PieceMessage message)
            throws InvalidPeerException {
        final Torrent t = message.getDestination().getTorrent();
        final int i = message.getIndex();
        final int b = message.getBegin();
        return t != null && i >= 0 && i < t.getNrPieces() && b >= 0 &&
                b < t.getPieceLength() &&
                message.getLength() == TorrentUtil.computeChunkSize(i, b,
                t.getChunkSize(), t.getFileLength(), t.getPieceLength());
    }

    public boolean validateRequestMessage(final BlockMessage message)
            throws InvalidPeerException {
        final Torrent t = message.getDestination().getTorrent();
        final int i = message.getIndex();
        final int b = message.getBegin();
        final int l = message.getLength();
        return t != null && i >= 0 && i < t.getNrPieces() && b >= 0 &&
                b < t.getPieceLength() && l <= t.getChunkSize() && l > 0 &&
                t.isPieceComplete(i);
    }
}
