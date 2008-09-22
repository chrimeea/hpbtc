package hpbtc.protocol.torrent;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.BitSet;
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
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi10240e4:name11:manifest.mf12:piece lengthi256e6:pieces20:12345678901234567890ee".getBytes(byteEncoding));
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
}
