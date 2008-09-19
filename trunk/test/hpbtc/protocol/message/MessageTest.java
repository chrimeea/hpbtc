package hpbtc.protocol.message;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import org.junit.Test;
import java.util.Arrays;

/**
 *
 * @author Cristian Mocanu
 */
public class MessageTest {

    private String byteEncoding = "US-ASCII";
    
    private byte[] getSupportedProtocol() throws UnsupportedEncodingException {
        byte[] protocol = new byte[20];
        ByteBuffer pr = ByteBuffer.wrap(protocol);
        pr.put((byte) 19);
        pr.put("BitTorrent protocol".getBytes(byteEncoding));
        return protocol;        
    }
    
    @Test
    public void testReadBitfield() {
        ByteBuffer bb = ByteBuffer.allocate(3);
        bb.putShort((short) 63472);
        bb.put((byte) 0);
        bb.rewind();
        BitfieldMessage m = new BitfieldMessage(bb, null);
        BitSet bs = m.getBitfield();
        assert bs.nextSetBit(0) == 0;
        assert bs.nextSetBit(1) == 1;
        assert bs.nextSetBit(2) == 2;
        assert bs.nextSetBit(3) == 3;
        assert bs.nextSetBit(4) == 5;
        assert bs.nextSetBit(6) == 6;
        assert bs.nextSetBit(7) == 7;
        assert bs.nextSetBit(8) == 8;
        assert bs.nextSetBit(9) == 9;
        assert bs.nextSetBit(10) == 10;
        assert bs.nextSetBit(11) == 11;
        assert bs.nextSetBit(12) == -1;
        assert m.getMessageLength() == 4;
    }
    
    @Test
    public void testWriteBitfield() {
        BitSet bs = new BitSet(20);
        bs.set(1); bs.set(2); bs.set(3); bs.set(7);
        bs.set(8); bs.set(9); bs.set(13); bs.set(14);
        BitfieldMessage m = new BitfieldMessage(bs, 20, null);
        ByteBuffer bb = m.send();
        bb.rewind();
        assert bb.getInt() == 4;
        assert bb.get() == SimpleMessage.TYPE_BITFIELD;
        assert bb.getShort() == 29126;
        assert bb.get() == 0;
        assert bb.remaining() == 0;
    }
    
    @Test
    public void testReadHandshake() throws UnsupportedEncodingException {
        ByteBuffer bb = ByteBuffer.allocate(68);
        bb.put((byte) 19);
        bb.put("BitTorrent protocol".getBytes(byteEncoding));
        bb.putLong(0L);
        bb.put("01234567890123456789".getBytes(byteEncoding));
        bb.put("ABCDEFGHIJKLMNOPQRST".getBytes(byteEncoding));
        bb.rewind();
        HandshakeMessage m = new HandshakeMessage(bb, null);
        assert Arrays.equals(getSupportedProtocol(), m.getProtocol());
        assert "01234567890123456789".equals(new String(m.getInfoHash(), byteEncoding));
        assert m.getPeerId() == null;
    }
    
    @Test
    public void testNullPeerInfoHandshake() throws UnsupportedEncodingException {
        ByteBuffer bb = ByteBuffer.allocate(48);
        bb.put((byte) 19);
        bb.put("BitTorrent protocol".getBytes(byteEncoding));
        bb.putLong(0L);
        bb.put("01234567890123456789".getBytes(byteEncoding));
        bb.rewind();
        HandshakeMessage m = new HandshakeMessage(bb, null);
        assert Arrays.equals(getSupportedProtocol(), m.getProtocol());
        assert "01234567890123456789".equals(new String(m.getInfoHash(), byteEncoding));
        assert m.getPeerId() == null;
        bb = m.send();
        bb.rewind();
        assert bb.get() == 19;
        byte[] p = new byte[19];
        bb.get(p);
        assert "BitTorrent protocol".equals(new String(p, byteEncoding));
        assert bb.getLong() == 0L;
        byte[] i = new byte[20];
        bb.get(i);
        assert "01234567890123456789".equals(new String(i, byteEncoding));
    }
    
