/*
 * Created on 26.09.2008
 */
package hpbtc.processor;

import com.sun.net.httpserver.HttpServer;
import hpbtc.bencoding.BencodingWriter;
import hpbtc.protocol.message.HandshakeMessage;
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
            assert false;
        } catch (IOException e) {
        }
    }

    @Test
    public void download() throws UnsupportedEncodingException, IOException,
            NoSuchAlgorithmException, URISyntaxException {
        final byte[] pid = TorrentUtil.generateId();
        final Client c = new Client(pid);
        final int port = c.startProtocol();
        final ServerSocket ch = new ServerSocket(3332);
        final byte[] peers = new byte[6];
        peers[0] = 127;
        peers[1] = 0;
        peers[2] = 0;
        peers[3] = 1;
        peers[4] = (byte) 13;
        peers[5] = (byte) 4;
        final Map<byte[], Object> response = new HashMap<byte[], Object>();
        response.put("peers".getBytes(byteEncoding), peers);
        response.put("interval".getBytes(byteEncoding), 60);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BencodingWriter bw = new BencodingWriter(os);
        bw.write(response);
        os.close();
        final String prefix = "/test";
        final HttpHandlerStub hh = new HttpHandlerStub(1, byteEncoding);
        final String dfile = "manifest.mf";
        final String trackEncoding = "ISO-8859-1";
        final String ihash = "d6:lengthi85e4:name11:" + dfile +
                "12:piece lengthi65536e6:pieces20:12345678901234567890e";
        final String infoHash = new String(MessageDigest.getInstance("SHA1").
                digest(ihash.getBytes(byteEncoding)), trackEncoding);
        hh.addExpectation(prefix + "?info_hash=" + URLEncoder.encode(infoHash,
                trackEncoding) + "&peer_id=" +
                URLEncoder.encode(new String(pid, trackEncoding),
                trackEncoding) + "&port=" + port +
                "&uploaded=0&downloaded=0&left=85&compact=1&event=started",
                new String(os.toByteArray(), byteEncoding));
        final HttpServer server = HttpServer.create(new InetSocketAddress(6000),
                0);
        server.createContext(prefix, hh);
        server.start();
        c.download(new ByteArrayInputStream(("d8:announce26:http://localhost:6000/test4:info" +
                ihash + "e").getBytes(byteEncoding)), ".");
        final Socket s = ch.accept();
        final InputStream is = s.getInputStream();
        byte[] b = new byte[48];
        is.read(b);
        final HandshakeMessage m = new HandshakeMessage(ByteBuffer.wrap(b), null);
        assert Arrays.equals(m.getProtocol(), TorrentUtil.getSupportedProtocol());
        assert Arrays.equals(m.getInfoHash(), infoHash.getBytes(trackEncoding));
        b = new byte[20];
        is.read(b);
        assert Arrays.equals(b, pid);
        final OutputStream outs = s.getOutputStream();
        m.setPeerId(TorrentUtil.generateId());
        outs.write(m.send().array());
        s.close();
        c.stopProtocol();
        server.stop(0);
        ch.close();
        new File(dfile).delete();
    }
}
