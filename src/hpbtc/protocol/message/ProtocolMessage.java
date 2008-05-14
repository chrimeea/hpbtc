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
    
    public ProtocolMessage(int len) {
        messageLength = len;
    }
    
    public int getMessageLength() {
        return messageLength;
    }
    
    public abstract ByteBuffer send();
}
