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
public class InterestedMessage implements ProtocolMessage {

    public static final byte TYPE_DISCRIMINATOR = 2;
    
    public InterestedMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(MessageProcessor processor) {
        processor.process(this);
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