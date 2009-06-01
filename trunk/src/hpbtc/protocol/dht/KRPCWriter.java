package hpbtc.protocol.dht;

import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.network.Register;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Chris
 */
public class KRPCWriter {

    private List<DatagramPacket> messagesToSend = Collections.synchronizedList(
            new LinkedList<DatagramPacket>());
    private Register register;
    private DatagramSocket socket;
    private Selector selector;

    public KRPCWriter(final Register register) {
        this.register = register;
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
