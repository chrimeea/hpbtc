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
public class UnchokeMessage extends ProtocolMessage {

    public UnchokeMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message, MessageProcessor processor) {
        processor.process(this);
        super.process(message, processor);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type UNCHOKE";
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.putInt(1);
        bb.put(TYPE_UNCHOKE);
        return bb;
    }
}
