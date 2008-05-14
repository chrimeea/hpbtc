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
public class InterestedMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 2;
    
    public InterestedMessage() {
        super(1);
    }
    
    public InterestedMessage(int len) {
        super(len);
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.put(TYPE_DISCRIMINATOR);
        return bb;
    }
}
