/*
 * Created on 21.10.2008
 */

package hpbtc.protocol.network;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.ByteChannel;

/**
 *
 * @author Cristian Mocanu <chrimeea@yahoo.com>
 */
public class NetworkComponent {

    protected ByteChannel channel;
    protected SocketAddress address;
    protected byte[] id;

    public NetworkComponent(final SocketAddress address) {
        this.address = address;
    }
    
    public void setId(final byte[] id) {
        this.id = id;
    }
    
    public byte[] getId() {
        return id;
    }
    
    @Override
    public int hashCode() {
        return this.address.hashCode();
    }
    
    @Override
    public boolean equals(final Object obj) {
        try {
            final NetworkComponent other = (NetworkComponent) obj;
            return address.equals(other.address);
        } catch (ClassCastException e) {
            return false;
        }
    }
    
    @Override
    public String toString() {
        return address.toString();
    }
    
    public SocketAddress getAddress() {
        return address;
    }
    
    public ByteChannel getChannel() {
        return channel;
    }

    public void setChannel(ByteChannel channel) {
        this.channel = channel;
    }
    
    public void disconnect() throws IOException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
    }
}
