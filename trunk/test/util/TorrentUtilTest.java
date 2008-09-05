package util;

import org.junit.Test;

/**
 *
 * @author Chris
 */
public class TorrentUtilTest {

    @Test
    public void testComputeBeginIndex() {
        int index = TorrentUtil.computeBeginIndex(753677, 16384);
        assert index == 46;
    }
    
    @Test
    public void testComputeEndIndex() {
        int index = TorrentUtil.computeEndIndex(753677, 56320, 16384);
        assert index == 50;
    }
    
    @Test
    public void computeBeginPosition() {
        int position = TorrentUtil.computeBeginPosition(46, 16384);
        assert position == 753664;
    }
}
