package hpbtc.protocol.message;

import java.nio.ByteBuffer;

public abstract class BlockMessage implements ProtocolMessage {

    protected int index;
    protected int begin;
    protected int length;
    
    public BlockMessage(ByteBuffer message) {
        index = message.getInt();
        begin = message.getInt();
        length = message.getInt();
    }
    
    public BlockMessage(int begin, int index, int length) {
        this.index = index;
        this.begin = begin;
        this.length = length;
    }
        
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (index + "#" + begin + "#" + length).hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof BlockMessage)) {
            return false;
        }
        BlockMessage rm = (BlockMessage) o;
        return index == rm.index &&
            begin == rm.begin &&
            length == rm.length;
    }

    public int getBegin() {
        return begin;
    }

    public int getIndex() {
        return index;
    }

    public int getLength() {
        return length;
    }
}
