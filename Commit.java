package gitlet;

import java.io.File;
import java.io.Serializable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


/**
 * Class that stores the properties of a commit.
 *
 * @author Rahul Kumar
 */
public class Commit implements Serializable {
    /**
     * Staging path.
     */
    private String stagingPath = ".gitlet/.staging";
    /**
     * Commit path.
     */
    private String commitPath = ".gitlet/.commits";
    /**
     * Blob path.
     */
    private String blobPath = ".gitlet/.blobs";
    /**
     * Removal path.
     */
    static final File REMOVAL_PATH = Utils.join(".gitlet/.staging", ".removal");

    /**
     * Message.
     */
    private final String message;
    /**
     * Timestamp.
     */
    private final String timestamp;
    /**
     * Date string.
     */
    private final String dateString;
    /**
     * Parent one.
     */
    private final Commit parent1;
    /**
     * Parent two.
     */
    private Commit parent2;
    /**
     * Blob contents.
     */
    private ArrayList<String> blobContents;
    /**
     * My branch.
     */
    private final String myBranch;
    /**
     * Sha ID of Commit.
     */
    private String sha1;


    public Commit() {
        TimeZone tz = TimeZone.getTimeZone("PST");
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d hh:mm:ss yyyy Z");
        dateFormat.setTimeZone(tz);

        this.message = "initial commit";
        this.parent1 = null;
        this.parent2 = null;
        this.timestamp = dateFormat.format(new Date(0));
        this.dateString = timestamp.toString();
        this.blobContents = new ArrayList<>();
        this.myBranch = "master";
    }

    public Commit(String messageString, Commit parent) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d hh:mm:ss yyyy Z");
        dateFormat.setTimeZone(tz);

        this.message = messageString;
        this.parent1 = parent;
        this.parent2 = null;
        this.timestamp =
                dateFormat.format(new Date(System.currentTimeMillis()));
        this.dateString = timestamp.toString();
        this.myBranch = "master";
        this.blobContents = new ArrayList<>();
        setBlobContents();
    }

    public Commit(String messageString, Commit parentOne, Commit parentTwo) {
        TimeZone tz = TimeZone.getTimeZone("UTC");
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE MMM d hh:mm:ss yyyy Z");
        dateFormat.setTimeZone(tz);

        this.message = messageString;
        this.parent1 = parentOne;
        this.parent2 = parentTwo;
        this.timestamp =
                dateFormat.format(new Date(System.currentTimeMillis()));
        this.dateString = timestamp.toString();
        this.myBranch = "master";
        this.blobContents = new ArrayList<>();
        setBlobContents();
    }

    public String getMessage() {
        return this.message;
    }

    public String getTimestamp() {
        return this.timestamp;
    }

    public void setParent2(Commit parentwo) {
        this.parent2 = parentwo;
    }

    public String getTimestampString() {
        return this.dateString;
    }

    public Commit getParent1() {
        return this.parent1;
    }

    public Commit getParent2() {
        return this.parent2;
    }

    public String getSha1() {
        return this.sha1;
    }

    public void setSHA1(String sha1Code) {
        this.sha1 = sha1Code;
    }

    public boolean hasParent2() {
        if (this.parent2 != null) {
            return true;
        }
        return false;
    }

    public ArrayList<String> getBlobSHA() {
        ArrayList<String> holder = new ArrayList<>();
        for (String i : this.blobContents) {
            holder.add(i);
        }
        return holder;
    }

    public ArrayList<String> getBlobNames() {
        ArrayList<String> holder = new ArrayList<>();
        for (String i : getBlobSHA()) {
            Blob b = Utils.readObject(Utils.join(blobPath, i), Blob.class);
            holder.add(b.getFileName());
        }
        return holder;
    }

    private void setBlobContents() {
        if (parent1.getBlobSHA().isEmpty()) {
            for (String i : Utils.plainFilenamesIn(stagingPath)) {
                this.blobContents.add(i);
            }
            return;
        } else {
            this.blobContents = parent1.getBlobSHA();
            ArrayList<String> holder = parent1.getBlobSHA();
            List<String> stagedBlobs =
                    Utils.plainFilenamesIn(stagingPath);
            for (String stagedBlobSHA : stagedBlobs) {
                Blob sBlob = Utils.readObject(Utils.join(stagingPath,
                        stagedBlobSHA), Blob.class);
                for (String commitBlobSHA : holder) {
                    Blob cBlob = Utils.readObject(Utils.join(blobPath,
                            commitBlobSHA), Blob.class);
                    if (sBlob.getFileName().equals(cBlob.getFileName())) {
                        this.blobContents.remove(commitBlobSHA);
                        this.blobContents.add(stagedBlobSHA);
                    }
                }
            }
            holder = this.getBlobSHA();
            for (String stagedBlobSHA : stagedBlobs) {
                if (!holder.contains(stagedBlobSHA)) {
                    holder.add(stagedBlobSHA);
                }
            }
            for (String file : Utils.plainFilenamesIn(REMOVAL_PATH)) {
                for (String blobSha : Utils.plainFilenamesIn(blobPath)) {
                    Blob blob = Utils.readObject(Utils.join(blobPath,
                            blobSha), Blob.class);
                    String blobName = blob.getFileName();
                    if (blobName.equals(file)) {
                        if (holder.contains(blobSha)) {
                            holder.remove(blobSha);
                        }
                    }
                }
            }
            this.blobContents = holder;
            for (String file : Utils.plainFilenamesIn(REMOVAL_PATH)) {
                Utils.join(REMOVAL_PATH, file).delete();
            }
        }
    }
}

