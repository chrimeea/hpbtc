/*
 * Created on Mar 23, 2006
 *
 */
package hpbtc.client.observer;

import hpbtc.client.torrent.BTFile;
import hpbtc.client.message.ProtocolMessage;
import hpbtc.client.peer.Peer;
import hpbtc.client.piece.Piece;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 * @author chris
 *
 */
public class LoggingObserver implements TorrentObserver {

    private static Logger logger = Logger.getLogger(LoggingObserver.class.getName());

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireServerStartedEvent(int)
     */
    public void fireServerStartedEvent(int port) {
        logger.info("Starting server on port " + port);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireIncomingConnectionEvent(java.lang.String)
     */
    public void fireIncomingConnectionEvent(String ip) {
        logger.info("Incoming connection from " + ip);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireConnectionFailedEvent(hpbtc.peer.Peer, java.io.IOException)
     */
    public void fireConnectionFailedEvent(Peer p, IOException e) {
        logger.warning("Can not connect to " + p.getIp() + " " + e.getMessage());
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireFileCreationEvent(java.lang.String, int)
     */
    public void fireFileCreationEvent(String path, int length) {
        logger.info("Creating file " + path + " with length " + length);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetTrackerURL(java.util.List)
     */
    public void fireSetTrackerURLEvent(List<LinkedList<String>> urls) {
        StringBuilder sb = new StringBuilder();
        for (LinkedList<String> ul: urls) {
            sb.append("[");
            for (String u : ul) {
                sb.append(u);
            }
            sb.append("]");
        }
        logger.info("Tracker URL is " + sb);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetPieceLengthEvent(int)
     */
    public void fireSetPieceLengthEvent(int length) {
        logger.info("Piece length is " + length);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetFilesEvent(java.util.List)
     */
    public void fireSetFilesEvent(List<BTFile> files) {
        for (BTFile f : files) {
            logger.info("File " + f.getPath() + " with length " + f.getLength());
        }
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetTotalPieces(int)
     */
    public void fireSetTotalPiecesEvent(int total) {
        logger.info("Total number of pieces is " + total);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireStartCheckSaved()
     */
    public void fireStartCheckSavedEvent() {
        logger.info("Checking for saved pieces");
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireStartSeeding()
     */
    public void fireStartSeedingEvent() {
        logger.info("All files are complete, start seeding");
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireTrackerNotAvailable(java.lang.String)
     */
    public void fireTrackerNotAvailableEvent(String url) {
        logger.warning("Tracker " + url + " is not available");
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireWaitTracker(int)
     */
    public void fireWaitTrackerEvent(long delay) {
        logger.info("Have to wait " + delay + " milliseconds until connect to tracker again");
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireTrackerFailure(java.lang.String)
     */
    public void fireTrackerFailureEvent(String reason) {
        logger.warning("Got tracker error " + reason);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireTrackerWarningEvent(java.lang.String)
     */
    public void fireTrackerWarningEvent(String warning) {
        logger.warning("Got tracker warning " + warning);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetTrackerMinIntervalEvent(int)
     */
    public void fireSetTrackerMinIntervalEvent(int interval) {
        logger.info("Tracker min interval is " + interval);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetTrackerInterval(int)
     */
    public void fireSetTrackerIntervalEvent(int interval) {
        logger.info("Tracker interval is " + interval);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetSeedersEvent(int)
     */
    public void fireSetSeedersEvent(int complete) {
        logger.info("Total seeders " + complete);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetLeechersEvent(int)
     */
    public void fireSetLeechersEvent(int incomplete) {
        logger.info("Total leechers " + incomplete);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSetTotalPeersEvent(int)
     */
    public void fireSetTotalPeersEvent(int peers) {
        logger.info("Total peers " + peers);
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireSendMessage(hpbtc.message.ProtocolMessage)
     */
    public void fireSendMessageEvent(ProtocolMessage pm) {
        logger.info("Sending " + pm.toString());
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireProcessMessage(hpbtc.message.ProtocolMessage)
     */
    public void fireProcessMessageEvent(ProtocolMessage pm) {
        logger.info("Processing " + pm.toString());
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireHandshakeOKEvent(hpbtc.peer.Peer)
     */
    public void fireHandshakeOKEvent(Peer peer) {
        logger.info("Handshake OK for peer " + peer.getIp());
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#fireHandshakeErrorEvent(hpbtc.peer.Peer)
     */
    public void fireHandshakeErrorEvent(Peer peer) {
        logger.info("Handshake error for peer " + peer.getIp());
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#firePIDOKEvent(hpbtc.peer.Peer)
     */
    public void firePIDOKEvent(Peer peer) {
        logger.info("PID OK for peer " + peer.getIp());
    }

    /* (non-Javadoc)
     * @see hpbtc.observer.TorrentObserver#firePIDErrorEvent(hpbtc.peer.Peer)
     */
    public void firePIDErrorEvent(Peer peer) {
        logger.info("PID error for peer " + peer.getIp());
    }
    
    public void fireSnubEvent(Peer peer, boolean state) {
        logger.info("Peer " + peer.getIp() + " is snubbed(" + state + ")");
    }
    
    public void fireConnectEvent(Peer peer) {
        logger.info("Connecting to peer " + peer.getIp() + " on port " + peer.getPort());
    }
    
    public void fireConnectFailedEvent(Peer peer) {
        logger.warning("Connection to peer " + peer.getIp() + " failed");
    }
    
    public void fireFinishedPieceEvent(Piece piece) {
        logger.info("Piece " + (piece.getIndex() + 1) +" has finished downloading");
    }
    
    public void firePieceDiscardedEvent(Piece piece) {
        logger.info("Piece " + (piece.getIndex() + 1) + " discarded");
    }
    
    public void fireRecoveredPieceEvent(Piece piece) {
        logger.info("Recovered piece " + (piece.getIndex() + 1));
    }
    
    public void fireFlushPieceEvent(Piece piece) {
        logger.info("Saving piece " + (piece.getIndex() + 1) + " on disk");
    }
    
    public void fireRateChangeEvent(Peer peer, int up, int down) {
        logger.finest("Rates for peer " + peer.getIp() + " upload " + up + " download " + down);
    }
}
