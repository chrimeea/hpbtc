/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

/**
 * @author chris
 *
 */
public class HaveMessage extends SimpleMessage {

    private int index;
    
    public HaveMessage(ByteBuffer message, Peer destination) {
        super(4, TYPE_HAVE, destination);
        index = message.getInt();
    }
    
    public HaveMessage(int index, Peer destination) {
        super(4, TYPE_HAVE, destination);
        this.index = index;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.putInt(index);
        return bb;
    }

    public int getIndex() {
        return index;
    }
}
