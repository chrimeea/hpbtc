/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * @author chris
 *
 */
public class BitfieldMessage extends ProtocolMessage {
    
    public static final byte TYPE_DISCRIMINATOR = 5;
    
    private BitSet pieces;
    
    public BitfieldMessage(ByteBuffer message, int len) throws IOException {
        super(len);
        int l = message.remaining();
        if (l < len) {
            throw new EOFException("wrong message");
        }
        int j = 0;
        int k = 0;
        pieces = new BitSet(len * 8);
        for (int i = 0; i < len; i++) {
            byte bit = message.get();
            byte c = (byte) 128;
            for (int p = 0; p < 8; p++) {
                if ((bit & c) == c) {
                    pieces.set(j);
                    k++;
                }
                bit <<= 1;
                j++;
            }
        }
    }
    
    public BitfieldMessage(BitSet pieces) {
        super((int) Math.ceil(pieces.size() / 8.0));
        this.pieces = pieces;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        byte x = TYPE_DISCRIMINATOR;
        byte y = (byte) -128;
        boolean hasp = false;
        for (int i = 0; i < messageLength; i++) {
            if (i % 8 == 0) {
                bb.put(x);
                x = 0;
                y = (byte) -128;
            }
            if (pieces.get(i)) {
                x |= y;
                hasp = true;
            }
            y >>= 1;
            if (y < 0) {
                y ^= (byte) -128;
            }
        }
        if (hasp) {
            if (y != 0) {
                bb.put(x);
            }
            return bb;
        }
        return ByteBuffer.allocate(0);
    }
    
    public BitSet getBitfield() {
        return pieces;
    }
}
