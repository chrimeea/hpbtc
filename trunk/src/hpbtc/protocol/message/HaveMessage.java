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
public class HaveMessage extends ProtocolMessage {

    private static Logger logger = Logger.getLogger(HaveMessage.class.getName());
    
    private int index;
    
    public HaveMessage() {
    }
    
    public HaveMessage(int index) {
        this.index = index;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message,MessageProcessor processor) {
        index = message.getInt();
        processor.process(this);
        super.process(message, processor);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type HAVE";
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        logger.info("send message " + this);
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.putInt(5);
        bb.put(TYPE_HAVE);
        bb.putInt(index);
        return bb;
    }

    public int getIndex() {
        return index;
    }
}