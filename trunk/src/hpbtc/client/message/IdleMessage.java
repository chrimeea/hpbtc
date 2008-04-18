/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.client.message;

import hpbtc.client.Client;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class IdleMessage extends ProtocolMessage {

    public IdleMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process(ByteBuffer message) {
        Client.getInstance().getObserver().fireProcessMessageEvent(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type IDLE";
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(0);
        return bb;
    }

}
