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
public class HaveMessage implements ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 4;
    
    private int index;
    
    public HaveMessage(ByteBuffer message) {
        index = message.getInt();
    }
    
    public HaveMessage(int index) {
        this.index = index;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.putInt(5);
        bb.put(TYPE_DISCRIMINATOR);
        bb.putInt(index);
        return bb;
    }

    public int getIndex() {
        return index;
    }
}
