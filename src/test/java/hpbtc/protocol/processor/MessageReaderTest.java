/*
 * Created on 09.07.2009
 */
package hpbtc.protocol.processor;

import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import hpbtc.util.ChannelStub;
import hpbtc.util.TorrentUtil;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.NoSuchAlgorithmException;
import java.util.Timer;
import org.junit.Test;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageReaderTest {

    @Test(expected=IOException.class)
    public void testReadMessageWrongLength()
            throws IOException, NoSuchAlgorithmException, InvalidPeerException {
        final Timer timer = new Timer();
        final MessageWriter writer = new MessageWriter(null, timer, null, null);
        final MessageReader reader = new MessageReader(null, writer, null, null);
        final Peer peer = new Peer(new InetSocketAddress(InetAddress.getLocalHost(),
                1000));
        final ByteArrayInputStream b =
                new ByteArrayInputStream("d8:announce27:http://www.test.ro/announce7:comment12:test comment10:created by13:uTorrent/177013:creation datei1209116668e8:encoding5:UTF-84:infod6:lengthi85e4:name11:manifest.mf12:piece lengthi65536e6:pieces20:12345678901234567890ee".
                getBytes("US-ASCII"));
        final Torrent info = new Torrent(b, ".", null, 0);
        b.close();
        peer.setTorrent(info);
        peer.setId(TorrentUtil.generateId());
        peer.setHandshakeReceived();
        peer.setExpectBody(false);
        final ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(Integer.MAX_VALUE);
        bb.rewind();
        final ChannelStub ch = new ChannelStub(bb, false);
        peer.setChannel(ch);
        reader.readMessage(peer);
    }
}
