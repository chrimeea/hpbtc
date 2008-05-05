/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class CancelMessage extends BlockMessage {

    private static Logger logger = Logger.getLogger(CancelMessage.class.getName());
    public static final byte TYPE_DISCRIMINATOR = 8;

    public CancelMessage(ByteBuffer message) {
        super(message);
    }
    
    /**
     * @param p
     */
    public CancelMessage(int begin, int index, int length) {
        super(begin, index, length);
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(MessageProcessor processor) {
        processor.process(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type CANCEL";
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        logger.info("send message " + this);
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.putInt(13);
        bb.put(TYPE_DISCRIMINATOR);
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        return bb;
    }
}
