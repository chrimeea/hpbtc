package hpbtc.protocol.message;

import java.nio.ByteBuffer;

public class PIDMessage implements ProtocolMessage {

    private ByteBuffer pid;
    
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
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.put(pid);
        return bb;
    }

    public ByteBuffer getPid() {
        return pid;
    }
}
