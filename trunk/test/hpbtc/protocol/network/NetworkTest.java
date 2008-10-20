package hpbtc.protocol.network;

import hpbtc.processor.MessageReader;
import hpbtc.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
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

    @Test
    public void testNetworkConnect() throws IOException,
            NoSuchAlgorithmException {
        final ServerSocket ch = new ServerSocket(0);
        final Register r = new Register();
        final MessageWriter mws = new MessageWriterStub(r);
        final NetworkWriter c = new NetworkWriter(mws, r);
        c.connect();
        final InetSocketAddress a = new InetSocketAddress(InetAddress.
                getLocalHost(), ch.getLocalPort());
        final Peer peer = new Peer(a);
        final ByteArrayInputStream bai =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes(encoding));
        final Torrent info = new Torrent(bai, ".", null, 0);
        bai.close();
        peer.setTorrent(info);
        mws.connect(peer);
        final Socket s = ch.accept();
        byte[] b = new byte[11];
        int i = s.getInputStream().read(b);
        c.disconnect();
        ch.close();
        assert i == 11;
        assert "bit torrent".equals(new String(b, 0, i, encoding));
    }
}
