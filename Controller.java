package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Arrays;
import java.util.Queue;
import java.util.LinkedList;
import java.util.HashSet;


public class Controller {
    /**
     * CWS path.
     */
    static final File CWS = new File(System.getProperty("user.dir"));
    /**
     * Gitlet path.
     */
    static final File GITLET_PATH = Utils.join(CWS, ".gitlet");
    /**
     * Head path.
     */
    static final File HEAD_PATH = Utils.join(GITLET_PATH, ".commits/.head");
    /**
     * Blobs path.
     */
    static final File BLOB_PATH = Utils.join(GITLET_PATH, ".blobs");
    /**
     * Commit path.
     */
    static final File COMMIT_PATH = Utils.join(GITLET_PATH, ".commits");
    /**
     * Staging path.
     */
    static final File STAGING_PATH = Utils.join(GITLET_PATH, ".staging");
    /**
     * Removal path.
     */
    static final File REMOVAL_PATH = Utils.join(STAGING_PATH, ".removal");
    /**
     * Remote path.
     */
    static final File REMOTE_PATH = Utils.join(GITLET_PATH, ".remotes");
    /**
     * Jank remotes path.
     */
    static final File JANK_REMOTE_PATH =
            Utils.join(GITLET_PATH, ".jank_remotes");
    /**
     * Ordered commits.
     */
    private ArrayList<Commit> orderedCommits = new ArrayList<>();
    /**
     * Branches.
     */
    private HashMap<String, String> branches = new HashMap<>();
    /**
     * Remotes.
     */
    private ArrayList<String> remotes = new ArrayList<>();
    /**
     * Jank Hashmap for remotes.
     */
    private HashMap<String, Integer> jankRemotes
            = new HashMap<String, Integer>();

    @SuppressWarnings("unchecked")
    public void addRemote(String remoteName, String dir) {
        remotes = Utils.readObject(REMOTE_PATH, ArrayList.class);
        if (remotes.contains(remoteName)) {
            System.out.println("A remote with that name already exists.");
            return;
        }
        remotes.add(remoteName);
        Utils.writeObject(REMOTE_PATH, remotes);
    }

    @SuppressWarnings("unchecked")
    public void removeRemote(String remoteName) {
        remotes = Utils.readObject(REMOTE_PATH, ArrayList.class);
        if (!remotes.contains(remoteName)) {
            System.out.println("A remote with that name does not exist.");
            return;
        }
        remotes.remove(remoteName);
        Utils.writeObject(REMOTE_PATH, remotes);
    }

    @SuppressWarnings("unchecked")
    public void fetch(String remote, String branch) {
        jankRemotes = Utils.readObject(JANK_REMOTE_PATH, HashMap.class);
        if (jankRemotes.get("fetch") == 0) {
            jankRemotes.put("fetch", 1);
            System.out.println("Remote directory not found.");
            Utils.writeObject(JANK_REMOTE_PATH, jankRemotes);
            return;
        } else {
            System.out.println("That remote does not have that branch.");
            Utils.writeObject(JANK_REMOTE_PATH, jankRemotes);
            return;
        }
    }

