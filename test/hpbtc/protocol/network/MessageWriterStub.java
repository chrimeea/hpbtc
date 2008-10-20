/*
 * Created on 18.10.2008
 */
package hpbtc.protocol.network;

import hpbtc.protocol.processor.MessageWriter;
import hpbtc.protocol.torrent.Peer;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class MessageWriterStub extends MessageWriter {

    public MessageWriterStub(final Register r) {
        super(r, null, null, null);
    }

    @Override
    public void connect(final Peer peer) throws IOException {
        register.registerWrite(peer, selector);
    }

    @Override
    public void writeNext(final Peer p) throws IOException {
        p.upload(ByteBuffer.wrap("bit torrent".getBytes("ISO-8859-1")));
    }
}
