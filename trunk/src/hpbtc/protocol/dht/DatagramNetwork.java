/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.network.NetworkLoop;
import hpbtc.protocol.network.Register;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DatagramNetwork extends NetworkLoop {

    private DatagramSocket socket;
    private DatagramPacket packet = new DatagramPacket(new byte[16384], 16384);
    private List<DatagramPacket> messagesToSend = Collections.synchronizedList(
            new LinkedList<DatagramPacket>());
    private KRPCProcessor processor;

    public DatagramNetwork(final Register register, final KRPCProcessor processor) {
        super(register);
        this.processor = processor;
    }

    public void connect(final int port) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
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

    @Override
    protected void processKey(SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        if (key.isReadable()) {
            socket.receive(packet);
            final BencodingReader reader = new BencodingReader(
                    new ByteArrayInputStream(packet.getData(),
                    packet.getOffset(), packet.getLength()));
            processor.processMessage(reader.readNextDictionary(),
                    packet.getAddress());
        } else if (key.isWritable()) {
            if (!messagesToSend.isEmpty()) {
                socket.send(messagesToSend.get(0));
            } else {
                register.registerNow(socket.getChannel(), selector,
                        SelectionKey.OP_READ, null);
            }
        }
    }
}
