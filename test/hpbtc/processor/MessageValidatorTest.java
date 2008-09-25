/*
 * Created on 25.09.2008
 */

package hpbtc.processor;

import hpbtc.protocol.message.HandshakeMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageValidatorTest {

    private String encoding = "US-ASCII";
    
    @Test
    public void testValidateHandshakeMessage() throws
            UnsupportedEncodingException, IOException, NoSuchAlgorithmException {
        Peer peer = new Peer(InetSocketAddress.createUnresolved("localhost",
                6000), null);
        ByteArrayInputStream b = new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".getBytes(encoding));
        Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        List<Torrent> t = new ArrayList<Torrent>(1);
        byte[] p = "PROT".getBytes(encoding);
        HandshakeMessage hm = new HandshakeMessage(null, p, peer,
                info.getInfoHash());
        MessageValidator v = new MessageValidator(t, p);
        assert !v.validateHandshakeMessage(hm);
        t.add(info);
        assert v.validateHandshakeMessage(hm);
    }
    
    @Test
    public void testValidateBitfieldMessage() {
    }
    
    @Test
    public void testValidateCancelMessage() {
    }
    
    @Test
    public void testValidateHaveMessage() {
    }
    
    @Test
    public void testValidatePieceMessage() {
    }
    
    @Test
    public void testValidateRequestMessage() {
    }
}
