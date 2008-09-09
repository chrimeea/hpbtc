package hpbtc.protocol.network;

import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import org.junit.Test;
import util.IOUtil;

/**
 *
 * @author Cristian Mocanu
 */
public class NetworkTest {

    @Test
    public void testNetworkIncomingConnection() throws IOException,
            UnsupportedEncodingException {
        PeerNetwork c = new PeerNetwork();
        c.connect();
        SocketChannel ch;
        synchronized (c) {
            ch = SocketChannel.open(new InetSocketAddress(InetAddress.
                    getLocalHost(), c.getPort()));
            ch.write(ByteBuffer.wrap("test client".getBytes("ISO-8859-1")));
            do {
                try {
                    c.wait();
                } catch (InterruptedException e) {
                }
            } while (!c.hasUnreadMessages());
        }
        RawMessage m = c.takeMessage();
        Socket s = ch.socket();
        InetSocketAddress a = IOUtil.getAddress(
                (SocketChannel) m.getPeer().getChannel());
        InetAddress remoteAddress = s.getLocalAddress();
        int remotePort = s.getLocalPort();
        assert a.getAddress().equals(remoteAddress);
        assert a.getPort() == remotePort;
        assert new String(m.getMessage(), "ISO-8859-1").equals("test client");
        synchronized (c) {
            ch.close();
            do {
                try {
                    c.wait();
                } catch (InterruptedException e) {
                }
            } while (!c.hasUnreadMessages());
        }
        m = c.takeMessage();
        a = m.getPeer().getAddress();
        assert m.isDisconnect();
        assert a.getAddress().equals(remoteAddress);
        assert a.getPort() == remotePort;
        c.disconnect();
    }

    @Test
    public void testNetworkConnect() throws IOException {
        ServerSocket ch = new ServerSocket(0);
        PeerNetwork c = new PeerNetwork();
        c.connect();
        final InetSocketAddress a = new InetSocketAddress(InetAddress.getLocalHost(),
                ch.getLocalPort());
        c.postMessage(new SimpleMessage() {

            @Override
            public Peer getDestination() {
                try {
                    return new Peer(a, null, "X".getBytes("ISO-8859-1"));
                } catch (UnsupportedEncodingException e) {
                    return null;
                }
            }
            
            @Override
            public ByteBuffer send() {
                try {
                    return ByteBuffer.wrap("bit torrent".getBytes("ISO-8859-1"));
                } catch (UnsupportedEncodingException e) {
                    assert false;
                }
                return null;
            }
        });
        Socket s = ch.accept();
        byte[] b = new byte[15];
        int i = s.getInputStream().read(b);
        c.disconnect();
        ch.close();
        assert i == 11;
        assert "bit torrent".equals(new String(b, 0, i, "ISO-8859-1"));
    }
}
