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
    public void testNetworkIncomingConnection() throws IOException, UnsupportedEncodingException {
        Network c = new Network();
        c.connect();
        SocketChannel ch = SocketChannel.open(new InetSocketAddress(InetAddress.getLocalHost(), c.getPort()));
        ch.write(ByteBuffer.wrap("test client".getBytes("US-ASCII")));
        synchronized (c) {
            do {
                try {
                    c.wait();
                } catch (InterruptedException e) {
                }
            } while (!c.hasUnreadMessages());
        }
        RawMessage m = c.takeMessage();
        Socket s = ch.socket();
        InetSocketAddress a = m.getPeer();
        InetAddress remoteAddress = s.getLocalAddress();
        int remotePort = s.getLocalPort();
        assert a.getAddress().equals(remoteAddress);
        assert a.getPort() == remotePort;
        assert new String(m.getMessage(), "US-ASCII").equals("test client");
        ch.close();
        synchronized (c) {
            do {
                try {
                    c.wait();
                } catch (InterruptedException e) {
                }
            } while (!c.hasUnreadMessages());
        }
        m = c.takeMessage();
        a = m.getPeer();
        assert m.isDisconnect();
        assert a.getAddress().equals(remoteAddress);
        assert a.getPort() == remotePort;
        c.disconnect();
    }

    @Test
    public void testNetworkConnect() throws IOException {
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
