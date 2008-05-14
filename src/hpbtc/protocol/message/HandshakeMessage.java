package hpbtc.protocol.message;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HandshakeMessage extends ProtocolMessage {

    private byte[] infoHash;
    private byte[] peerId;

    public HandshakeMessage(ByteBuffer message) throws IOException {
        super(68, (byte) 0);
        if (message.remaining() < 48) {
            throw new EOFException("wrong hanshake");
        }
        message.limit(20);
        ByteBuffer h = ByteBuffer.allocate(20);
        getProtocol(h);
        h.rewind();
        if (!message.equals(h)) {
            throw new IOException("wrong hanshake");
        }
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

    public HandshakeMessage(byte[] infoHash, byte[] peerId) {
        super(68, (byte) 0);
        this.infoHash = infoHash;
        this.peerId = peerId;
    }
    
    public byte[] getPeerId() {
        return peerId;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(68);
        getProtocol(bb);
        for (int i = 0; i < 8; i++) {
            bb.put((byte) 0);
        }
        bb.put(infoHash);
        bb.put(peerId);
        return bb;
    }

    private void getProtocol(ByteBuffer pr) {
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes());
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
