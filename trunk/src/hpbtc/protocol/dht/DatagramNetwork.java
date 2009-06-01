/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.dht;

import hpbtc.bencoding.BencodingReader;
import hpbtc.protocol.network.NetworkLoop;
import hpbtc.protocol.network.Register;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class DatagramNetwork extends NetworkLoop {

    private DatagramSocket socket;
    private DatagramPacket packet = new DatagramPacket(new byte[16384], 16384);
    private KRPCReader processor;
    private KRPCWriter writer;

    public DatagramNetwork(final KRPCWriter writer, final Register register) {
        super(register);
        this.writer = writer;
        this.processor = new KRPCReader(register, writer);
    }

    public void connect(final int port) throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        socket = channel.socket();
        socket.bind(new InetSocketAddress(InetAddress.getLocalHost(), port));
        super.connect();
        processor.setSelector(selector);
        processor.setSocket(socket);
    }

    @Override
    public int connect() throws IOException {
        final DatagramChannel channel = DatagramChannel.open();
        socket = channel.socket();
        socket.bind(null);
        super.connect();
        processor.setSelector(selector);
        processor.setSocket(socket);
        register.registerNow(channel, selector, SelectionKey.OP_READ, null);
        return socket.getPort();
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
                    packet.getSocketAddress());
        } else if (key.isWritable()) {
            writer.writeNext();
        }
    }
}
