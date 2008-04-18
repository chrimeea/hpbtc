/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol;

import hpbtc.client.Client;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class InterestedMessage extends ProtocolMessage {

    private static Logger logger = Logger.getLogger(InterestedMessage.class.getName());
    
    public InterestedMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message,MessageProcessor processor) {
        processor.process(this);
        super.process(message, processor);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type INTERESTED";
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        logger.info("send message " + this);
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.putInt(1);
        bb.put(TYPE_INTERESTED);
        return bb;
    }
}
