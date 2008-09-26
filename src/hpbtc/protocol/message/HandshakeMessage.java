package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

public class HandshakeMessage extends LengthPrefixMessage {

    private byte[] infoHash;
    private byte[] protocol;
    private byte[] peerId;

    public HandshakeMessage(final ByteBuffer message, final Peer destination) {
        super(48, destination);
        message.limit(20);
        protocol = new byte[20];
        message.get(protocol);
        message.limit(48);
        message.position(28);
        infoHash = new byte[20];
        message.get(infoHash);
    }

    public HandshakeMessage(final byte[] peerId, final byte[] protocol,
            final Peer destination, byte[] infoHash) {
        super(68, destination);
        this.protocol = protocol;
        this.infoHash = infoHash;
        this.peerId = peerId;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    @Override
    public ByteBuffer send() {
        final ByteBuffer bb = ByteBuffer.allocate(peerId == null ? 48 : 68);
        bb.put(protocol);
        bb.putLong(0L);
        bb.put(infoHash);
        if (peerId != null) {
            bb.put(peerId);
        }
        return bb;
    }

    public byte[] getProtocol() {
        return protocol;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(final byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public void setPeerId(final byte[] peerId) {
        this.peerId = peerId;
    }
}
