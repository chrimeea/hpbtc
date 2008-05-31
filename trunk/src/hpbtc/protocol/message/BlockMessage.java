package hpbtc.protocol.message;

import java.nio.ByteBuffer;

public class BlockMessage extends SimpleMessage {

    private int index;
    private int begin;
    private int length;
    
    public BlockMessage(ByteBuffer message, byte disc) {
        super(13, disc);
        index = message.getInt();
        begin = message.getInt();
        length = message.getInt();
    }
    
    public BlockMessage(int begin, int index, int length, byte disc) {
        super(13, disc);
        this.index = index;
        this.begin = begin;
        this.length = length;
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
    
    @Override
    public ByteBuffer send() {
        ByteBuffer bb = super.send();
        bb.putInt(index);
        bb.putInt(begin);
        bb.putInt(length);
        return bb;
    }
}
