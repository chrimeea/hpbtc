package hpbtc.protocol.torrent;

import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.PieceMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.BitSet;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu
 */
public class PeerTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testRequests() throws IOException,
            NoSuchAlgorithmException {
        final Peer p = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000),
                null);
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi10240e4:name11:manifest.mf12:piece lengthi256e6:pieces20:12345678901234567890ee".
                getBytes(byteEncoding));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        p.setTorrent(info);
        int cs = info.getChunkSize();
        p.addRequest(10, cs);
        BitSet bs = p.getRequests(10);
        assert bs.cardinality() == 1;
        assert bs.get(1);
        assert p.countTotalRequests() == 1;
        p.removeRequest(10, 0);
        bs = p.getRequests(10);
        assert bs.cardinality() == 1;
        assert bs.get(1);
        assert p.countTotalRequests() == 1;
        bs = p.getRequests(10);
        p.removeRequest(10, cs);
        assert bs.cardinality() == 0;
        assert p.countTotalRequests() == 0;
    }

    @Test
    public void testEquals() {
        final Peer p1 = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000), null);
        final Peer p2 = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000), null);
        assert p1.equals(p2);
        assert p2.equals(p1);
        final Peer p3 = new Peer(InetSocketAddress.createUnresolved("localhost",
                7000), null);
        assert !p1.equals(p3);
        assert !p3.equals(p1);
    }

    @Test
    public void testMessagesToSend() {
        final Peer p = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000), null);
        assert p.isMessagesToSendEmpty();
        final LengthPrefixMessage m = new LengthPrefixMessage(0, p);
        p.addMessageToSend(m);
        assert !p.isMessagesToSendEmpty();
        p.cancelPieceMessage();
        assert !p.isMessagesToSendEmpty();
        assert m.equals(p.getMessageToSend());
        assert p.isMessagesToSendEmpty();
        final PieceMessage pm = new PieceMessage(1, 2, 3, p);
        p.addMessageToSend(pm);
        assert !p.isMessagesToSendEmpty();
        p.cancelPieceMessage(0, 2, 3);
        assert !p.isMessagesToSendEmpty();
        p.cancelPieceMessage(1, 2, 3);
        assert p.isMessagesToSendEmpty();
    }

    @Test
    public void testUploadDownload() throws IOException {
        final InetSocketAddress a = new InetSocketAddress(
                InetAddress.getByName("127.0.0.1"), 6001);
        final Peer p = new Peer(a, null);
        final ServerSocket ss = new ServerSocket(6001);
        final byte[] b = "test".getBytes("US-ASCII");
        final byte[] x = "response".getBytes("US-ASCII");
        new Thread() {

            @Override
            public void run() {
                try {
                    final Socket socket = ss.accept();
                    byte[] r = new byte[4];
                    socket.getInputStream().read(r);
                    assert Arrays.equals(b, r);
                    socket.getOutputStream().write(x);
                    socket.close();
                } catch (IOException ex) {
                    assert false;
                }
            }
        }.start();
        final SocketChannel s = SocketChannel.open(a);
        p.setChannel(s);
        assert p.countUploaded() == 0;
        int i = p.upload(ByteBuffer.wrap(b));
        assert i == 4;
        assert p.countUploaded() == 4;
        p.setNextDataExpectation(8);
        assert p.countDownloaded() == 0;
        assert p.download();
        assert p.countDownloaded() == 8;
        assert Arrays.equals(p.getData().array(), x);
        s.close();
        ss.close();
    }
}