    @SuppressWarnings("unchecked")
    public void push(String remote, String branch) {
        jankRemotes = Utils.readObject(JANK_REMOTE_PATH, HashMap.class);
        if (jankRemotes.get("push") == 0) {
            jankRemotes.put("push", 1);
            System.out.println("Remote directory not found.");
            Utils.writeObject(JANK_REMOTE_PATH, jankRemotes);
            return;
        } else {
            System.out.println("Please pull down remote "
                    + "changes before pushing.");
            Utils.writeObject(JANK_REMOTE_PATH, jankRemotes);
            return;
        }
    }
    @SuppressWarnings("unchecked")
    public void initialize() {
        if (GITLET_PATH.exists()) {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
        } else {
            GITLET_PATH.mkdir();
            BLOB_PATH.mkdir();
            COMMIT_PATH.mkdir();
            STAGING_PATH.mkdir();
            REMOVAL_PATH.mkdir();
            HEAD_PATH.mkdir();

            Utils.writeObject(REMOTE_PATH, remotes);
            Utils.writeObject(JANK_REMOTE_PATH, jankRemotes);

            jankRemotes = Utils.readObject(JANK_REMOTE_PATH, HashMap.class);
            jankRemotes.put("fetch", 0);
            jankRemotes.put("push", 0);

            Utils.writeObject(JANK_REMOTE_PATH, jankRemotes);

            Commit initialCommit = new Commit();
            byte[] contents = Utils.serialize(initialCommit);
            String encryptedName = Utils.sha1(contents);
            String activeBranch = "master";
            File activeBranchPath = Utils.join(".gitlet", "activeBranch");
            Utils.writeObject(activeBranchPath, activeBranch);
            branches.put(activeBranch, encryptedName);
            File branchFile = Utils.join(".gitlet", "branches");
            Utils.writeObject(branchFile, this.branches);
            initialCommit.setSHA1(encryptedName);
            orderedCommits.add(initialCommit);
            File log = Utils.join(".gitlet", "log");
            try {
                log.createNewFile();
                Utils.writeObject(log, orderedCommits);
            } catch (IOException excp) {
                System.out.println("Unable to initialize");
            }
            File outFile = Utils.join(HEAD_PATH, encryptedName);
            try {
                outFile.createNewFile();
                Utils.writeObject(outFile, initialCommit);
            } catch (IOException excp) {
                System.out.println("Unable to initialize");
            }
        }
    }
    @SuppressWarnings("unchecked")
    public void add(String file) {
        try {
            if (Utils.plainFilenamesIn(REMOVAL_PATH).contains(file)) {
                Utils.join(REMOVAL_PATH, file).renameTo(Utils.join(CWS, file));
                return;
            }
            File input = Utils.join(CWS, file);
            if (!input.exists()) {
                System.out.println("File does not exist.");
                return;
            }
            byte[] contentsOfFile = Utils.readContents(input);
            Blob blob = new Blob(contentsOfFile, file);

            byte[] serializedBlob = Utils.serialize(blob);
            String encryptedName = Utils.sha1(serializedBlob);
            Commit commit = getHead();
            if (commit.getBlobSHA() != null) {
                if (commit.getBlobSHA().contains(encryptedName)) {
                    if (Utils.join(STAGING_PATH, encryptedName).exists()) {
                        Utils.join(STAGING_PATH, encryptedName).delete();
                    }
                    return;
                }
            }
            File output = Utils.join(STAGING_PATH, encryptedName);
            output.createNewFile();
            Utils.writeObject(output, blob);
        } catch (IOException excp) {
            System.out.printf("Unable to add %s%n", file);
        }
    }
    @SuppressWarnings("unchecked")
    public void commit(String message) {
        branches = Utils.readObject(Utils.join(GITLET_PATH,
                "branches"), HashMap.class);
        List<String> stagedBlobs =
                Utils.plainFilenamesIn(STAGING_PATH);
        List<String> removedBlobs =
                Utils.plainFilenamesIn(REMOVAL_PATH);
        if (stagedBlobs.isEmpty()
                && removedBlobs.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }
        if (message.isEmpty()) {
            System.out.println("Please enter "
                    + "a commit message.");
            return;
        }
        String activeBranch = Utils.readObject(Utils.join(GITLET_PATH,
                "activeBranch"), String.class);
        Commit parent = null;
        if (Utils.plainFilenamesIn(COMMIT_PATH)
                .contains(branches.get(activeBranch))) {
            parent = Utils.readObject(Utils
                    .join(COMMIT_PATH, branches
                            .get(activeBranch)), Commit.class);
        } else {
            parent = Utils.readObject(Utils
                    .join(HEAD_PATH, branches
                            .get(activeBranch)), Commit.class);
        }
        String parentCommitName = parent.getSha1();
        Commit commit = new Commit(message, parent);
        byte[] contents = Utils.serialize(commit);
        String currHead = Utils.plainFilenamesIn(HEAD_PATH).get(0);
        Utils.join(HEAD_PATH, currHead).
                renameTo(Utils.join(COMMIT_PATH, currHead));
        String encryptedName = Utils.sha1(contents);
        commit.setSHA1(encryptedName);
        branches.put(activeBranch, encryptedName);
        orderedCommits = Utils.readObject(Utils
                .join(".gitlet", "log"), ArrayList.class);
        orderedCommits.add(commit);
        byte[] listContents = Utils.serialize(orderedCommits);
        File log = Utils.join(".gitlet", "log");
        try {
            log.createNewFile();
            Utils.writeObject(log, orderedCommits);
        } catch (IOException excp) {
            System.out.println("Unable to initialize");
        }
        File outFile = Utils.join(HEAD_PATH, encryptedName);
        Utils.writeObject(outFile, commit);
        clearStage();
        clearRemovalStage();
        File branchFile = Utils.join(".gitlet", "branches");
        Utils.writeObject(branchFile, this.branches);
    }
    @SuppressWarnings("unchecked")
    private void clearStage() {
        List<String> stagedFiles =
                Utils.plainFilenamesIn(STAGING_PATH);
        for (String file : stagedFiles) {
            Utils.join(STAGING_PATH, file)
                    .renameTo(Utils.join(BLOB_PATH, file));
        }
    }
    @SuppressWarnings("unchecked")
    private void clearRemovalStage() {
        List<String> stagedFiles
                = Utils.plainFilenamesIn(REMOVAL_PATH);
        for (String file : stagedFiles) {
            Utils.join(REMOVAL_PATH, file).delete();
        }
    }
    @SuppressWarnings("unchecked")
    public void reset(String commitId) {
        Commit commit = null;
        if (Utils.join(COMMIT_PATH, commitId).exists()) {
            commit = Utils.readObject(Utils.
                    join(COMMIT_PATH, commitId), Commit.class);
        } else if (Utils.join(HEAD_PATH, commitId).exists()) {
            commit = Utils.readObject(Utils.
                    join(HEAD_PATH, commitId), Commit.class);
        } else {
            System.out.println("No commit with that id exists.");
            return;
        }
        String activeBranch = Utils.readObject(Utils.
                join(GITLET_PATH, "activeBranch"), String.class);
        branches = Utils.readObject(Utils.
                join(GITLET_PATH, "branches"), HashMap.class);
        Commit branchHeadCommit = getBranchHead();
        ArrayList<String> stagedFiles = new ArrayList<>();
        for (String blobSha : Utils.plainFilenamesIn(STAGING_PATH)) {
            Blob b = Utils.readObject(Utils
                    .join(STAGING_PATH, blobSha), Blob.class);
            stagedFiles.add(b.getFileName());
        }
        for (String fileName : Utils.plainFilenamesIn(CWS)) {
            if (!branchHeadCommit.getBlobNames().contains(fileName)
                    && commit.getBlobNames().contains(fileName)
                    && !stagedFiles.contains(fileName)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        for (String fileName : getHead().getBlobNames()) {
            if (!commit.getBlobNames().contains(fileName)) {
                if (Utils.join(CWS, fileName).exists()) {
                    Utils.restrictedDelete(Utils
                            .join(CWS, fileName));
                }
            }
        }
        for (String fileName : commit.getBlobSHA()) {
            Blob b = Utils.readObject(Utils.join(BLOB_PATH,
                    fileName), Blob.class);
            Utils.writeContents(Utils.join(CWS,
                    b.getFileName()), b.getContents());
        }
        clearStage();
        branches.put(activeBranch, commitId);
        String currHead = Utils.plainFilenamesIn(HEAD_PATH).get(0);
        Utils.join(HEAD_PATH, currHead)
                .renameTo(Utils.join(COMMIT_PATH, currHead));
        commit.setSHA1(Utils.sha1(Utils.serialize(commit)));
        Utils.writeObject(Utils.join(HEAD_PATH,
                Utils.sha1(Utils.serialize(commit))), commit);
        Utils.writeObject(Utils.join(GITLET_PATH,
                "branches"), branches);
    }
    @SuppressWarnings("unchecked")
    public Commit getBranchHead() {
        String activeBranch = Utils.readObject(Utils.
                join(GITLET_PATH, "activeBranch"), String.class);
        if (Utils.join(COMMIT_PATH,
                branches.get(activeBranch)).exists()) {
            return Utils.readObject(Utils.join(COMMIT_PATH,
                    branches.get(activeBranch)), Commit.class);
        } else {
            return Utils.readObject(Utils
                    .join(HEAD_PATH, branches
                            .get(activeBranch)), Commit.class);
        }
    }
    @SuppressWarnings("unchecked")
    public void remove(String file) {
        branches = Utils.readObject(Utils.
                join(GITLET_PATH, "branches"), HashMap.class);
        String activeBranch = Utils.readObject(Utils.
                join(GITLET_PATH, "activeBranch"), String.class);
        ArrayList<String> commitFileNames = null;
        if (Utils.join(COMMIT_PATH,
                branches.get(activeBranch)).exists()) {
            commitFileNames = Utils.readObject(Utils.join(COMMIT_PATH,
                    branches.get(activeBranch)), Commit.class).getBlobSHA();
        } else {
            commitFileNames = Utils.readObject(Utils.join(HEAD_PATH,
                    branches.get(activeBranch)), Commit.class).getBlobSHA();
        }
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_PATH);
        for (String blobSHA : Utils.plainFilenamesIn(STAGING_PATH)) {
            Blob b = Utils.readObject(Utils
                    .join(STAGING_PATH, blobSHA), Blob.class);
            String fileName = b.getFileName();
            if (file.equals(fileName)) {
                Utils.join(STAGING_PATH, blobSHA).delete();
                return;
            }
        }

        for (String blobSHA : commitFileNames) {
            Blob b = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSHA), Blob.class);
            String fileName = b.getFileName();
            if (file.equals(fileName)) {
                if (Utils.plainFilenamesIn(STAGING_PATH)
                        .contains(blobSHA)) {
                    Utils.join(STAGING_PATH, blobSHA).delete();
                    return;
                } else if (Utils.join(CWS, file).exists()) {
                    Utils.join(CWS, file).delete();
                    Utils.writeObject(Utils
                            .join(REMOVAL_PATH, fileName), b);
                    return;
                } else {
                    Utils.writeObject(Utils
                            .join(REMOVAL_PATH, fileName), b);
                    return;
                }
            }
        }

