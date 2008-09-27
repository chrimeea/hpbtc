/*
 * Created on 26.09.2008
 */

package hpbtc.processor;

import hpbtc.protocol.message.HandshakeMessage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
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
    
//    @Test
//    public void download() throws UnsupportedEncodingException, IOException,
//            NoSuchAlgorithmException {
//        final Client c = new Client();
//        c.startProtocol();
//        c.download(new ByteArrayInputStream("TODO".getBytes(byteEncoding)), ".");
//        c.stopProtocol();
//    }
}
