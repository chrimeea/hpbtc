/*
 * Created on Mar 6, 2006
 *
 */
package hpbtc.protocol.message;

import hpbtc.protocol.torrent.Peer;
import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * @author Cristian Mocanu
 *
 */
public class BitfieldMessage extends SimpleMessage {
    
    private BitSet pieces;
    
    public BitfieldMessage(final ByteBuffer message, final Peer destination) {
        super(message.remaining(), TYPE_BITFIELD, destination);
        pieces = bytesToBits(message);
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
        final ByteBuffer bb = super.send();
        bitsToBytes(pieces, bb);
        return bb;
    }
    
    public BitSet getBitfield() {
        return pieces;
    }

    @Override
    public String toString() {
        return super.toString() + ", Pieces: " + pieces.cardinality();
    }

    static BitSet bytesToBits(final ByteBuffer bb) {
        int len = bb.remaining();
        int j = 0;
        BitSet pieces = new BitSet(len * 8);
        for (int i = 0; i < len; i++) {
            byte bit = bb.get();
            byte c = (byte) 128;
            for (int p = 0; p < 8; p++) {
                if ((bit & c) == c) {
                    pieces.set(j);
                }
                bit <<= 1;
                j++;
            }
        }
        return pieces;
    }

    static void bitsToBytes(final BitSet bs, final ByteBuffer dest) {
        int len = bs.length();
        byte x = 0;
        byte y = (byte) -128;
        for (int i = 0; i < len; i++) {
            if (i % 8 == 0 && i != 0) {
                dest.put(x);
                x = 0;
                y = (byte) -128;
            }
            if (bs.get(i)) {
                x |= y;
            }
            y >>= 1;
            if (y < 0) {
                y ^= (byte) -128;
            }
        }
        if (dest.remaining() > 0) {
            dest.put(x);
        }
    }
}
