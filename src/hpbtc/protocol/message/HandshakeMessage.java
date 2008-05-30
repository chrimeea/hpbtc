package hpbtc.protocol.message;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HandshakeMessage extends EmptyMessage {

    private byte[] infoHash;
    private byte[] peerId;
    private byte[] protocol;

    public HandshakeMessage(ByteBuffer message) throws IOException {
        if (message.remaining() < 48) {
            throw new EOFException("wrong hanshake");
        }
        message.limit(20);
        message.get(protocol);
        message.limit(48);
        message.position(28);
        message.get(infoHash);
        if (message.remaining() >= 20) {
            message.limit(68);
            message.position(48);
            peerId = new byte[20];
            message.get(peerId);
        }
    }

    public HandshakeMessage(byte[] infoHash, byte[] peerId, byte[] protocol) {
        this.protocol = protocol;
        this.infoHash = infoHash;
        this.peerId = peerId;
    }
    
    public byte[] getPeerId() {
        return peerId;
    }

    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(68);
        bb.put(protocol);
        for (int i = 0; i < 8; i++) {
            bb.put((byte) 0);
        }
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
