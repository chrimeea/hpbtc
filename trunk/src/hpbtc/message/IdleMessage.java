/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.message;

import hpbtc.Client;

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
    public void process() {
        Client.getInstance().getObserver().fireProcessMessageEvent(this);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type IDLE, peer " + peer.getIp();
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(0);
        return bb;
    }

}
