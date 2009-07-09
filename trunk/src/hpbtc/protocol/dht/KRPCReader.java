
/*
 * Created on 22.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.protocol.network.Register;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.channels.Selector;
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
    private Map<InetAddress, DHTNode> nodes =
            new HashMap<InetAddress, DHTNode>();
    private Register register;
    private DatagramSocket socket;
    private KRPCWriter writer;

    public KRPCReader(final Register register, final KRPCWriter writer) {
        this.register = register;
        this.writer = writer;
    }

    public void processMessage(final Map<byte[], Object> message,
            final SocketAddress address) throws UnsupportedEncodingException,
            IOException {
        final DHTNode node = nodes.get(address);
        final byte[] mid = (byte[]) message.get("t".getBytes(byteEncoding));
        final char mtype =
                (char) ((byte[]) message.get("y".getBytes(byteEncoding)))[0];
        switch (mtype) {
            case 'q':
                final byte[] mquery =
                        (byte[]) message.get("q".getBytes(byteEncoding));
                final Map<byte[], Object> margs = (Map<byte[], Object>) message.
                        get("a".getBytes(byteEncoding));
                byte[] remoteId = (byte[]) margs.get("id".getBytes(byteEncoding));
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
                    Bucket b = table.findBucket(targetID);
                    if (b != null) {
                        respargs.put("nodes".getBytes(byteEncoding),
                                b.getNode(targetID).getCompactNodeInfo());
                    } else {
                        List<String> sb = new LinkedList<String>();
                        for(DHTNode n: b.getNodes()) {
                            sb.add(n.getCompactNodeInfo());
                        }
                        respargs.put("nodes".getBytes(byteEncoding), sb);
                    }
                    //TODO: update routing info
                } else if (Arrays.equals(mquery,
                        "get_peers".getBytes(byteEncoding))) {
                    byte[] infohash = (byte[]) margs.get(
                            "info_hash".getBytes(byteEncoding));
                    //TODO: update routing info
                } else if (Arrays.equals(mquery,
                        "announce_peer".getBytes(byteEncoding))) {
                    byte[] infohash = (byte[]) margs.get(
                            "info_hash".getBytes(byteEncoding));
                    int port = ((Long) margs.get(
                            "port".getBytes(byteEncoding))).intValue();
                    byte[] token = (byte[]) margs.get(
                            "token".getBytes(byteEncoding));
                    //TODO: update routing info
                } else {
                    throw new IOException();
                }
                resp.put("r".getBytes(byteEncoding), respargs);
                writer.postMessage(resp, address);
                break;
            case 'r':
                final Map<byte[], Object> mret = (Map<byte[], Object>) message.
                        get("r".getBytes(byteEncoding));
                //TODO: update routing info
                break;
            case 'e':
                final List<Object> merr = (List<Object>) message.get("e".
                        getBytes(byteEncoding));
                //TODO: update routing info
                break;
            default:
                nodes.remove(node);
        }
    }

    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }
}
