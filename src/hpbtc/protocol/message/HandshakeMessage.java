package hpbtc.protocol.message;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class HandshakeMessage implements ProtocolMessage {

    private byte[] infoHash;

    public HandshakeMessage(ByteBuffer message) throws IOException {
        if (message.limit() < 28) {
            throw new EOFException("wrong hanshake");
        }
        message.limit(20);
        ByteBuffer h = ByteBuffer.allocate(20);
        getProtocol(h);
        h.rewind();
        if (!message.equals(h)) {
            throw new IOException("wrong hanshake");
        }
        message.position(28);
        if (message.remaining() >= 20) {
            message.limit(48);
            message.get(infoHash);
        }
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(48);
        getProtocol(bb);
        for (int i = 0; i < 8; i++) {
            bb.put((byte) 0);
        }
        bb.put(infoHash);
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
}
