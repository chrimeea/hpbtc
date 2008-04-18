package hpbtc.client.message;

import hpbtc.client.Client;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class RequestMessage extends BlockMessage implements Cloneable {
    
    public RequestMessage() {
    }

    public RequestMessage(int begin, int index, int length) {
        super(begin, index, length);
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message) {
        index = message.getInt();
        begin = message.getInt();
        length = message.getInt();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type REQUEST";
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.putInt(13);
        bb.put(ProtocolMessage.TYPE_REQUEST);
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
