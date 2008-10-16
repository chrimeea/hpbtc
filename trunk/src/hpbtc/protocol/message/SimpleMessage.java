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
public class SimpleMessage extends LengthPrefixMessage {
    
    public static final byte TYPE_BITFIELD = 5;
    public static final byte TYPE_CANCEL = 8;
    public static final byte TYPE_CHOKE = 0;
    public static final byte TYPE_HAVE = 4;
    public static final byte TYPE_INTERESTED = 2;
    public static final byte TYPE_NOT_INTERESTED = 3;
    public static final byte TYPE_PIECE = 7;
    public static final byte TYPE_REQUEST = 6;
    public static final byte TYPE_UNCHOKE = 1;

    protected byte disc;
    
    public void setDestination(final Peer destination) {
        this.destination = destination;
    }
    
    public SimpleMessage(final byte disc, final Peer destination) {
        this(0, disc, destination);
    }
    
    public SimpleMessage(final int len, final byte disc,
            final Peer destination) {
        super(len + 1, destination);
        this.disc = disc;
    }
    
    public byte getMessageType() {
        return disc;
    }
    
    @Override
    public boolean isPriorityMessage() {
        return disc == TYPE_CANCEL || disc == TYPE_CHOKE;
    }
    
    @Override
    public ByteBuffer send() {
        final ByteBuffer prefix = super.send();
        prefix.rewind();
        final ByteBuffer bb = ByteBuffer.allocate(messageLength + 4);
        bb.put(prefix);
        bb.put(disc);
        return bb;
    }

    @Override
    public String toString() {
        return super.toString() + ", Type: " + disc;
    }
}
