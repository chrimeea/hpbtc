/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.client.message;

import hpbtc.client.Client;

import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class HaveMessage extends ProtocolMessage {

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
    public void process(ByteBuffer message) {
        index = message.getInt();
        Client.getInstance().getObserver().fireProcessMessageEvent(this);
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
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.putInt(5);
        bb.put(ProtocolMessage.TYPE_HAVE);
        bb.putInt(index);
        return bb;
    }

    public int getIndex() {
        return index;
    }
}
