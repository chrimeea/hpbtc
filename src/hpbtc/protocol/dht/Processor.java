/*
 * Created on 22.10.2008
 */

package hpbtc.protocol.dht;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class Processor {
    
    private String byteEncoding = "US-ASCII";
    private RoutingTable table = new RoutingTable();
    private Map<InetAddress, Node> nodes = new HashMap<InetAddress, Node>();
    
    public void processMessage(final Map<byte[], Object> message,
            final InetAddress address) throws UnsupportedEncodingException,
            IOException {
        final Node node = nodes.get(address);
        final byte[] mid = (byte[]) message.get("t".getBytes(byteEncoding));
        final char mtype =
                (char)((byte[]) message.get("y".getBytes(byteEncoding)))[0];
        switch (mtype) {
            case 'q':
                final byte[] mquery =
                        (byte[]) message.get("q".getBytes(byteEncoding));
                final Map<byte[], Object> margs = (Map<byte[], Object>)
                        message.get("a".getBytes(byteEncoding));
                break;
            case 'r':
                final Map<byte[], Object> mret = (Map<byte[], Object>)
                        message.get("r".getBytes(byteEncoding));
                break;
            case 'e':
                final List<Object> merr = (List<Object>)
                        message.get("e".getBytes(byteEncoding));
                break;
            default:
                nodes.remove(node);
                node.disconnect();
        }
    }
}
