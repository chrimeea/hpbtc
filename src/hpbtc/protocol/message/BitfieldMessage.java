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
    
    public BitfieldMessage(ByteBuffer message, int len) throws IOException {
        super(len, TYPE_BITFIELD);
        pieces = IOUtil.bytesToBits(message);
    }
    
    public BitfieldMessage(BitSet pieces) {
        super((int) Math.ceil(pieces.size() / 8.0), TYPE_BITFIELD);
        this.pieces = pieces;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.put(IOUtil.bitsToBytes(pieces));
        return bb;
    }
    
    public BitSet getBitfield() {
        return pieces;
    }
}
