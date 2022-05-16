package gitlet;

import java.io.Serializable;

public class Blob implements Serializable {

    /** Contents. */
    private final byte[] contents;
    /** Filename. */
    private final String fileName;

    public Blob(byte[] fileContents, String file) {
        this.contents = fileContents;
        this.fileName = file;
    }

    public byte[] getContents() {
        return this.contents;
    }

    public String getFileName() {
        return this.fileName;
    }

    public String getSha1() {
        return Utils.sha1(Utils.serialize(this));
    }
}
