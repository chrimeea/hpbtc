package hpbtc.processor;

import hpbtc.protocol.message.LengthPrefixMessage;
import hpbtc.protocol.torrent.Peer;
import hpbtc.protocol.torrent.Torrent;
import java.io.IOException;

/**
 *
 * @author Cristian Mocanu
 */
public interface MessageWriter {
    
    void stopTorrent(Torrent torrent) throws IOException;
    
    void writeNext(Peer peer) throws IOException;
    
    void disconnect(Peer peer) throws IOException;
    
    void postMessage(LengthPrefixMessage message) throws IOException;
    
    void download(Torrent torrent);
    
    void connect(Peer peer) throws IOException;
    
    void keepAliveRead(Peer peer);
}
