package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedList;
import java.util.List;
import org.junit.Test;
import util.IOUtil;
import hpbtc.protocol.message.BlockMessage;
import hpbtc.protocol.message.SimpleMessage;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.BitSet;
import util.ChannelStub;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentTest {
    
    @Test
    public void testTorrentInfo() throws IOException, NoSuchAlgorithmException {
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".getBytes("ISO-8859-1"));
        Torrent info = new Torrent(b, ".");
        b.close();
        assert info.getFileLength() == 85;
        assert info.getComment().equals("test comment");
        assert info.getCreatedBy().equals("uTorrent/1770");
        assert info.getCreationDate().getTime() == 1209116668000L;
        assert info.getEncoding().equals("UTF-8");
        assert info.getPieceLength() == 65536;
        assert info.getNrPieces() == 1;
        List<LinkedList<String>> trackers = info.getTrackers();
        assert trackers.size() == 1;
        List<String> l = trackers.get(0);
        assert l.size() == 1;
        assert l.get(0).equals("http://www.test.ro/announce");
        List<BTFile> files = info.getFiles();
        assert files.size() == 1;
        BTFile f = files.get(0);
        assert f.getLength() == 85;
        assert f.getPath().equals(".\\manifest.mf");
    }
    
    @Test
    public void testSavePiece() throws IOException, NoSuchAlgorithmException {
        File f = File.createTempFile("hpbtc-save-piece", "test");
        String t = "d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BencodingWriter wr = new BencodingWriter(bos);
        wr.write(f.getName());
        String s = new String(bos.toByteArray(), "ISO-8859-1");
        t += s + "12:piece lengthi65536e6:pieces20:12345678901234567890ee";        
        bos.close();
        ByteArrayInputStream b = new ByteArrayInputStream(t.getBytes("ISO-8859-1"));
        Torrent info = new Torrent(b, f.getParent());
        b.close();
        ByteBuffer piece = ByteBuffer.allocate(info.getPieceLength());
        for (int i = 0; i < info.getPieceLength(); i++) {
            piece.put((byte) 5);
        }
        piece.rewind();
        info.savePiece(0, 0, piece);
        ByteBuffer dest = ByteBuffer.allocate(info.getPieceLength());
        IOUtil.readFromFile(f, 0, dest);
        f.delete();
        dest.rewind();
        piece.rewind();
        assert dest.equals(piece);
    }
    
    @Test
    public void testLoadPiece() throws IOException, NoSuchAlgorithmException {
        File f = File.createTempFile("hpbtc-load-piece", "test");
        String t = "d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        BencodingWriter wr = new BencodingWriter(bos);
        wr.write(f.getName());
        String s = new String(bos.toByteArray(), "ISO-8859-1");
        t += s + "12:piece lengthi65536e6:pieces20:12345678901234567890ee";        
        bos.close();
        ByteArrayInputStream b = new ByteArrayInputStream(t.getBytes("ISO-8859-1"));
        Torrent info = new Torrent(b, f.getParent());
        b.close();
        ByteBuffer piece = ByteBuffer.allocate(info.getPieceLength());
        for (int i = 0; i < info.getPieceLength(); i++) {
            piece.put((byte) 5);
        }
        piece.rewind();
        IOUtil.writeToFile(f, 0, piece);
        piece.rewind();
        ByteBuffer dest = info.loadPiece(0, 0, info.getPieceLength());
        f.delete();
        dest.rewind();
        assert piece.equals(dest);
    }
    
    @Test
    public void testDecideNextPiece() throws IOException,
            NoSuchAlgorithmException {
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".getBytes("ISO-8859-1"));
        Torrent info = new Torrent(b, ".");
        b.close();
        Peer peer = new Peer(null, null, null);
        peer.setPiece(0);
        int np = info.getNrPieces();
        BitSet[] req = new BitSet[np];
        for (int i = 0; i < np; i++) {
            req[i] = new BitSet();
        }
        BlockMessage bm = info.decideNextPiece(peer, req);
        assert bm.getBegin() == 0;
        assert bm.getIndex() == 0;
        assert bm.getLength() == 85;
    }
    
    @Test
    public void testDecideChoking() throws IOException,
            NoSuchAlgorithmException {
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".getBytes("ISO-8859-1"));
        Torrent info = new Torrent(b, ".");
        b.close();
        ChannelStub cs = new ChannelStub(0, false);
        InetAddress ia = InetAddress.getLocalHost();
        Peer peer = new Peer(new InetSocketAddress(ia, 9001), null, null);
        peer.setChannel(cs);peer.setHandshakeReceived();info.addPeer(peer);
        peer = new Peer(new InetSocketAddress(ia, 9002), null, null);
        peer.setChannel(cs);peer.setHandshakeReceived();info.addPeer(peer);
        peer = new Peer(new InetSocketAddress(ia, 9003), null, null);
        peer.setChannel(cs);peer.setHandshakeReceived();info.addPeer(peer);
        peer = new Peer(new InetSocketAddress(ia, 9004), null, null);
        peer.setChannel(cs);peer.setHandshakeReceived();info.addPeer(peer);
        peer = new Peer(new InetSocketAddress(ia, 9005), null, null);
        peer.setChannel(cs);peer.setHandshakeReceived();info.addPeer(peer);
        List<SimpleMessage> m = info.decideChoking();
        assert m.size() == 5;
        for (SimpleMessage sm: m) {
            assert sm.getMessageType() == SimpleMessage.TYPE_UNCHOKE;
        }
    }
}
