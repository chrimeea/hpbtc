/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.network;

import hpbtc.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Selector;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageWriterStub extends MessageWriter {

    public MessageWriterStub() {
        super(new Register(), null, null, null);
    }

    @Override
    public void connect(Peer peer) throws IOException {
        register.registerWrite(peer, selector);
    }
    
    @Override
    public Selector openWriteSelector() throws IOException {
        selector = register.openSelector();
        return selector;
    }

    @Override
    public void writeNext(Peer p) throws IOException {
        p.upload(ByteBuffer.wrap("bit torrent".getBytes("ISO-8859-1")));
    }
}
