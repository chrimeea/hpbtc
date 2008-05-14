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
public class IdleMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 0;
    
    public IdleMessage() {
        super(0);
    }
    
    public IdleMessage(int len) {
        super(len);
    }
    
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.put(TYPE_DISCRIMINATOR);
        return bb;
    }
}
