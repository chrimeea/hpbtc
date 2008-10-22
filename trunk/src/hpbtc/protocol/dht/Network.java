/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.bencoding.BencodingReader;
import hpbtc.protocol.network.NetworkLoop;
import hpbtc.protocol.network.Register;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class Network extends NetworkLoop {

    private DatagramSocket socket;
    private DatagramPacket packet = new DatagramPacket(new byte[16384], 16384);
    private RoutingTable table = new RoutingTable();
    private List<DatagramPacket> messagesToSend = Collections.synchronizedList(
            new LinkedList<DatagramPacket>());
    private String byteEncoding = "US-ASCII";
    private Map<SocketAddress, Node> nodes = new HashMap<SocketAddress, Node>();

    public Network(final Register register) {
        super(register);
    }

    public void connect(final int port) throws IOException {
        DatagramChannel channel = DatagramChannel.open();
        socket = channel.socket();
        socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), port));
        super.connect();
    }

    @Override
    public int connect() throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        socket = channel.socket();
        socket.bind(null);
        super.connect();
        register.registerNow(channel, selector, SelectionKey.OP_READ, null);
        return socket.getPort();
    }

    public void postMessage(byte[] message, SocketAddress address)
            throws SocketException, IOException {
        messagesToSend.add(new DatagramPacket(message, message.length, address));
        register.registerNow(socket.getChannel(), selector,
                SelectionKey.OP_WRITE, null);
    }

    @Override
    protected void processKey(SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        if (key.isReadable()) {
            socket.receive(packet);
            BencodingReader reader = new BencodingReader(
                    new ByteArrayInputStream(packet.getData(),
                    packet.getOffset(), packet.getLength()));
            processMessage(reader.readNextDictionary(),
                    nodes.get(packet.getAddress()));
        } else if (key.isWritable()) {
            if (!messagesToSend.isEmpty()) {
                socket.send(messagesToSend.get(0));
            } else {
                register.registerNow(socket.getChannel(), selector,
                        SelectionKey.OP_READ, null);
            }
        }
    }
    
    private void processMessage(final Map<byte[], Object> message,
            final Node node) throws UnsupportedEncodingException, IOException {
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
