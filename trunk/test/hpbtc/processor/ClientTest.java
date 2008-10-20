/*
 * Created on 26.09.2008
 */
package hpbtc.processor;

import com.sun.net.httpserver.HttpServer;
import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.message.HaveMessage;
import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.message.SimpleMessage;
import hpbtc.protocol.torrent.HttpHandlerStub;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class ClientTest {

    private String byteEncoding = "US-ASCII";

    @Test
    public void testStartStop() throws UnsupportedEncodingException,
            IOException {
        final Client c = new Client();
        final int port = c.startProtocol();
        final Socket s = new Socket(InetAddress.getLocalHost(), port);
        final HandshakeMessage hm = new HandshakeMessage(null,
                TorrentUtil.getSupportedProtocol(), null, new byte[20]);
        s.getOutputStream().write(hm.send().array());
        final InputStream is = s.getInputStream();
        assert is.read() == -1;
        s.close();
        c.stopProtocol();
        try {
            new Socket(InetAddress.getLocalHost(), port);
        } catch (IOException e) {
            assert false;
        }
    }

    @Test
    public void download() throws UnsupportedEncodingException, IOException,
            NoSuchAlgorithmException, URISyntaxException {
        final byte[] pid = TorrentUtil.generateId();
        final Client c = new Client(pid);
        final int port = c.startProtocol();
        final byte[] peers = new byte[12];
        peers[0] = 127; peers[1] = 0; peers[2] = 0;
        peers[3] = 1; peers[4] = (byte) 13; peers[5] = (byte) 4;
        peers[6] = 127; peers[7] = 0; peers[8] = 0;
        peers[9] = 1; peers[10] = (byte) 13; peers[11] = (byte) 5;
        final Map<byte[], Object> response = new HashMap<byte[], Object>();
        response.put("peers".getBytes(byteEncoding), peers);
        response.put("interval".getBytes(byteEncoding), 1800);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BencodingWriter bw = new BencodingWriter(os);
        bw.write(response); os.close();
        final String prefix = "/test";
        final HttpHandlerStub hh = new HttpHandlerStub(1, byteEncoding);
        final String dfile = "manifest.mf";
        final String trackEncoding = "ISO-8859-1"; final int len = 65536;
        final String ihash = "d6:lengthi" + len + "e4:name11:" + dfile +
                "12:piece lengthi32768e6:pieces20:12345678901234567890e";
        final String infoHash = new String(MessageDigest.getInstance("SHA1").
                digest(ihash.getBytes(byteEncoding)), trackEncoding);
        hh.addExpectation(prefix + "?info_hash=" + URLEncoder.encode(infoHash,
                trackEncoding) + "&peer_id=" +
                URLEncoder.encode(new String(pid, trackEncoding),
                trackEncoding) + "&port=" + port +
                "&uploaded=0&downloaded=0&left=" + len +
                "&compact=1&event=started", new String(os.toByteArray(),
                byteEncoding));
        final HttpServer server = HttpServer.create(new InetSocketAddress(6000),
                0);
        server.createContext(prefix, hh); server.start();
        c.download(new ByteArrayInputStream(("d8:announce26:http://localhost:6000/test4:info" +
                ihash + "e").getBytes(byteEncoding)), ".");
        final ServerSocket ch1 = new ServerSocket(3332);
        final Socket s1 = ch1.accept();
        final ServerSocket ch2 = new ServerSocket(3333);
        final Socket s2 = ch2.accept();
        testHandshake(s1.getInputStream(), s1.getOutputStream(),
                infoHash, trackEncoding, pid);
        testHandshake(s2.getInputStream(), s2.getOutputStream(),
                infoHash, trackEncoding, pid);
        testPeerMessages(s1.getInputStream(), s1.getOutputStream());
        testPeerMessages(s2.getInputStream(), s2.getOutputStream());
        s1.close(); s2.close();
        c.stopProtocol(); server.stop(0);
        ch1.close(); ch2.close();
        new File(dfile).delete();
    }

    private void testHandshake(final InputStream is, final OutputStream outs,
            final String infoHash, final String trackEncoding, final byte[] pid)
            throws IOException {
        byte[] b = new byte[48]; is.read(b);
        ByteBuffer bb = ByteBuffer.wrap(b);
        final HandshakeMessage m = new HandshakeMessage(bb, null);
        assert Arrays.equals(m.getProtocol(), TorrentUtil.getSupportedProtocol());
        assert Arrays.equals(m.getInfoHash(), infoHash.getBytes(trackEncoding));
        b = new byte[20]; is.read(b);
        assert Arrays.equals(b, pid);
        m.setPeerId(TorrentUtil.generateId());
        outs.write(m.send().array());
        LengthPrefixMessage hm = new HaveMessage(0, null);
        outs.write(hm.send().array());
        b = new byte[5]; is.read(b);
        assert b[4] == SimpleMessage.TYPE_INTERESTED;        
    }
    
    private void testPeerMessages(final InputStream is, final OutputStream outs)
            throws IOException {
        byte[] b = new byte[5];
        is.read(b);
        assert b[4] == SimpleMessage.TYPE_UNCHOKE;
        LengthPrefixMessage hm = new SimpleMessage(SimpleMessage.TYPE_UNCHOKE,
                null);
        outs.write(hm.send().array());
        b = new byte[17]; is.read(b);
        assert b[4] == SimpleMessage.TYPE_REQUEST;
        assert b[8] == 0; assert b[11] == 0; assert b[12] == 0;
        assert b[15] == 64; assert b[16] == 0;
        b = new byte[17]; is.read(b);
        assert b[4] == SimpleMessage.TYPE_REQUEST;
        assert b[8] == 0;
        assert b[11] == 64; assert b[12] == 0;
        assert b[15] == 64; assert b[16] == 0;        
    }
}
