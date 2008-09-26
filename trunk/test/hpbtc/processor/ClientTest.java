/*
 * Created on 26.09.2008
 */

package hpbtc.processor;

import hpbtc.protocol.message.HandshakeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import org.junit.Test;
import util.TorrentUtil;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class ClientTest {

    @Test
    public void testStartStop() throws UnsupportedEncodingException,
            IOException {
        Client c = new Client();
        Socket s = new Socket(InetAddress.getLocalHost(), c.startProtocol());
        HandshakeMessage hm = new HandshakeMessage(null,
                TorrentUtil.getSupportedProtocol(), null, new byte[20]);
        s.getOutputStream().write(hm.send().array());
        InputStream is = s.getInputStream();
        assert is.read() == -1;
        s.close();
        c.stopProtocol();
    }
}
