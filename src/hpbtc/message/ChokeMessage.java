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
public class ChokeMessage extends ProtocolMessage {

    public ChokeMessage() {
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        Client.getInstance().getObserver().fireProcessMessageEvent(this);
        peer.setChokedThere(true);
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type CHOKE, peer " + peer.getIp();
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(5);
        bb.putInt(1);
        bb.put(ProtocolMessage.TYPE_CHOKE);
        return bb;
    }

}
