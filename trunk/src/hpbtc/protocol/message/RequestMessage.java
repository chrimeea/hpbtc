package hpbtc.protocol.message;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class RequestMessage extends BlockMessage implements Cloneable {

    public static final byte TYPE_DISCRIMINATOR = 6;
    
    public RequestMessage(ByteBuffer message, int len) {
        super(message, len);
    }

    public RequestMessage(int begin, int index, int length) {
        super(begin, index, length);
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.putInt(13);
        bb.put(TYPE_DISCRIMINATOR);
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        return bb;
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RequestMessage(begin, index, length);
    }
}
