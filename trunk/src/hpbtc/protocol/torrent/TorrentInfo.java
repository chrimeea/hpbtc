package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingParser;
import hpbtc.bencoding.element.BencodedDictionary;
import hpbtc.bencoding.element.BencodedElement;
import hpbtc.bencoding.element.BencodedInteger;
import hpbtc.bencoding.element.BencodedList;
import hpbtc.bencoding.element.BencodedString;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
    private long pieceLength;
    private List<BTFile> files;
    private long fileLength;
    private long nrPieces;
    private byte[] pieceHash;
    private Date creationDate;
    private String comment;
    private String createdBy;
    private String encoding;

    public TorrentInfo(InputStream is) throws IOException, NoSuchAlgorithmException {
        BencodingParser parser = new BencodingParser(is);
        BencodedDictionary meta = parser.readNextDictionary();
        BencodedDictionary info = (BencodedDictionary) meta.get("info");
        infoHash = info.getDigest();
        parseTrackers(meta);
        if (meta.containsKey("creation date")) {
            creationDate = new Date(((BencodedInteger) meta.get("creation date")).getValue() * 1000);
        }
        if (meta.containsKey("comment")) {
            comment = ((BencodedString) meta.get("comment")).getValue();
        }
        if (meta.containsKey("created by")) {
            createdBy = ((BencodedString) meta.get("created by")).getValue();
        }
        if (meta.containsKey("encoding")) {
            encoding = ((BencodedString) meta.get("encoding")).getValue();
        }
        multiple = info.containsKey("files");
        String fileName = ((BencodedString) info.get("name")).getValue();
        pieceLength = ((BencodedInteger) info.get("piece length")).getValue();
        logger.info("set piece length " + pieceLength);
        if (multiple) {
            BencodedList fls = (BencodedList) info.get("files");
            files = new ArrayList<BTFile>(fls.getSize());
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
                long fl = ((BencodedInteger) fd.get("length")).getValue();
                fileLength += fl;
                files.add(new BTFile(sb.substring(0, sb.length() - 1).toString(), fl));
            }
        } else {
            files = new ArrayList<BTFile>(1);
            fileLength = ((BencodedInteger) info.get("length")).getValue();
            files.add(new BTFile(fileName, fileLength));
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
    
    public long getFileLength() {
        return fileLength;
    }

    public List<BTFile> getFiles() {
        return files;
    }

    public byte[] getInfoHash() {
        return infoHash;
    }

    public long getNrPieces() {
        return nrPieces;
    }

    public long getPieceLength() {
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

    public String getComment() {
        return comment;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public String getEncoding() {
        return encoding;
    }
}
