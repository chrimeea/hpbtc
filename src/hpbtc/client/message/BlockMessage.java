package hpbtc.client.message;

public abstract class BlockMessage extends ProtocolMessage {

    protected int index;
    protected int begin;
    protected int length;

    public BlockMessage() {
    }

    public void setBegin(int begin) {
        this.begin = begin;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public void setLength(int length) {
        this.length = length;
    }
    
    /**
     * @return
     */
    public int getIndex() {
        return index;
    }
    
    /**
     * @return
     */
    public int getBegin() {
        return begin;
    }
    
    /**
     * @return
     */
    public int getLength() {
        return length;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return (index + "#" + begin + "#" + length + "#" + peer.getId()).hashCode();
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if (o == null || !(o instanceof BlockMessage)) {
            return false;
        }
        BlockMessage rm = (BlockMessage) o;
        if (index == rm.index &&
            begin == rm.begin &&
            length == rm.length &&
            ((peer == null && rm.peer == null) || (peer.equals(rm.peer)))) {
                return true;
        }
        return false;
    }
}
