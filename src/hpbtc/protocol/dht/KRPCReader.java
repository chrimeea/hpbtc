
/*
 * Created on 22.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.util.ByteArrayWrapper;
import hpbtc.util.DHTUtil;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class KRPCReader {

    private String byteEncoding = "US-ASCII";
    private RoutingTable table = new RoutingTable(new Timer());
    private Map<ByteArrayWrapper, DHTNode> nodes =
            new HashMap<ByteArrayWrapper, DHTNode>();
    private Map<ByteArrayWrapper, List<String>> torrents =
            new HashMap<ByteArrayWrapper, List<String>>();
    private KRPCWriter writer;

    public KRPCReader(final KRPCWriter writer) {
        this.writer = writer;
    }

    public void processMessage(final Map<byte[], Object> message,
            final SocketAddress address) throws UnsupportedEncodingException,
            IOException {
        final byte[] mid = (byte[]) message.get("t".getBytes(byteEncoding));
        final char mtype =
                (char) ((byte[]) message.get("y".getBytes(byteEncoding)))[0];
        byte[] remoteId;
        DHTNode node;
        ByteArrayWrapper baw;
        switch (mtype) {
            case 'q':
                final byte[] mquery =
                        (byte[]) message.get("q".getBytes(byteEncoding));
                final Map<byte[], Object> margs = (Map<byte[], Object>) message.get("a".getBytes(byteEncoding));
                remoteId = (byte[]) margs.get("id".getBytes(byteEncoding));
                node = nodes.get(new ByteArrayWrapper(remoteId));
                final Map<byte[], Object> resp = new HashMap<byte[], Object>();
                resp.put("t".getBytes(byteEncoding), mid);
                resp.put("y".getBytes(byteEncoding), "r".getBytes(
                        byteEncoding));
                final Map<byte[], Object> respargs =
                        new HashMap<byte[], Object>();
                respargs.put("id".getBytes(byteEncoding), table.getNodeID());
                if (Arrays.equals(mquery, "ping".getBytes(byteEncoding))) {
                    //TODO: update routing info
                } else if (Arrays.equals(mquery,
                        "find_node".getBytes(byteEncoding))) {
                    byte[] targetID = (byte[]) margs.get(
                            "target".getBytes(byteEncoding));
                    final Bucket b = table.findBucket(targetID);
                    if (b != null) {
                        respargs.put("nodes".getBytes(byteEncoding),
                                b.getNode(targetID).getCompactNodeInfo());
                    } else {
                        respargs.put("nodes".getBytes(byteEncoding),
                                b.getCompactNodes());
                    }
                //TODO: update routing info
                } else if (Arrays.equals(mquery,
                        "get_peers".getBytes(byteEncoding))) {
                    byte[] infohash = (byte[]) margs.get(
                            "info_hash".getBytes(byteEncoding));
                    //TODO: update routing info
                    byte[] token = DHTUtil.generateToken();
                    respargs.put("token".getBytes(byteEncoding), token);
                    node.setToken(token);
                    baw = new ByteArrayWrapper(infohash);
                    if (torrents.containsKey(baw)) {
                        respargs.put("values".getBytes(byteEncoding),
                                torrents.get(baw));
                    } else {
                        Bucket b = table.findBucket(infohash);
                        respargs.put("nodes".getBytes(byteEncoding),
                                b.getCompactNodes());
                    }
                } else if (Arrays.equals(mquery,
                        "announce_peer".getBytes(byteEncoding))) {
                    byte[] infohash = (byte[]) margs.get(
                            "info_hash".getBytes(byteEncoding));
                    int port = ((Long) margs.get(
                            "port".getBytes(byteEncoding))).intValue();
                    byte[] token = (byte[]) margs.get(
                            "token".getBytes(byteEncoding));
                    //TODO: update routing info
                    if (Arrays.equals(token, node.getToken()) &&
                            node.getTokenAge() < 600000L) {
                        baw = new ByteArrayWrapper(infohash);
                        List<String> l;
                        if (!torrents.containsKey(baw)) {
                            l = new LinkedList<String>();
                            torrents.put(baw, l);
                        } else {
                            l = torrents.get(baw);
                        }
                        l.add(node.getCompactNodeInfo(port));
                    } else {
                        //TODO: return error
                    }
                } else {
                    //TODO: return error
                }
                resp.put("r".getBytes(byteEncoding), respargs);
                writer.postMessage(resp, node.getAddress());
                break;
            case 'r':
                final Map<byte[], Object> mret = (Map<byte[], Object>) message.get("r".getBytes(byteEncoding));
                remoteId = (byte[]) mret.get("id".getBytes(byteEncoding));
                //TODO: update routing info
                break;
            case 'e':
                final List<Object> merr = (List<Object>) message.get("e".getBytes(byteEncoding));
                //TODO: update routing info
                break;
            default:
                //TODO: return error
        }
    }
}
