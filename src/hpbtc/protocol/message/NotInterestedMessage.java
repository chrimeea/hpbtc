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
public class NotInterestedMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 3;
    
    public NotInterestedMessage() {
        super(1);
    }
    
    public NotInterestedMessage(int len) {
        super(len);
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.putInt(1);
        bb.put(TYPE_DISCRIMINATOR);
        return bb;
    }
}
