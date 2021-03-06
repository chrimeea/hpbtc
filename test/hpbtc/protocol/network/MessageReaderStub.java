/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageReader;
import hpbtc.protocol.torrent.InvalidPeerException;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import hpbtc.util.IOUtil;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageReaderStub extends MessageReader {
    
    public MessageReaderStub(Register r) {
        super(null, new MessageWriterStub(r), null, null);
    }
    
    @Override
    public boolean readMessage(final Peer peer) throws IOException,
            NoSuchAlgorithmException, InvalidPeerException {
        peer.setNextDataExpectation(11);
        assert downloadFromPeer(peer);
        final ByteBuffer bb = peer.getData();
        bb.flip();
        final SocketChannel ch = (SocketChannel) peer.getChannel();
        final Socket s = ch.socket();
        final InetSocketAddress a = IOUtil.getAddress(ch);
        final InetAddress remoteAddress = s.getLocalAddress();
        int remotePort = s.getPort();
        assert a.getAddress().equals(remoteAddress);
        assert a.getPort() == remotePort;
        bb.limit(11);
        assert bb.equals(ByteBuffer.wrap("test client".getBytes()));
        return false;
    }
}
