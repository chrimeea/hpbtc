
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
        final DHTMessage dhtmessage = new DHTMessage(message);
        DHTNode node;
        ByteArrayWrapper baw;
        if (dhtmessage.isQuery()) {
            node = nodes.get(new ByteArrayWrapper(dhtmessage.getRemoteID()));
            final Map<byte[], Object> resp = new HashMap<byte[], Object>();
            resp.put("t".getBytes(byteEncoding), dhtmessage.getTransactionID());
            resp.put("y".getBytes(byteEncoding), "r".getBytes(
                    byteEncoding));
            final Map<byte[], Object> respargs =
                    new HashMap<byte[], Object>();
            respargs.put("id".getBytes(byteEncoding), table.getNodeID());
            if (dhtmessage.isPingQuery()) {
                //TODO: update routing info
            } else if (dhtmessage.isFindNodeQuery()) {
                final Bucket b = table.findBucket(dhtmessage.getTargetID());
                if (b != null) {
                    respargs.put("nodes".getBytes(byteEncoding),
                            b.getNode(dhtmessage.getTargetID()).getCompactNodeInfo());
                } else {
                    respargs.put("nodes".getBytes(byteEncoding),
                            b.getCompactNodes());
                }
            //TODO: update routing info
            } else if (dhtmessage.isGetPeersQuery()) {
                //TODO: update routing info
                byte[] token = DHTUtil.generateToken();
                respargs.put("token".getBytes(byteEncoding), token);
                node.setToken(token);
                baw = new ByteArrayWrapper(dhtmessage.getInfohash());
                if (torrents.containsKey(baw)) {
                    respargs.put("values".getBytes(byteEncoding),
                            torrents.get(baw));
                } else {
                    Bucket b = table.findBucket(dhtmessage.getInfohash());
                    respargs.put("nodes".getBytes(byteEncoding),
                            b.getCompactNodes());
                }
            } else if (dhtmessage.isAnnouncePeerQuery()) {
                //TODO: update routing info
                if (Arrays.equals(dhtmessage.getToken(), node.getToken()) &&
                        node.getTokenAge() < 600000L) {
                    baw = new ByteArrayWrapper(dhtmessage.getInfohash());
                    List<String> l;
                    if (!torrents.containsKey(baw)) {
                        l = new LinkedList<String>();
                        torrents.put(baw, l);
                    } else {
                        l = torrents.get(baw);
                    }
                    l.add(node.getCompactNodeInfo(dhtmessage.getPort()));
                } else {
                    //TODO: return error
                    }
            } else {
                //TODO: return error
            }
            resp.put("r".getBytes(byteEncoding), respargs);
            writer.postMessage(resp, node.getAddress());
        } else if (dhtmessage.isResponse()) {
            //TODO: update routing info
        } else if (dhtmessage.isError()) {
            //TODO: update routing info
        } else {
            //TODO: return error
        }
    }
}
