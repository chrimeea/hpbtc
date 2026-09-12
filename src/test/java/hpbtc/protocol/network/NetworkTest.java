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
public class NetworkTest {

    private String encoding = "US-ASCII";

    @Test
    public void testNetworkIncomingConnection() throws IOException,
            UnsupportedEncodingException {
        final Register r = new Register();
        final NetworkReader c = new NetworkReader(new MessageReaderStub(r), r);
        int port = c.connect();
        final SocketChannel ch = SocketChannel.open(new InetSocketAddress(
                InetAddress.getLocalHost(), port));
        ch.write(ByteBuffer.wrap("test client".getBytes(encoding)));
        c.disconnect();
    }
}
