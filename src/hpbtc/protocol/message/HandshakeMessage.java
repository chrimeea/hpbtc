package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

public class HandshakeMessage extends SimpleMessage {

    private byte[] infoHash;
    private byte[] peerId;
    private byte[] protocol;

    public HandshakeMessage(ByteBuffer message, Peer destination) {
        this.destination = destination;
        message.limit(19);
        protocol = new byte[19];
        message.get(protocol);
        message.limit(47);
        message.position(27);
        infoHash = new byte[20];
        message.get(infoHash);
        if (message.capacity() >= 67) {
            message.limit(67);
            message.position(47);
            peerId = new byte[20];
            message.get(peerId);
        }
    }

    public HandshakeMessage(byte[] infoHash, byte[] peerId, byte[] protocol,
            Peer destination) {
        this.protocol = protocol;
        this.infoHash = infoHash;
        this.peerId = peerId;
        this.destination = destination;
    }

    public byte[] getPeerId() {
        return peerId;
    }

    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(67);
        bb.put(protocol);
        bb.putLong(0L);
        bb.put(infoHash);
        bb.put(peerId);
        return bb;
    }

    public byte[] getProtocol() {
        return protocol;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public void setInfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public void setPeerId(byte[] peerId) {
        this.peerId = peerId;
    }
}
