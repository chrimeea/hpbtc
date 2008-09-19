package hpbtc.protocol.network;

import hpbtc.processor.MessageReader;
import hpbtc.processor.MessageWriter;
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
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
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
        Register r = new Register(new Timer());
        r.openWriteSelector();
        final NetworkReader c = new NetworkReader(new MessageReader() {

            public void readMessage(Peer peer) throws IOException,
                    NoSuchAlgorithmException {
                peer.setNextDataExpectation(11);
                assert peer.download();
                ByteBuffer bb = peer.getData();
                SocketChannel ch = (SocketChannel) peer.getChannel();
                Socket s = ch.socket();
                InetSocketAddress a = IOUtil.getAddress(ch);
                InetAddress remoteAddress = s.getLocalAddress();
                int remotePort = s.getPort();
                assert a.getAddress().equals(remoteAddress);
                assert a.getPort() == remotePort;
                bb.limit(11);
                assert bb.equals(ByteBuffer.wrap("test client".getBytes()));
            }

            public void disconnect(Peer arg0) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, r);
        int port = c.connect();
        SocketChannel ch = SocketChannel.open(new InetSocketAddress(InetAddress.
                getLocalHost(), port));
        ch.write(ByteBuffer.wrap("test client".getBytes("ISO-8859-1")));
        c.disconnect();
    }

    @Test
    public void testNetworkConnect() throws IOException {
        Register r = new Register(new Timer());
        r.openReadSelector();
        ServerSocket ch = new ServerSocket(0);
        NetworkWriter c = new NetworkWriter(new MessageWriter() {

            public void cancelPieceMessage(Peer arg0) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            
            public void postMessage(SimpleMessage arg0) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void writeNext(Peer p) throws IOException {
                p.upload(ByteBuffer.wrap("bit torrent".getBytes("ISO-8859-1")));
            }

            public void cancelPieceMessage(int arg0, int arg1, int arg2,
                    Peer arg3) {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void disconnect(Peer arg0) throws IOException {
                throw new UnsupportedOperationException("Not supported yet.");
            }

            public void connect(Peer arg0) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
        }, r);
        c.connect();
        InetSocketAddress a = new InetSocketAddress(InetAddress.getLocalHost(),
                ch.getLocalPort());
        Peer peer = new Peer(a, null);
        r.registerWrite(peer);
        Socket s = ch.accept();
        byte[] b = new byte[11];
        int i = s.getInputStream().read(b);
        c.disconnect();
        ch.close();
        assert i == 11;
        assert "bit torrent".equals(new String(b, 0, i, "ISO-8859-1"));
    }
}
