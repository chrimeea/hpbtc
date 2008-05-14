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
public class CancelMessage extends BlockMessage {

    public static final byte TYPE_DISCRIMINATOR = 8;

    public CancelMessage(ByteBuffer message, int len) {
        super(message, len);
    }
    
    /**
     * @param p
     */
    public CancelMessage(int begin, int index, int length) {
        super(begin, index, length);
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.put(TYPE_DISCRIMINATOR);
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        return bb;
    }
}
