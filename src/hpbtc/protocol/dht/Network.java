/*
 * Created on 18.10.2008
 */

package hpbtc.protocol.dht;

import hpbtc.protocol.network.NetworkLoop;
import hpbtc.protocol.network.Register;
import java.io.IOException;
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
public class Network extends NetworkLoop {

    private DatagramChannel channel;

    public Network(final Register register) {
        super(register);
    }

    public void connect(final int port) throws IOException {
        channel = DatagramChannel.open();
        channel.socket().bind(new InetSocketAddress(InetAddress.getLocalHost(),
                port));
        super.connect();
    }

    @Override
    public int connect() throws IOException {
        channel = DatagramChannel.open();
        final DatagramSocket s = channel.socket();
        s.bind(null);
        super.connect();
        return s.getPort();
    }

    @Override
    protected void disconnect(SelectionKey key) throws IOException {
        key.cancel();
    }

    @Override
    protected void processKey(SelectionKey key) throws IOException,
            NoSuchAlgorithmException {
        if (key.isReadable()) {
            //TODO: read
        } else if (key.isWritable()) {
            //TODO: write
        }
    }
}