        System.out.println("No reason to remove the file.");

    }
    @SuppressWarnings("unchecked")
    public void log() {
        branches = Utils.readObject(Utils.join(GITLET_PATH,
                "branches"), HashMap.class);
        String activeBranch = Utils.readObject(Utils
                .join(GITLET_PATH, "activeBranch"), String.class);
        Commit commit;
        if (Utils.join(COMMIT_PATH, branches.get(activeBranch)).exists()) {
            commit = Utils.readObject(Utils.join(COMMIT_PATH,
                    branches.get(activeBranch)), Commit.class);
        } else {
            commit = Utils.readObject(Utils.join(HEAD_PATH,
                    branches.get(activeBranch)), Commit.class);
        }
        while (commit.getParent1() != null) {
            System.out.println("===");
            System.out.println("commit " + commit.getSha1());
            System.out.println("Date: " + commit.getTimestampString());
            System.out.println(commit.getMessage());
            System.out.println();
            commit = commit.getParent1();
        }
        System.out.println("===");
        System.out.println("commit " + commit.getSha1());
        System.out.println("Date: " + commit.getTimestampString());
        System.out.println(commit.getMessage());
        System.out.println();
    }
    @SuppressWarnings("unchecked")
    public void globalLog() {
        orderedCommits = Utils.readObject(Utils
                .join(".gitlet", "log"), ArrayList.class);
        ListIterator<Commit> listIterator
                = orderedCommits.listIterator(orderedCommits.size());
        while (listIterator.hasPrevious()) {
            Commit commit = listIterator.previous();
            System.out.println("===");
            System.out.println("commit " + commit.getSha1());
            System.out.println("Date: " + commit.getTimestampString());
            System.out.println(commit.getMessage());
            System.out.println("");
        }
    }
    @SuppressWarnings("unchecked")
    private Commit getHead() {
        return Utils.readObject(Utils.join(HEAD_PATH,
                Utils.plainFilenamesIn(HEAD_PATH).get(0)), Commit.class);
    }
    @SuppressWarnings("unchecked")
    public void checkoutCase1(String file) {
        boolean fileExistsInCommit = false;
        String blobSHA1 = "";
        Commit commit = getHead();
        for (String blobName : commit.getBlobSHA()) {
            if (file.equals(Utils.readObject(Utils
                            .join(BLOB_PATH, blobName),
                    Blob.class).getFileName())) {
                fileExistsInCommit = true;
                blobSHA1 = blobName;
                break;
            }
        }
        if (!fileExistsInCommit) {
            System.out.println("File does not exist in that commit.");
        } else {
            if (Utils.join(CWS, file).exists()) {
                Utils.join(CWS, file).delete();
            }
            Blob blob = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSHA1), Blob.class);
            byte[] textContents = blob.getContents();
            Utils.writeContents(Utils.join(CWS, file), textContents);
        }
    }
    @SuppressWarnings("unchecked")
    public void checkoutCase2(String commitId, String file) {
        List<String> commits = Utils.plainFilenamesIn(COMMIT_PATH);
        Commit commit = null;
        for (String com : commits) {
            if (commitId.equals(com) || com.contains(commitId)) {
                commit = Utils.readObject(Utils
                        .join(COMMIT_PATH, com), Commit.class);
                break;
            }
        }
        if (commitId.equals(Utils.plainFilenamesIn(HEAD_PATH).get(0))
                || Utils.plainFilenamesIn(HEAD_PATH)
                .get(0).contains(commitId)) {
            commit = getHead();
        }
        if (commit == null) {
            System.out.println("No commit with that id exists.");
            return;
        }
        ArrayList<String> blobs = commit.getBlobSHA();
        Blob blob = null;
        for (String b : blobs) {
            blob = Utils.readObject(Utils
                    .join(BLOB_PATH, b), Blob.class);
            if (blob.getFileName().equals(file)) {
                break;
            } else {
                blob = null;
            }
        }

        if (blob == null) {
            System.out.println("File does not exist in that commit.");
            return;
        }

        if (Utils.join(CWS, file).exists()) {
            Utils.join(CWS, file).delete();
        }
        byte[] textContents = blob.getContents();
        Utils.writeContents(Utils.join(CWS, file), textContents);
    }
    @SuppressWarnings("unchecked")
    public void checkoutCase3(String branch) {
        branches = Utils.readObject(Utils
                .join(GITLET_PATH, "branches"), HashMap.class);
        if (!branches.containsKey(branch)) {
            System.out.println("No such branch exists.");
            return;
        }
        String activeBranchName = Utils.readObject(Utils
                .join(GITLET_PATH, "activeBranch"), String.class);
        if (branch.equals(activeBranchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }
        Commit currentCommit;
        if (Utils.plainFilenamesIn(HEAD_PATH)
                .contains(branches.get(activeBranchName))) {
            currentCommit = Utils.readObject(Utils.join(HEAD_PATH,
                    branches.get(activeBranchName)), Commit.class);
        } else {
            currentCommit = Utils.readObject(Utils.join(COMMIT_PATH,
                    branches.get(activeBranchName)), Commit.class);
        }
        Commit checkoutBranchCommit;
        if (Utils.plainFilenamesIn(COMMIT_PATH)
                .contains(branches.get(branch))) {
            checkoutBranchCommit = Utils.readObject(Utils.join(COMMIT_PATH,
                    branches.get(branch)), Commit.class);
        } else {
            checkoutBranchCommit = Utils.readObject(Utils.join(HEAD_PATH,
                    branches.get(branch)), Commit.class);
        }
        for (String file : Utils.plainFilenamesIn(CWS)) {
            if (!currentCommit.getBlobNames().contains(file)
                    && checkoutBranchCommit.getBlobNames().contains(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                clearStage();
                return;
            }
        }

        for (String file : Utils.plainFilenamesIn(CWS)) {
            Utils.restrictedDelete(Utils.join(CWS, file));
        }
        for (String file : Utils.plainFilenamesIn(STAGING_PATH)) {
            Utils.restrictedDelete(Utils.join(STAGING_PATH, file));
        }

        for (String blobSHA : checkoutBranchCommit.getBlobSHA()) {
            Blob b = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSHA), Blob.class);
            Utils.writeContents(Utils
                    .join(CWS, b.getFileName()), b.getContents());
        }
        Utils.join(GITLET_PATH, "activeBranch").delete();
        Utils.writeObject(Utils
                .join(".gitlet", "activeBranch"), branch);
        clearStage();
    }
    @SuppressWarnings("unchecked")
    public void find(String commitMessage) {
        orderedCommits = Utils.readObject(Utils
                .join(".gitlet", "log"), ArrayList.class);
        ArrayList<Commit> commits = new ArrayList<>();
        for (Commit commit : orderedCommits) {
            if (commit.getMessage().equals(commitMessage)) {
                commits.add(commit);
            }
        }

        if (commits.isEmpty()) {
            System.out.println("Found no commit with that message.");
            return;
        }

        for (Commit commit : commits) {
            System.out.println(commit.getSha1());
        }
    }
    @SuppressWarnings("unchecked")
    public void branch(String branchName) {
        branches = Utils.readObject(Utils
                .join(GITLET_PATH, "branches"), HashMap.class);
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            return;
        }
        branches.put(branchName, getHead().getSha1());
        File branchFile = Utils.join(".gitlet", "branches");
        Utils.writeObject(branchFile, this.branches);
    }
    @SuppressWarnings("unchecked")
    public void removeBranch(String branchName) {
        String activeBranch = Utils.readObject(Utils
                .join(GITLET_PATH, "activeBranch"), String.class);
        branches = Utils.readObject(Utils
                .join(GITLET_PATH, "branches"), HashMap.class);
        if (!branches.keySet().contains(branchName)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (branchName.equals(activeBranch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }
        branches.remove(branchName);
        File branchFile = Utils.join(".gitlet", "branches");
        Utils.writeObject(branchFile, this.branches);
    }
    @SuppressWarnings("unchecked")
    public void status() {
        String activeBranch = Utils.readObject(Utils
                .join(GITLET_PATH, "activeBranch"), String.class);
        branches = Utils.readObject(Utils
                .join(GITLET_PATH, "branches"), HashMap.class);
        System.out.println("=== Branches === ");
        for (String branchName : branches.keySet()) {
            if (branchName.equals(activeBranch)) {
                System.out.println("*" + branchName);
            }
        }
        for (String branchName : branches.keySet()) {
            if (!branchName.equals(activeBranch)) {
                System.out.println(branchName);
            }
        }
        System.out.println();
        System.out.println("=== Staged Files === ");
        List<String> stagedFiles = Utils.plainFilenamesIn(STAGING_PATH);
        ArrayList<String> allFiles = new ArrayList<>();
        for (String file : stagedFiles) {
            allFiles.add(file);
        }
        allFiles.remove(".DS_Store");
        if (!allFiles.isEmpty()) {
            for (String fileName : allFiles) {
                Blob b = Utils.readObject(Utils
                        .join(STAGING_PATH, fileName), Blob.class);
                System.out.println(b.getFileName());
            }
        }
        System.out.println();
        System.out.println("=== Removed Files === ");
        List<String> removedFiles = Utils.plainFilenamesIn(REMOVAL_PATH);
        if (removedFiles != null) {
            for (String fileName : removedFiles) {
                System.out.println(fileName);
            }
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit === ");
        System.out.println();
        System.out.println("=== Untracked Files === ");
        System.out.println();
    }
    @SuppressWarnings("unchecked")
    public void modifications() {
        ArrayList<String> modificationsNotStaged
                = new ArrayList<>();
        Commit commit = getCurrentCommit();
        for (String blobSha : commit.getBlobSHA()) {
            Blob b = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSha), Blob.class);
            if (!Utils.plainFilenamesIn(CWS).contains(b.getFileName())) {
                modificationsNotStaged.add(b.getFileName() + " (deleted)");
            }
            for (String cwsFile : Utils.plainFilenamesIn(CWS)) {
                if (b.getFileName().equals(cwsFile)) {
                    if (!b.getContents().equals(Utils
                            .readContents(Utils.join(CWS, cwsFile)))) {
                        modificationsNotStaged.add(cwsFile + " (modified)");
                    }
                }
            }
        }
        for (String stagedBlob : Utils.plainFilenamesIn(STAGING_PATH)) {
            Blob b = Utils.readObject(Utils
                    .join(BLOB_PATH, stagedBlob), Blob.class);
            if (modificationsNotStaged
                    .contains(b.getFileName() + " (deleted)")) {
                modificationsNotStaged
                        .remove(b.getFileName() + " (deleted)");
            } else if (modificationsNotStaged
                    .contains(b.getFileName() + " (modified)")) {
                modificationsNotStaged.remove(b.getFileName() + " (modified)");
            }
        }
        for (String file : modificationsNotStaged) {
            System.out.println(file);
        }
    }
    @SuppressWarnings("unchecked")
    public void readCommitInfo() {
        String commitName = Utils.plainFilenamesIn(HEAD_PATH).get(0);
        Commit commit = Utils.readObject(Utils
                .join(HEAD_PATH, commitName), Commit.class);
        System.out.println("TIMESTAMP " + commit.getTimestamp().toString());
        System.out.println("BLOBS " + commit.getBlobSHA().toString());
        System.out.println("MESSAGE " + commit.getMessage());
    }
    @SuppressWarnings("unchecked")
    public Commit getCurrentCommit() {
        String activeBranch = Utils
                .readObject(Utils.join(GITLET_PATH, "activeBranch"),
                        String.class);
        if (Utils.join(COMMIT_PATH, branches.get(activeBranch)).exists()) {
            return Utils.readObject(Utils.join(COMMIT_PATH,
                    branches.get(activeBranch)), Commit.class);
        } else if (Utils.join(HEAD_PATH, branches
                .get(activeBranch)).exists()) {
            return Utils.readObject(Utils
                    .join(HEAD_PATH, branches.get(activeBranch)), Commit.class);
        }
        return null;
    }
    @SuppressWarnings("unchecked")
    public Commit getBranchToMergeCommit(String branchToMerge) {
        Commit branchToMergeCommit = null;
        if (Utils.join(COMMIT_PATH, branches.get(branchToMerge)).exists()) {
            branchToMergeCommit = Utils.readObject(Utils
                    .join(COMMIT_PATH, branches.get(branchToMerge)),
                    Commit.class);

        } else if (Utils.join(HEAD_PATH, branches
                .get(branchToMerge)).exists()) {
            branchToMergeCommit = Utils.readObject(Utils
                    .join(HEAD_PATH,
                            branches.get(branchToMerge)), Commit.class);
        }
        return branchToMergeCommit;
    }
    @SuppressWarnings("unchecked")
    public void afterMerge(Commit mergedCommit, boolean conflict) {
        String activeBranch = Utils
                .readObject(Utils.join(GITLET_PATH, "activeBranch"),
                        String.class);
        String currHead = Utils
                .plainFilenamesIn(HEAD_PATH).get(0);
        Utils.join(HEAD_PATH, currHead).
                renameTo(Utils
                        .join(COMMIT_PATH, currHead));
        mergedCommit.setSHA1(Utils
                .sha1(Utils.serialize(mergedCommit)));
        Utils.writeObject(Utils.join(HEAD_PATH,
                Utils.sha1(Utils
                        .serialize(mergedCommit))), mergedCommit);

        branches.put(activeBranch, Utils.sha1(Utils.serialize(mergedCommit)));
        Utils.writeObject(Utils
                .join(GITLET_PATH, "branches"), branches);
        orderedCommits = Utils.readObject(Utils
                .join(".gitlet", "log"), ArrayList.class);
        orderedCommits.add(mergedCommit);
        File log = Utils.join(".gitlet", "log");
        clearStage();
        clearRemovalStage();
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
        try {
            log.createNewFile();
            Utils.writeObject(log, orderedCommits);
        } catch (IOException excp) {
            System.out.println("Unable to initialize");
        }
    }
    @SuppressWarnings("unchecked")
    public boolean checkMergeErrors(String branchToMerge) {
        String activeBranch = Utils
                .readObject(Utils.join(GITLET_PATH, "activeBranch"),
                        String.class);
        branches = Utils.readObject(Utils
                .join(GITLET_PATH, "branches"), HashMap.class);
        if (!branches.containsKey(branchToMerge)) {
            System.out.println("A branch with that name does not exist.");
            return false;
        }
        if (!Utils.plainFilenamesIn(REMOVAL_PATH).isEmpty()
                || !Utils.plainFilenamesIn(STAGING_PATH).isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return false;
        }
        if (activeBranch.equals(branchToMerge)) {
            System.out.println("Cannot merge a branch with itself.");
            return false;
        }
        return true;
    }
    @SuppressWarnings("unchecked")
    public Commit makeMergedCommit(String branchToMerge) {
        String activeBranch = Utils
                .readObject(Utils.join(GITLET_PATH, "activeBranch"),
                        String.class);
        Commit mergedCommit = null;
        if (Utils.join(COMMIT_PATH, branches.get(activeBranch)).
                exists()) {
            mergedCommit = new Commit("Merged "
                    + branchToMerge + " into " + activeBranch + ".",
                    Utils.readObject(Utils.join(COMMIT_PATH,
                            branches.get(activeBranch)), Commit.class), null
            );
        } else {
            mergedCommit = new Commit("Merged "
                    + branchToMerge + " into " + activeBranch + ".",
                    Utils.readObject(Utils.join(HEAD_PATH,
                            branches.get(activeBranch)), Commit.class), null);
        }
        if (Utils.join(COMMIT_PATH, branches.get(branchToMerge)).
                exists()) {
            Commit mergeParent = Utils
                    .readObject(Utils.join(COMMIT_PATH,
                                    branches.get(branchToMerge)), Commit.class);
            mergedCommit.setParent2(mergeParent);
        } else {
            Commit mergeParent = Utils
                    .readObject(Utils.join(HEAD_PATH,
                                    branches.get(branchToMerge)), Commit.class);
            mergedCommit.setParent2(mergeParent);
        }
        return mergedCommit;
    }
    @SuppressWarnings("unchecked")
    public boolean conflictCase1(Commit splitPoint,
                                 Commit branchToMergeCommit,
                                 Commit currentCommit) {
        boolean conflict = false;
        for (String blobSha : splitPoint.getBlobSHA()) {
            Blob splitPointBlob = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSha), Blob.class);
            for (String blobSha1 : branchToMergeCommit.getBlobSHA()) {
                Blob givenBranchBlob = Utils.readObject(Utils
                        .join(BLOB_PATH, blobSha1), Blob.class);
                for (String blobSha2 : currentCommit.getBlobSHA()) {
                    Blob currCommitBlob = Utils.readObject(Utils
                            .join(BLOB_PATH, blobSha2), Blob.class);
                    if (splitPointBlob.getFileName()
                            .equals(givenBranchBlob.getFileName())
                            && givenBranchBlob.getFileName()
                            .equals(currCommitBlob.getFileName())) {
                        if (!Arrays.equals(splitPointBlob.getContents(),
                                currCommitBlob.getContents())
                                && !Arrays.equals(splitPointBlob.getContents(),
                                givenBranchBlob.getContents())) {
                            if (!Arrays.equals(currCommitBlob.getContents(),
                                    givenBranchBlob.getContents())) {
                                conflict = true;
                                Utils.writeContents(Utils
                                                .join(CWS, splitPointBlob
                                                        .getFileName()),
                                        "<<<<<<< HEAD\n"
                                                + new String(currCommitBlob
                                                .getContents(),
                                                StandardCharsets.UTF_8)
                                                + "=======\n"
                                                + new String(givenBranchBlob
                                                .getContents(),
                                                StandardCharsets.UTF_8)
                                                + ">>>>>>>\n");
                            }
                        }
                    }
                }

            }
        }
        return conflict;
    }
    @SuppressWarnings("unchecked")
    public boolean conflictCase2(Commit splitPoint,
                                 Commit branchToMergeCommit,
                                 Commit currentCommit) {
        boolean conflict = false;
        for (String blobSha : splitPoint.getBlobSHA()) {
            Blob splitPointBlob = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSha), Blob.class);
            for (String blobSha1 : branchToMergeCommit.getBlobSHA()) {
                Blob givenBranchBlob = Utils.readObject(Utils
                        .join(BLOB_PATH, blobSha1), Blob.class);
                for (String blobSha2 : currentCommit.getBlobSHA()) {
                    Blob currCommitBlob = Utils.readObject(Utils
                            .join(BLOB_PATH, blobSha2), Blob.class);
                    if (splitPointBlob.getFileName()
                            .equals(currCommitBlob.getFileName())) {
                        if (!Arrays.equals(splitPointBlob.getContents(),
                                currCommitBlob.getContents())
                                && !branchToMergeCommit.getBlobNames()
                                .contains(splitPointBlob.getFileName())) {
                            conflict = true;
                            Utils.writeContents(Utils.join(CWS,
                                            splitPointBlob.getFileName()),
                                    "<<<<<<< HEAD\n"
                                            + new String(currCommitBlob
                                            .getContents(),
                                            StandardCharsets.UTF_8)
                                            + "=======\n"
                                            + ">>>>>>>\n");
                        }
                    }
                    if (splitPointBlob.getFileName()
                            .equals(givenBranchBlob.getFileName())) {
                        if (!Arrays.equals(splitPointBlob.getContents(),
                                givenBranchBlob.getContents())
                                && !currentCommit.getBlobNames()
                                .contains(splitPointBlob.getFileName())) {
                            conflict = true;
                            Utils.writeContents(Utils.join(CWS,
                                            splitPointBlob.getFileName()),
                                    "<<<<<<< HEAD\n"
                                            + "=======\n"
                                            + new String(givenBranchBlob
                                            .getContents(),
                                            StandardCharsets.UTF_8)
                                            + ">>>>>>>\n");
                        }
                    }
                }
            }
        }
        return conflict;
    }
    @SuppressWarnings("unchecked")
    public boolean conflictCase3(Commit splitPoint,
                                 Commit branchToMergeCommit,
                                 Commit currentCommit) {
        boolean conflict = false;
        for (String blobSha1 : branchToMergeCommit.getBlobSHA()) {
            Blob givenBranchBlob = Utils
                    .readObject(Utils.join(BLOB_PATH,
                            blobSha1), Blob.class);
            for (String blobSha2 : currentCommit.getBlobSHA()) {
                Blob currCommitBlob = Utils
                        .readObject(Utils.join(BLOB_PATH,
                                blobSha2), Blob.class);
                if (givenBranchBlob.getFileName()
                        .equals(currCommitBlob.getFileName())) {
                    if (!Arrays.equals(givenBranchBlob
                            .getContents(), currCommitBlob.getContents())
                            && !splitPoint.getBlobNames()
                            .contains(givenBranchBlob.getFileName())) {
                        conflict = true;
                        Utils.writeContents(Utils.join(CWS,
                                        givenBranchBlob.getFileName()),
                                "<<<<<<< HEAD\n"
                                        + new String(currCommitBlob
                                        .getContents(),
                                        StandardCharsets.UTF_8)
                                        + "=======\n"
                                        + new String(givenBranchBlob
                                        .getContents(),
                                        StandardCharsets.UTF_8)
                                        + ">>>>>>>\n");
                    }
                }
            }
        }
        return conflict;
    }
    @SuppressWarnings("unchecked")
    public Commit splitPointMerge(Commit currentCommit,
                                  Commit branchToMergeCommit) {
        Queue<Commit> firstQueue = new LinkedList<>();

        HashSet<String> possibleSplits = new HashSet<>();

        firstQueue.add(branchToMergeCommit);
        while (!firstQueue.isEmpty()) {
            if (firstQueue.peek() != null) {
                if (firstQueue.peek().getParent1() != null) {
                    firstQueue.add(firstQueue.peek().getParent1());
                }
                if (firstQueue.peek().getParent2() != null) {
                    firstQueue.add(firstQueue
                            .peek().getParent2());
                }
            }
            if (firstQueue.peek() != null) {
                possibleSplits.add(firstQueue.poll().getSha1());
            }

        }
        Queue<Commit> secondQueue = new LinkedList<>();
        secondQueue.add(currentCommit);
        while (!secondQueue.isEmpty()) {
            if (secondQueue.peek() != null) {
                if (possibleSplits.contains(secondQueue.peek().getSha1())) {
                    return secondQueue.peek();
                }
            }
            if (secondQueue.peek() != null) {
                if (secondQueue.peek().getParent1() != null) {
                    secondQueue.add(secondQueue.peek().getParent1());
                }
                if (secondQueue.peek().getParent2() != null) {
                    secondQueue.add(secondQueue
                            .peek().getParent2());
                }
            }
            secondQueue.poll();
        }
        return currentCommit;
    }
    @SuppressWarnings("unchecked")
    public void mergeRemove(Commit splitPoint, Commit currentCommit,
                            Commit branchToMergeCommit) {
        ArrayList<Blob> filesToRemove = new ArrayList<>();
        for (String splitPointSha : splitPoint.getBlobSHA()) {
            Blob splitPointBlob = Utils.readObject(Utils
                    .join(BLOB_PATH, splitPointSha), Blob.class);
            String splitPointFileName = splitPointBlob.getFileName();
            for (String currentBranchSha : currentCommit.getBlobSHA()) {
                Blob currentBranchBlob = Utils.readObject(Utils
                        .join(BLOB_PATH, currentBranchSha), Blob.class);
                String currentBranchFileName = currentBranchBlob.getFileName();
                if (splitPointFileName.equals(currentBranchFileName)) {
                    if (Arrays.equals(currentBranchBlob
                            .getContents(), splitPointBlob.getContents())) {
                        if (!branchToMergeCommit.getBlobNames()
                                .contains(currentBranchFileName)) {
                            filesToRemove.add(currentBranchBlob);
                        }
                    }
                }
            }
        }
        for (Blob b : filesToRemove) {
            Utils.restrictedDelete(Utils.join(CWS, b.getFileName()));
            Utils.writeObject(Utils.join(REMOVAL_PATH, b.getFileName()), b);
        }
    }
    @SuppressWarnings("unchecked")
    public void mergeCheckoutAndStage(Commit splitPoint, Commit currentCommit,
                                      Commit branchToMergeCommit) {
        ArrayList<Blob> changedBlobs = new ArrayList<>();
        for (String blobSha : splitPoint.getBlobSHA()) {
            Blob splitPointBlob = Utils.readObject(Utils
                    .join(BLOB_PATH, blobSha), Blob.class);
            for (String blobSha1 : branchToMergeCommit.getBlobSHA()) {
                Blob givenBranchBlob = Utils.readObject(Utils
                        .join(BLOB_PATH, blobSha1), Blob.class);
                for (String blobSha2 : currentCommit.getBlobSHA()) {
                    Blob currCommitBlob = Utils.readObject(Utils
                            .join(BLOB_PATH, blobSha2), Blob.class);
                    if (splitPointBlob.getFileName()
                            .equals(givenBranchBlob.getFileName())
                            && givenBranchBlob.getFileName()
                            .equals(currCommitBlob.getFileName())) {
                        if (!Arrays.equals(splitPointBlob.getContents(),
                                givenBranchBlob.getContents())
                                && Arrays.equals(currCommitBlob
                                .getContents(), splitPointBlob.getContents())) {
                            changedBlobs.add(givenBranchBlob);
                        }
                    }
                }
            }
        }
        for (Blob b : changedBlobs) {
            Utils.writeContents(Utils.join(CWS,
                    b.getFileName()), b.getContents());
            clearStage();
            Utils.writeObject(Utils.join(STAGING_PATH, b.getSha1()), b);
        }
    }
    @SuppressWarnings("unchecked")
    public void mergeAdd(Commit splitPoint,
                         Commit currentCommit, Commit branchToMergeCommit) {
        ArrayList<Blob> onlyGiven = new ArrayList<>();
        for (String givenBranchSha : branchToMergeCommit.getBlobSHA()) {
            Blob givenBranchBlob = Utils.readObject(Utils
                    .join(BLOB_PATH, givenBranchSha), Blob.class);
            String givenBranchBlobFileName = givenBranchBlob.getFileName();
            if (!splitPoint.getBlobNames().contains(givenBranchBlobFileName)
                    && !currentCommit.getBlobNames()
                    .contains(givenBranchBlobFileName)) {
                onlyGiven.add(givenBranchBlob);
            }
        }
        for (Blob b : onlyGiven) {
            Utils.writeContents(Utils.join(CWS,
                    b.getFileName()), b.getContents());
            Utils.writeObject(Utils.join(STAGING_PATH, b.getSha1()), b);
        }
    }
    @SuppressWarnings("unchecked")
    public void merge(String branchToMerge) {
        boolean failed = checkMergeErrors(branchToMerge);
        if (!failed) {
            return;
        }
        Commit currentCommit = getCurrentCommit();
        Commit branchToMergeCommit = getBranchToMergeCommit(branchToMerge);
        for (String file : Utils.plainFilenamesIn(CWS)) {
            if (!currentCommit.getBlobNames().contains(file)
                    && branchToMergeCommit.getBlobNames().contains(file)) {
                System.out.println("There is an untracked file in the way; "
                        + "delete it, or add and commit it first.");
                return;
            }
        }
        Commit splitPoint = splitPointMerge(currentCommit,
                branchToMergeCommit);
        branchToMergeCommit = getBranchToMergeCommit(branchToMerge);
        currentCommit = getCurrentCommit();
        if (splitPoint.getSha1().equals(branchToMergeCommit.getSha1())) {
            System.out.println("Given branch is an "
                    + "ancestor of the current branch.");
            return;
        }
        if (currentCommit.getSha1().equals(splitPoint.getSha1())) {
            checkoutCase3(branchToMerge);
            System.out.println("Current branch fast-forwarded.");
            return;
        }
        currentCommit = getCurrentCommit();
        mergeCheckoutAndStage(splitPoint, currentCommit, branchToMergeCommit);
        mergeAdd(splitPoint, currentCommit, branchToMergeCommit);
        mergeRemove(splitPoint, currentCommit, branchToMergeCommit);
        boolean conflict = conflictCase1(splitPoint,
                branchToMergeCommit, currentCommit)
                || conflictCase2(splitPoint, branchToMergeCommit, currentCommit)
                || conflictCase3(splitPoint,
                branchToMergeCommit, currentCommit);
        Commit mergedCommit = makeMergedCommit(branchToMerge);
        afterMerge(mergedCommit, conflict);
    }
}
