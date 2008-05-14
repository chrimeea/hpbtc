package hpbtc.protocol.message;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class RequestMessage extends BlockMessage implements Cloneable {

    public static final byte TYPE_DISCRIMINATOR = 6;
    
    public RequestMessage(ByteBuffer message, int len) {
        super(message, len, TYPE_DISCRIMINATOR);
    }

    public RequestMessage(int begin, int index, int length) {
        super(begin, index, length, TYPE_DISCRIMINATOR);
    }
    
    @Override
    public Object clone() throws CloneNotSupportedException {
        return new RequestMessage(begin, index, length);
    }
}
