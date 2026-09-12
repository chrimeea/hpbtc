
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
            final DHTMessage dhtreply = dhtmessage.createReply(table.getNodeID());
            if (dhtmessage.isPingQuery()) {
                //TODO: update routing info
            } else if (dhtmessage.isFindNodeQuery()) {
                final Bucket b = table.findBucket(dhtmessage.getTargetID());
                if (b != null) {
                    dhtreply.setNodes(b.getNode(dhtmessage.getTargetID()).
                            getCompactNodeInfo());
                } else {
                    dhtreply.setNodes(b.getCompactNodes());
                }
            //TODO: update routing info
            } else if (dhtmessage.isGetPeersQuery()) {
                //TODO: update routing info
                byte[] token = DHTUtil.generateToken();
                dhtreply.setToken(token);
                node.setToken(token);
                baw = new ByteArrayWrapper(dhtmessage.getInfohash());
                if (torrents.containsKey(baw)) {
                    dhtreply.setValues(torrents.get(baw));
                } else {
                    Bucket b = table.findBucket(dhtmessage.getInfohash());
                    dhtreply.setNodes(b.getCompactNodes());
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
            writer.postMessage(dhtreply.getMessage(), node.getAddress());
        } else if (dhtmessage.isResponse()) {
            //TODO: update routing info
        } else if (dhtmessage.isError()) {
            //TODO: update routing info
        } else {
            //TODO: return error
        }
    }
}
