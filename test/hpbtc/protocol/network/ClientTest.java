package hpbtc.protocol.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class ClientTest {

    @Test
    public void testClientIncomingConnection() throws IOException, UnsupportedEncodingException {
        final Client c = new Client();
        c.connect();
        final SocketChannel ch = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), c.getPort()));
        ch.write(ByteBuffer.wrap("test client".getBytes("US-ASCII")));
        synchronized (c) {
            do {
                try {
                    c.wait();
                } catch (InterruptedException e) {}
            } while (!c.hasUnreadMessages());
        }
        ClientProtocolMessage m = c.takeMessage();
        assert m.getPeer().getAddress().equals(ch.socket().getLocalAddress());
        assert m.getPeer().getPort() == ch.socket().getLocalPort();
        assert new String(m.getMessage(), "US-ASCII").equals("test client");
        ch.close();
        c.disconnect();
    }
}
