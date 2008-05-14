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
public class HaveMessage extends EmptyMessage {

    private int index;
    
    public HaveMessage(ByteBuffer message) {
        super(5, TYPE_HAVE);
        index = message.getInt();
    }
    
    public HaveMessage(int index) {
        super(5, TYPE_HAVE);
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
