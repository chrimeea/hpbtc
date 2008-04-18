package hpbtc.client.message;

import hpbtc.client.Client;

import java.nio.ByteBuffer;

public class PIDMessage extends ProtocolMessage {
    
    ByteBuffer pid;
    
    public PIDMessage() {
    }

    public PIDMessage(byte[] pid) {
        this.pid = ByteBuffer.wrap(pid);
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message,MessageProcessor processor) {
        message.limit(20);
        pid = message;
        processor.process(this);
        super.process(message, processor);
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
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(20);
        bb.put(pid);
        return bb;
    }

    public ByteBuffer getPid() {
        return pid;
    }
}
