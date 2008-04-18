package hpbtc.protocol;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class HandshakeMessage extends ProtocolMessage {

    private static Logger logger = Logger.getLogger(HandshakeMessage.class.getName());
    
    private ByteBuffer infoHash;
    
    public HandshakeMessage() {
    }
    
    public HandshakeMessage(byte[] infoHash) {
        this.infoHash = ByteBuffer.wrap(infoHash);
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message,MessageProcessor processor) {
        logger.info("process message " + this);
        message.limit(20);
        ByteBuffer h = ByteBuffer.allocate(20);
        getProtocol(h);
        h.rewind();
        if (message.equals(h)) {
            message.limit(48);
            message.position(28);
            infoHash = message;
            processor.process(this);
            super.process(message, processor);
        }
        //TODO process error if message not equal h
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type HANDSHAKE";
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        logger.info("send message " + this);
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
