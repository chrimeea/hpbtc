package util;

import hpbtc.protocol.torrent.Peer;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class TrackerUtil {

    public static List<Peer> doCompactPeer(Map<String, Object> response,
            byte[] infoHash) throws UnsupportedEncodingException,
            UnknownHostException {
        List<Peer> peers = new LinkedList<Peer>();
        byte[] prs = ((String) response.get("peers")).getBytes("ISO-8859-1");
        int k = 0;
        while (k < prs.length) {
            InetAddress peerIp = InetAddress.getByAddress(
                    Arrays.copyOfRange(prs, k, k + 4));
            k += 6;
            int peerPort = getUnsigned(prs[k - 2]) * 256 + getUnsigned(
                    prs[k - 1]);
            peers.add(new Peer(new InetSocketAddress(peerIp, peerPort), infoHash,
                    null));
        }
        return peers;
    }

    public static List<Peer> doLoosePeer(Map<String, Object> response,
            byte[] infoHash) throws
            UnsupportedEncodingException {
        List<Peer> peers = new LinkedList<Peer>();
        List<Map<String, Object>> prs =
                (List<Map<String, Object>>) response.get("peers");
        for (Map<String, Object> d : prs) {
            peers.add(new Peer(new InetSocketAddress((String) d.get("ip"),
                    ((Integer) d.get("port")).intValue()), infoHash,
                    ((String) d.get("peer id")).getBytes("ISO-8859-1")));
        }
        return peers;
    }

    private static int getUnsigned(byte b) {
        return b < 0 ? 127 - b : b;
    }
}
