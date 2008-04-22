/*
 * Created on Jan 19, 2006
 *
 */
package hpbtc.client;

import hpbtc.protocol.torrent.TorrentInfo;
import java.io.IOException;

/**
 * @author chris
 *
 */
public class DownloadItem {

    public static final String DOWNLOAD_STARTED = "started";
    public static final String DOWNLOAD_COMPLETED = "completed";
    public static final String DOWNLOAD_STOPPED = "stopped";
    public static final int PEER_MAX_REQUESTS = 2;
    public static final int PIPELINE_SIZE = 10;
    public static final int RECALCULATION_DELAY = 10000;
    public static final int OPTIMISTIC_RATE = 3;
    public static final int OPTIMISTIC = 1;
    public static final int END_THRESHOLD = 2;
    public static final int REQUEST_TIMEOUT = 60000;
    public static final int MAX_CONNECTIONS = 2;
    
    private TorrentInfo torrent;

    /**
     * @param ft
     */
    public DownloadItem(String ft) throws IOException {
        torrent = new TorrentInfo(ft);
    }

    public void stopDownload() {
    }

    public void startDownload() {
    }
}
