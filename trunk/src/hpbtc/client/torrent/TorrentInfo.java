package hpbtc.client.torrent;

import hpbtc.bencoding.BencodingParser;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
import hpbtc.client.Client;
import hpbtc.client.torrent.BTFile;
import hpbtc.client.observer.TorrentObserver;
import hpbtc.client.peer.Peer;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentInfo {

    private byte[] infoHash;
    private TrackerInfo trackers;
    private boolean multiple;
    private int pieceLength;
    private List<BTFile> files;
    private int fileLength;
    private boolean saved;
    private int nrPieces;
    private byte[] pieceHash;

    public TorrentInfo(String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        BencodingParser parser = new BencodingParser(fis);
        BencodedDictionary meta = parser.readNextDictionary();
        fis.close();
        BencodedDictionary info = (BencodedDictionary) meta.get("info");
        infoHash = info.getDigest();
        trackers = new TrackerInfo(meta);
        TorrentObserver to = Client.getInstance().getObserver();
        to.fireSetTrackerURLEvent(trackers.getTrackers());
        multiple = info.containsKey("files");
        fileName = ((BencodedString) info.get("name")).getValue();
        pieceLength = ((BencodedInteger) info.get("piece length")).getValue();
        to.fireSetPieceLengthEvent(pieceLength);
        if (multiple) {
            BencodedList fls = (BencodedList) info.get("files");
            files = new ArrayList<BTFile>(fls.getSize());
            int index = 0;
            int i = 0;
            int off = 0;
            fileLength = 0;
            for (BencodedElement d : fls) {
                BencodedDictionary fd = (BencodedDictionary) d;
                BencodedList dirs = (BencodedList) fd.get("path");
                StringBuilder sb = new StringBuilder(fileName);
                sb.append(File.separator);
                for (BencodedElement dir : dirs) {
                    sb.append(dir);
                    sb.append(File.separator);
                }
                Integer fl = ((BencodedInteger) fd.get("length")).getValue();
                fileLength += fl;
                BTFile f = new BTFile();
                f.setPath(sb.substring(0, sb.length() - 1).toString());
                f.setLength(fl);
                f.setPieceIndex(index);
                f.setIndex(i++);
                f.setOffset(off);
                if (!f.create()) {
                    saved = true;
                }
                files.add(f);
                index = fileLength / pieceLength;
                off = fileLength - index * pieceLength;
            }
        } else {
            files = new ArrayList<BTFile>(1);
            fileLength = ((BencodedInteger) info.get("length")).getValue();
            BTFile f = new BTFile();
            f.setPath(fileName);
            f.setLength(fileLength);
            f.setPieceIndex(0);
            f.setIndex(0);
            f.setOffset(0);
            if (!f.create()) {
                saved = true;
            }
            files.add(f);
        }
        to.fireSetFilesEvent(files);
        nrPieces = fileLength / pieceLength;
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        to.fireSetTotalPiecesEvent(nrPieces);
        pieceHash = ((BencodedString) info.get("pieces")).getBytes();
    }

    public int getFileLength() {
        return fileLength;
    }

    public List<BTFile> getFiles() {
        return files;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public int getNrPieces() {
        return nrPieces;
    }

    public int getPieceLength() {
        return pieceLength;
    }

    public List<LinkedList<String>> getTrackerURL() {
        return trackers.getTrackers();
    }

    public boolean isMultiple() {
        return multiple;
    }

    public boolean isSaved() {
        return saved;
    }

    public byte[] getPieceHash() {
        return pieceHash;
    }
    
    public int getInterval() {
        return trackers.getInterval();
    }
    
    public Set<Peer> tryGetTrackerPeers(String event, int uploaded, int downloaded, int bytesLeft) {
        return trackers.tryGetTrackerPeers(event, uploaded, downloaded, infoHash, bytesLeft);
    }
}
