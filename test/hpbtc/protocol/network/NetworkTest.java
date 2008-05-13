package hpbtc.protocol.network;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class NetworkTest {

    @Test
    public void testClientIncomingConnection() throws IOException, UnsupportedEncodingException {
        Network c = new Network();
        c.connect();
        SocketChannel ch = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), c.getPort()));
        ch.write(ByteBuffer.wrap("test client".getBytes("US-ASCII")));
        synchronized (c) {
            do {
                try {
                    c.wait();
                } catch (InterruptedException e) {}
            } while (!c.hasUnreadMessages());
        }
        RawMessage m = c.takeMessage();
        assert m.getPeer().getAddress().equals(ch.socket().getLocalAddress());
        assert m.getPeer().getPort() == ch.socket().getLocalPort();
        assert new String(m.getMessage(), "US-ASCII").equals("test client");
        ch.close();
        c.disconnect();
    }
    
    @Test
    public void testClientConnect() throws IOException {
        ServerSocket ch = new ServerSocket(0);
        Network c = new Network();
        c.connect();
        InetSocketAddress a = new InetSocketAddress(InetAddress.getLocalHost(), ch.getLocalPort());
        c.postMessage(a, ByteBuffer.wrap("bit torrent".getBytes("US-ASCII")));
        Socket s = ch.accept();
        byte[] b = new byte[15];
        int i = s.getInputStream().read(b);
        c.disconnect();
        ch.close();
        assert i == 11;
        assert "bit torrent".equals(new String(b, 0, i, "US-ASCII"));
    }
}
