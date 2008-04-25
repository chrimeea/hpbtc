package hpbtc.protocol.torrent;

import hpbtc.bencoding.BencodingReader;
import hpbtc.bencoding.BencodingWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Cristian Mocanu
 */
public class TorrentInfo {
    
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
        BencodingReader parser = new BencodingReader(is);
        Map<String, Object> meta = parser.readNextDictionary();
        Map<String, Object> info = (Map) meta.get("info");
        infoHash = computeInfoHash(info);
        if (meta.containsKey("announce-list")) {
            trackers = (List<LinkedList<String>>) meta.get("announce-list");
        } else {
            trackers = new ArrayList<LinkedList<String>>(1);
            LinkedList<String> ul = new LinkedList<String>();
            ul.add((String) meta.get("announce"));
            trackers.add(ul);
        }
        if (meta.containsKey("creation date")) {
            creationDate = new Date((Long) meta.get("creation date") * 1000);
        }
        if (meta.containsKey("comment")) {
            comment = (String) meta.get("comment");
        }
        if (meta.containsKey("created by")) {
            createdBy = (String) meta.get("created by");
        }
        if (meta.containsKey("encoding")) {
            encoding = (String) meta.get("encoding");
        }
        multiple = info.containsKey("files");
        String fileName = (String) info.get("name");
        pieceLength = (Long) info.get("piece length");
        if (multiple) {
            List<Map> fls = (List<Map>) info.get("files");
            files = new ArrayList<BTFile>(fls.size());
            fileLength = 0;
            for (Map fd : fls) {
                List<String> dirs = (List<String>) fd.get("path");
                StringBuilder sb = new StringBuilder(fileName);
                sb.append(File.separator);
                for (String dir : dirs) {
                    sb.append(dir);
                    sb.append(File.separator);
                }
                long fl = (Long) fd.get("length");
                fileLength += fl;
                files.add(new BTFile(sb.substring(0, sb.length() - 1).toString(), fl));
            }
        } else {
            files = new ArrayList<BTFile>(1);
            fileLength = (Long) info.get("length");
            files.add(new BTFile(fileName, fileLength));
        }
        nrPieces = fileLength / pieceLength;
        if (fileLength % pieceLength > 0) {
            nrPieces++;
        }
        pieceHash = ((String) info.get("pieces")).getBytes("ISO-8859-1");
    }

    private static byte[] computeInfoHash(Map<String, Object> info) throws NoSuchAlgorithmException, IOException {
        MessageDigest md = MessageDigest.getInstance("SHA1");
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        BencodingWriter w = new BencodingWriter(os);
        w.write(info);
        md.update(os.toByteArray());
        return md.digest();
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
