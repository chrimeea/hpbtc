package hpbtc.protocol.message;

import java.nio.ByteBuffer;
import java.util.logging.Logger;

public class PIDMessage implements ProtocolMessage {

    private static Logger logger = Logger.getLogger(PIDMessage.class.getName());
    
    ByteBuffer pid;
    
    public PIDMessage(ByteBuffer message) {
        message.limit(20);
        pid = message;
    }

    public PIDMessage(byte[] pid) {
        this.pid = ByteBuffer.wrap(pid);
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
        return "type PID";
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        logger.info("send message " + this);
        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.put(pid);
        return bb;
    }

    public ByteBuffer getPid() {
        return pid;
    }
}
