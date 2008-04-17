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
public class CancelMessage extends BlockMessage {
    
    /**
     * @param p
     */
    public CancelMessage() {
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#process(java.nio.ByteBuffer)
     */
    @Override
    public void process() {
        index = message.getInt();
        begin = message.getInt();
        length = message.getInt();
        peer.getConnection().cancelRequestReceived(this);
        Client.getInstance().getObserver().fireProcessMessageEvent(this);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "type CANCEL, peer " + peer.getIp() + " index " + (index + 1) + " begin " + begin + " length" + length;
    }
    
    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() throws IOException {
        Client.getInstance().getObserver().fireSendMessageEvent(this);
        ByteBuffer bb = ByteBuffer.allocate(17);
        bb.putInt(13);
        bb.put(ProtocolMessage.TYPE_CANCEL);
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        return bb;
    }
}
