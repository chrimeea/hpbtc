package hpbtc.protocol.torrent;

import hpbtc.protocol.message.LengthPrefixMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
import java.util.Iterator;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testRequests() throws IOException,
            NoSuchAlgorithmException {
        Peer p = new Peer(InetSocketAddress.createUnresolved("localhost", 6000),
                null);
        ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi10240e4:name11:manifest.mf12:piece lengthi256e6:pieces20:12345678901234567890ee".
                getBytes(byteEncoding));
        Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        p.setTorrent(info);
        int cs = info.getChunkSize();
        p.addRequest(10, cs);
        BitSet bs = p.getRequests(10);
        assert bs.cardinality() == 1;
        assert bs.get(1);
        assert p.countTotalRequests() == 1;
        p.removeRequest(10, 0, cs);
        bs = p.getRequests(10);
        assert bs.cardinality() == 1;
        assert bs.get(1);
        assert p.countTotalRequests() == 1;
        bs = p.getRequests(10);
        p.removeRequest(10, cs, cs);
        assert bs.cardinality() == 0;
        assert p.countTotalRequests() == 0;
    }

    @Test
    public void testEquals() {
        Peer p1 = new Peer(InetSocketAddress.createUnresolved("localhost", 6000),
                null);
        Peer p2 = new Peer(InetSocketAddress.createUnresolved("localhost", 6000),
                null);
        assert p1.equals(p2);
        assert p2.equals(p1);
        Peer p3 = new Peer(InetSocketAddress.createUnresolved("localhost", 7000),
                null);
        assert !p1.equals(p3);
        assert !p3.equals(p1);
    }

    @Test
    public void testMessagesToSend() {
        Peer p = new Peer(InetSocketAddress.createUnresolved("localhost", 6000),
                null);
        assert p.isMessagesToSendEmpty();
        LengthPrefixMessage m = new LengthPrefixMessage(0, p);
        p.addMessageToSend(m);
        assert !p.isMessagesToSendEmpty();
        Iterator<LengthPrefixMessage> i = p.listMessagesToSend().iterator();
        assert i.hasNext();
        LengthPrefixMessage l = i.next();
        assert m.equals(l);
        assert !i.hasNext();
    }
}
