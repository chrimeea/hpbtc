package hpbtc.protocol.message;

import java.nio.ByteBuffer;

public class HandshakeMessage implements ProtocolMessage {

    private ByteBuffer infoHash;
    
    public HandshakeMessage(ByteBuffer message) {
        message.limit(20);
        ByteBuffer h = ByteBuffer.allocate(20);
        getProtocol(h);
        h.rewind();
        if (message.equals(h)) {
            message.limit(48);
            message.position(28);
            infoHash = message;
        }
        //TODO process error if message not equal h
    }
    
    public HandshakeMessage(byte[] infoHash) {
        this.infoHash = ByteBuffer.wrap(infoHash);
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(MessageProcessor processor) {
        processor.process(this);
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
    
    public void getProtocol(ByteBuffer pr) {
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes());        
    }

    public ByteBuffer getInfoHash() {
        return infoHash;
    }
}
