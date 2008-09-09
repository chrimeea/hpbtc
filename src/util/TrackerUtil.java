package util;

import hpbtc.protocol.torrent.Peer;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Chris
 */
public class TrackerUtil {

    public static Set<Peer> doCompactPeer(Map<String, Object> response) throws
            UnsupportedEncodingException, UnknownHostException {
        Set<Peer> peers = new HashSet<Peer>();
        byte[] prs = ((String) response.get("peers")).getBytes("ISO-8859-1");
        int k = 0;
        while (k < prs.length) {
            InetAddress peerIp = InetAddress.getByAddress(
                    Arrays.copyOfRange(prs, k, k + 4));
            k += 6;
            int peerPort = getUnsigned(prs[k - 2]) * 256 + getUnsigned(
                    prs[k - 1]);
            peers.add(new Peer(new InetSocketAddress(peerIp, peerPort), null));
        }
        return peers;
    }

    public static Set<Peer> doLoosePeer(Map<String, Object> response) throws
            UnsupportedEncodingException {
        Set<Peer> peers = new HashSet<Peer>();
        List<Map<String, Object>> prs =
                (List<Map<String, Object>>) response.get("peers");
        for (Map<String, Object> d : prs) {
            peers.add(new Peer(new InetSocketAddress((String) d.get("ip"),
                    ((Integer) d.get("port")).intValue()), ((String) d.get(
                    "peer id")).getBytes("ISO-8859-1")));
        }
        return peers;
    }

    private static int getUnsigned(byte b) {
        return b < 0 ? 127 - b : b;
    }
}
