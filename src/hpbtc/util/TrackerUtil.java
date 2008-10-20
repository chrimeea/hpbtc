package hpbtc.util;

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
 * @author Cristian Mocanu
 */
public class TrackerUtil {

    public static Set<Peer> doCompactPeer(final byte[] prs)
            throws UnsupportedEncodingException, UnknownHostException {
        final Set<Peer> peers = new HashSet<Peer>();
        int k = 0;
        while (k < prs.length) {
            final InetAddress peerIp = InetAddress.getByAddress(
                    Arrays.copyOfRange(prs, k, k + 4));
            k += 6;
            final int peerPort = getUnsigned(prs[k - 2]) * 256 + getUnsigned(
                    prs[k - 1]);
            peers.add(new Peer(new InetSocketAddress(peerIp, peerPort), null));
        }
        return peers;
    }

    public static Set<Peer> doLoosePeer(final List<Map<byte[], Object>> prs,
            final String byteEncoding) throws
            UnsupportedEncodingException {
        final Set<Peer> peers = new HashSet<Peer>();
        for (Map<byte[], Object> d : prs) {
            peers.add(new Peer(new InetSocketAddress(new String((byte[]) d.get("ip".
                    getBytes(byteEncoding)), byteEncoding), ((Long) d.get("port".
                    getBytes(byteEncoding))).intValue()), ((byte[]) d.
                    get("peer id".getBytes(byteEncoding)))));
        }
        return peers;
    }

    private static int getUnsigned(final byte b) {
        return b < 0 ? 256 + b : b;
    }
}
