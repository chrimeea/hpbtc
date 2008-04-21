package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingParser;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentInfo {

    private static Logger logger = Logger.getLogger(TorrentInfo.class.getName());
    
    private List<LinkedList<String>> trackers;
    private byte[] infoHash;
    private boolean multiple;
    private int pieceLength;
    private List<BTFile> files;
    private int fileLength;
    private int nrPieces;
    private byte[] pieceHash;

    public TorrentInfo(String fileName) throws IOException {
        FileInputStream fis = new FileInputStream(fileName);
        BencodingParser parser = new BencodingParser(fis);
        BencodedDictionary meta = parser.readNextDictionary();
        fis.close();
        BencodedDictionary info = (BencodedDictionary) meta.get("info");
        infoHash = info.getDigest();
        parseTrackers(meta);
        multiple = info.containsKey("files");
        fileName = ((BencodedString) info.get("name")).getValue();
        pieceLength = ((BencodedInteger) info.get("piece length")).getValue();
        logger.info("set piece length " + pieceLength);
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
                BTFile f = new BTFile(pieceLength);
                f.setPath(sb.substring(0, sb.length() - 1).toString());
                f.setLength(fl);
                f.setPieceIndex(index);
                f.setIndex(i++);
                f.setOffset(off);
                files.add(f);
                index = fileLength / pieceLength;
                off = fileLength - index * pieceLength;
            }
        } else {
            files = new ArrayList<BTFile>(1);
            fileLength = ((BencodedInteger) info.get("length")).getValue();
            BTFile f = new BTFile(pieceLength);
            f.setPath(fileName);
            f.setLength(fileLength);
            f.setPieceIndex(0);
            f.setIndex(0);
            f.setOffset(0);
            files.add(f);
        }
        nrPieces = fileLength / pieceLength;
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        logger.info("total pieces " + nrPieces);
        pieceHash = ((BencodedString) info.get("pieces")).getBytes();
    }

    private void parseTrackers(BencodedDictionary meta) {
        if (meta.containsKey("announce-list")) {
            BencodedList bl = (BencodedList) meta.get("announce-list");
            trackers = new ArrayList<LinkedList<String>>(bl.getSize());
            for (BencodedElement ul : bl) {
                BencodedList x = (BencodedList) ul;
                LinkedList<String> z = new LinkedList<String>();
                for (BencodedElement y : x) {
                    String u = ((BencodedString) y).getValue();
                    z.add(u);
                }
                Collections.shuffle(z);
                trackers.add(z);
            }
        } else {
            trackers = new ArrayList<LinkedList<String>>(1);
            LinkedList<String> ul = new LinkedList<String>();
            String u = ((BencodedString) meta.get("announce")).getValue();
            ul.add(u);
            trackers.add(ul);
        }
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

    public boolean isMultiple() {
        return multiple;
    }

    public byte[] getPieceHash() {
        return pieceHash;
    }

    public List<LinkedList<String>> getTrackers() {
        return trackers;
    }
}
