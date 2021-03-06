/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;

/**
 * @author Cristian Mocanu
 *
 */
public class HaveMessage extends SimpleMessage {

    private int index;
    
    public HaveMessage(final ByteBuffer message, final Peer destination) {
        super(4, TYPE_HAVE, destination);
        index = message.getInt();
    }
    
    public HaveMessage(final int index, final Peer destination) {
        super(4, TYPE_HAVE, destination);
        this.index = index;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        final ByteBuffer bb = super.send();
        bb.putInt(index);
        return bb;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return super.toString() + ", Index: " + index;
    }
}
