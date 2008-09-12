/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;
import java.util.BitSet;
import util.IOUtil;

/**
 * @author Cristian Mocanu
 *
 */
public class BitfieldMessage extends SimpleMessage {
    
    private BitSet pieces;
    
    public BitfieldMessage(final ByteBuffer message, final Peer destination) {
        super(message.remaining(), TYPE_BITFIELD, destination);
        pieces = IOUtil.bytesToBits(message);
    }
    
    public BitfieldMessage(final BitSet pieces, final int nPieces,
            final Peer destination) {
        super((int) Math.ceil(nPieces / 8.0), TYPE_BITFIELD, destination);
        this.pieces = pieces;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        IOUtil.bitsToBytes(pieces, bb);
        return bb;
    }
    
    public BitSet getBitfield() {
        return pieces;
    }
}
