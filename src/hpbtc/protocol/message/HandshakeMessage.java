package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

public class HandshakeMessage extends SimpleMessage {

    private byte[] infoHash;
    private byte[] peerId;
    private byte[] protocol;

    public HandshakeMessage(ByteBuffer message, Peer destination) {
        this.destination = destination;
        byte pstrlen = message.get();
        if (pstrlen == 19) {
            message.limit(20);
            protocol = new byte[19];
            message.get(protocol);
            message.limit(48);
            message.position(28);
            infoHash = new byte[20];
            message.get(infoHash);
            if (message.capacity() >= 68) {
                message.limit(68);
                message.position(48);
                peerId = new byte[20];
                message.get(peerId);
            }
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
        ByteBuffer bb = ByteBuffer.allocate(68);
        bb.put((byte) 19);
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