    @Test
    public void testWriteHandshake() throws UnsupportedEncodingException {
        HandshakeMessage m = new HandshakeMessage(
                "ABCDEFGHIJKLMNOPQRST".getBytes(byteEncoding),
                getSupportedProtocol(), null);
        ByteBuffer bb = m.send();
        bb.rewind();
        assert bb.get() == 19;
        byte[] p = new byte[19];
        bb.get(p);
        assert "BitTorrent protocol".equals(new String(p, byteEncoding));
        assert bb.getLong() == 0L;
        byte[] i = new byte[20];
        bb.get(i);
        assert "01234567890123456789".equals(new String(i, byteEncoding));
        bb.get(i);
        assert "ABCDEFGHIJKLMNOPQRST".equals(new String(i, byteEncoding));
        assert !bb.hasRemaining();
    }
    
    @Test
    public void testReadHave() {
        ByteBuffer bb = ByteBuffer.allocate(9);
        bb.putInt(1234);
        bb.rewind();
        HaveMessage m = new HaveMessage(bb, null);
        assert m.getIndex() == 1234;
        assert m.getMessageLength() == 5;
    }
    
    @Test
    public void testWriteHave() {
        HaveMessage m = new HaveMessage(548677, null);
        ByteBuffer bb = m.send();
        bb.rewind();
        assert bb.getInt() == 5;
        assert bb.get() == SimpleMessage.TYPE_HAVE;
        assert bb.getInt() == 548677;
        assert bb.remaining() == 0;
    }
    
    @Test
    public void testReadPiece() {
        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.putInt(456);
        bb.putInt(123);
        bb.putInt(46734);
        bb.rewind();
        PieceMessage m = new PieceMessage(bb, null);
        ByteBuffer b = m.getPiece();
        assert m.getBegin() == 123;
        assert m.getIndex() == 456;
        assert b.getInt() == 46734;
        assert b.remaining() == 0;
        assert m.getLength() == 4;
        assert m.getMessageLength() == 13;
    }
    
    @Test
    public void testWritePiece() {
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(99);
        bb.rewind();
        PieceMessage m = new PieceMessage(11, 70, bb, 4, null);
        ByteBuffer s = m.send();
        s.rewind();
        assert s.getInt() == 13;
        assert s.get() == SimpleMessage.TYPE_PIECE;
        assert s.getInt() == 70;
        assert s.getInt() == 11;
        assert s.getInt() == 99;
        assert s.remaining() == 0;
    }
    
    @Test
    public void testReadBlock() {
        ByteBuffer bb = ByteBuffer.allocate(12);
        bb.putInt(1);
        bb.putInt(2);
        bb.putInt(3);
        bb.rewind();
        BlockMessage m = new BlockMessage(bb, SimpleMessage.TYPE_REQUEST, null);
        assert m.getBegin() == 2;
        assert m.getIndex() == 1;
        assert m.getLength() == 3;
        assert m.getMessageLength() == 13;
    }
    
    @Test
    public void testWriteBlock() {
        BlockMessage m = new BlockMessage(1, 2, 3, SimpleMessage.TYPE_CANCEL, null);
        ByteBuffer bb = m.send();
        bb.rewind();
        assert bb.getInt() == 13;
        assert bb.get() == SimpleMessage.TYPE_CANCEL;
        assert bb.getInt() == 2;
        assert bb.getInt() == 1;
        assert bb.getInt() == 3;
        assert bb.remaining() == 0;
    }
    
    @Test
    public void testWriteSimple() {
        SimpleMessage m = new SimpleMessage(0, SimpleMessage.TYPE_INTERESTED, null);
        ByteBuffer bb = m.send();
        bb.rewind();
        assert bb.getInt() == 1;
        assert bb.get() == SimpleMessage.TYPE_INTERESTED;
        assert bb.remaining() == 0;
    }
}
