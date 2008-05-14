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
public class HaveMessage extends ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 4;
    private int index;
    
    public HaveMessage(ByteBuffer message, int len) {
        super(len, TYPE_DISCRIMINATOR);
        index = message.getInt();
    }
    
    public HaveMessage(int index) {
        super(5, TYPE_DISCRIMINATOR);
        this.index = index;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.putInt(index);
        return bb;
    }

    public int getIndex() {
        return index;
    }
}
