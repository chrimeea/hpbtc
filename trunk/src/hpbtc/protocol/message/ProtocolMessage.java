/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public abstract class ProtocolMessage {
    
    protected int messageLength;
    protected byte disc;
    
    public ProtocolMessage(int len, byte disc) {
        messageLength = len;
        this.disc = disc;
    }
    
    public int getMessageLength() {
        return messageLength;
    }
    
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(messageLength + 4);
        bb.putInt(messageLength);
        bb.put(disc);
        return bb;
    }
}
