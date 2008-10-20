/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageReader;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import hpbtc.util.IOUtil;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageReaderStub extends MessageReader {
    
    public MessageReaderStub(final Register register) {
        super(register, null, null, null, null);
    }

    @Override
    public void setReadSelector(final Selector selector) {
        this.selector = selector;
    }

    @Override
    public void connect(final Peer peer) throws IOException {
        register.registerRead(peer, selector);
    }
    
    @Override
    public void readMessage(final Peer peer) throws IOException,
            NoSuchAlgorithmException {
        peer.setNextDataExpectation(11);
        assert peer.download();
        final ByteBuffer bb = peer.getData();
        final SocketChannel ch = peer.getChannel();
        final Socket s = ch.socket();
        final InetSocketAddress a = IOUtil.getAddress(ch);
        final InetAddress remoteAddress = s.getLocalAddress();
        int remotePort = s.getPort();
        assert a.getAddress().equals(remoteAddress);
        assert a.getPort() == remotePort;
        bb.limit(11);
        assert bb.equals(ByteBuffer.wrap("test client".getBytes()));
    }
}
