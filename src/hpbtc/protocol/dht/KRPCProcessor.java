/*
 * Created on 22.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.network.Register;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class KRPCProcessor {

    private String byteEncoding = "US-ASCII";
    private RoutingTable table = new RoutingTable();
    private Map<InetAddress, DHTNode> nodes =
            new HashMap<InetAddress, DHTNode>();
    private List<DatagramPacket> messagesToSend = Collections.synchronizedList(
            new LinkedList<DatagramPacket>());
    private Register register;
    private DatagramSocket socket;
    private Selector selector;

    public KRPCProcessor(final Register register) {
        this.register = register;
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
                final Map<byte[], Object> resp = new HashMap<byte[], Object>();
                resp.put("t".getBytes(byteEncoding), mid);
                if (Arrays.equals(mquery, "ping".getBytes(byteEncoding))) {
                    byte[] remoteId = (byte[]) margs.get(
                            "id".getBytes(byteEncoding));
                    resp.put("y".getBytes(byteEncoding), "r".getBytes(
                            byteEncoding));
                    final Map<byte[], Object> respargs =
                            new HashMap<byte[], Object>();
                    respargs.put("id".getBytes(byteEncoding), table.getNodeID());
                    resp.put("r".getBytes(byteEncoding), respargs);
                    postMessage(resp, address);
                } else if (Arrays.equals(mquery,
                        "find_node".getBytes(byteEncoding))) {
                    //FIND NODE
                } else if (Arrays.equals(mquery,
                        "get_peers".getBytes(byteEncoding))) {
                    //GET PEERS
                } else if (Arrays.equals(mquery,
                        "announce_peer".getBytes(byteEncoding))) {
                    //ANNOUNCE PEER
                }
                break;
            case 'r':
                final Map<byte[], Object> mret = (Map<byte[], Object>) message.
                        get("r".getBytes(byteEncoding));
                break;
            case 'e':
                final List<Object> merr = (List<Object>) message.get("e".
                        getBytes(byteEncoding));
                break;
            default:
                nodes.remove(node);
        }
    }

    public void postMessage(final Map<byte[], Object> dict,
            final SocketAddress address) throws SocketException, IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final BencodingWriter writer = new BencodingWriter(bos);
        writer.write(dict);
        final byte[] message = bos.toByteArray();
        messagesToSend.add(new DatagramPacket(message, message.length, address));
        register.registerNow(socket.getChannel(), selector,
                SelectionKey.OP_WRITE, null);
    }

    public void writeNext() throws IOException {
        if (!messagesToSend.isEmpty()) {
            socket.send(messagesToSend.get(0));
        } else {
            register.registerNow(socket.getChannel(), selector,
                    SelectionKey.OP_READ, null);
        }
    }
    
    public void setSelector(Selector selector) {
        this.selector = selector;
    }
    
    public void setSocket(DatagramSocket socket) {
        this.socket = socket;
    }
}
