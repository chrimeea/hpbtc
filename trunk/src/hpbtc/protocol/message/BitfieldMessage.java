/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * @author chris
 *
 */
public class BitfieldMessage implements ProtocolMessage {
    
    public static final byte TYPE_DISCRIMINATOR = 5;
    
    private BitSet pieces;

    public BitfieldMessage(ByteBuffer message) {
        int j = 0;
        int k = 0;
        int l = message.remaining();
        for (int i = 0; i < l; i++) {
            byte bit = message.get();
            byte c = (byte) 128;
            for (int p = 0; p < 8; p++) {
                if ((bit & c) == c) {
                    pieces.set(j);
                    k++;
                }
                bit <<= 1;
                j++;
                //Might read more than the number of actual pieces
            }
        }
    }
    
    public BitfieldMessage(BitSet pieces) {
        this.pieces = pieces;
    }

    /* (non-Javadoc)
     * @see hpbtc.message.ProtocolMessage#send()
     */
    @Override
    public ByteBuffer send() {
        int n = 1 + pieces.size() / 8;
        if (pieces.size() % 8 > 0) {
            n++;
        }
        ByteBuffer bb = ByteBuffer.allocate(n + 4);
        bb.putInt(n);
        byte x = TYPE_DISCRIMINATOR;
        byte y = (byte) -128;
        boolean hasp = false;
        for (int i = 0; i < pieces.size(); i++) {
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
