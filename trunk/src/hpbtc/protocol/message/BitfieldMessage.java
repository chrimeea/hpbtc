/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import util.IOUtil;

/**
 * @author chris
 *
 */
public class BitfieldMessage extends SimpleMessage {
    
    private BitSet pieces;
    
    public BitfieldMessage(ByteBuffer message, int len) {
        super(len, TYPE_BITFIELD);
        pieces = IOUtil.bytesToBits(message);
    }
    
    public BitfieldMessage(BitSet pieces, int nPieces) {
        super((int) Math.ceil(nPieces / 8.0), TYPE_BITFIELD);
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
