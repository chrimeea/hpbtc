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
public class UnchokeMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 1;
    
    public UnchokeMessage() {
        super(1);
    }
    
    public UnchokeMessage(int len) {
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
