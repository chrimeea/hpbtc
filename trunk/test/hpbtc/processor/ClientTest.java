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
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Ignore;
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
        } catch (IOException e) {}
    }
    
    @Test
    @Ignore
    public void download() throws UnsupportedEncodingException, IOException,
            NoSuchAlgorithmException, URISyntaxException {
        final byte[] pid = TorrentUtil.generateId();
        final Client c = new Client(pid);
        final int port = c.startProtocol();
        final ServerSocket ch = new ServerSocket(0);
        final byte[] peers = new byte[6];
        peers[0] = 127; peers[1] = 0;
        peers[2] = 0; peers[3] = 1;
        final int peerPort = ch.getLocalPort();
        peers[4] = (byte) (peerPort / 256);
        peers[5] = (byte) (peerPort % 256);
        final Map<byte[], Object> response = new HashMap<byte[], Object>();
        response.put("peers".getBytes(byteEncoding), peers);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        final BencodingWriter bw = new BencodingWriter(os);
        bw.write(response);
        os.close();
        final String prefix = "/test";
        final HttpHandlerStub hh = new HttpHandlerStub(1, byteEncoding);
        hh.addExpectation(prefix + "?info_hash=TODO&peer_id=" +
                new String(pid, byteEncoding) + "&port=" + port +
                "&uploaded=0&downloaded=0&left=TODO&compact=1&event=started",
                new String(os.toByteArray(), byteEncoding));
        final HttpServer server = HttpServer.create(new InetSocketAddress(6000),
                0);
        server.createContext(prefix, hh);
        server.start();
        new Thread(new Runnable() {

            public void run() {
                try {
                    Socket s = ch.accept();
                    //TODO expect handshake from client
                } catch (IOException ex) {
                    assert false;
                }
            }
        }).start();
        c.download(new ByteArrayInputStream("TODO".getBytes(byteEncoding)), ".");
        server.stop(0);
        c.stopProtocol();
    }
}
