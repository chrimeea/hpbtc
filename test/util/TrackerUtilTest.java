package util;

import hpbtc.protocol.torrent.Peer;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerUtilTest {

    @Test
    public void testDoCompactPeer() throws UnknownHostException,
            UnsupportedEncodingException {
        byte[] peers = new byte[12];
        ByteBuffer bb = ByteBuffer.wrap(peers);
        bb.put((byte) 127);
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.put((byte) 1);
        bb.put((byte) 5);
        bb.put((byte) 1);
        bb.put((byte) 127);
        bb.put((byte) 0);
        bb.put((byte) 0);
        bb.put((byte) 1);
        bb.put((byte) 5);
        bb.put((byte) 2);
        Set<Peer> p = TrackerUtil.doCompactPeer(peers);
        assert p.size() == 2;
        for (Peer pr: p) {
            InetSocketAddress a = pr.getAddress();
            assert a.getHostName().equals("127.0.0.1");
            assert a.getPort() == 1281 || a.getPort() == 1282;
            assert pr.getId() == null;
        }
    }
    
    @Test
    public void testDoLoosePeer() throws UnsupportedEncodingException {
        List<Map<byte[], Object>> peers = new ArrayList<Map<byte[], Object>>(2);
        Map<byte[], Object> p = new TreeMap<byte[], Object>();
        byte[] id1 = "11111111111111111111".getBytes("US-ASCII");
        p.put("peer id".getBytes("US-ASCII"), id1);
        p.put("ip".getBytes("US-ASCII"), "localhost");
        p.put("port".getBytes("US-ASCII"), 6515);
        peers.add(p);
        p = new TreeMap<byte[], Object>();
        byte[] id2 = "22222222222222222222".getBytes("US-ASCII");
        p.put("peer id".getBytes("US-ASCII"),
                "22222222222222222222".getBytes("US-ASCII"));
        p.put("ip".getBytes("US-ASCII"), "127.0.0.1");
        p.put("port".getBytes("US-ASCII"), 6690);
        peers.add(p);
        Set<Peer> prs = TrackerUtil.doLoosePeer(peers, "US-ASCII");
        assert p.size() == 2;
        for (Peer pr: prs) {
            InetSocketAddress a = pr.getAddress();
            if (Arrays.equals(pr.getId(), id1)) {
                assert a.getHostName().equals("localhost");
                assert a.getPort() == 6515;
            } else if (Arrays.equals(pr.getId(), id2)) {
                assert a.getHostName().equals("127.0.0.1");
                assert a.getPort() == 6690;
            } else {
                assert false;
            }
        }
    }
}
