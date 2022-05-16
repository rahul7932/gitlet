# Gitlet Design Document

#### Author: Rahul Kumar

## Design Document Guidelines

Please use the following format for your Gitlet design document. Your design document should be written in markdown, a
language that allows you to nicely format and style a text file. Organize your design document in a way that will make
it easy for you or a course-staff member to read.

## 1. Classes and Data Structures

### Setup Class

This class will setup the initial directory structure

### Controller Class

The controller class will handle arguments as passed into the main method and will contain the main logic for executing
the appropriate instructions

* `HashMap<Blob, File> blobs`: a HashMap of all blob references with the keys as the file names for each file and the
  values being the File references
* `Commit recentCommit`: the most recent commit 01"

### Commit Class

The commit class needs to include the following instance variables

* `final String message` message: the message associated with the commit
* `final SimpleDateFormat timestamp`: the timestamp associated with when the commit was created
* `ArrayList<File> blobContents`: an ArrayList that contains the file references associated with all of the blobs in
  contained in that commit
* `boolean head`: a boolean describing whether this commit is the head commit
* `final String myBranch`: a string describing what branch the given commit is in
* `final Commit parent`: the previous Commit object
* `ArrayList<Commit> next`: an ArrayList of the next commit(s); will only have a length > 1 if a branch is created after
  this commit

### Blob Class

The commit class needs to include the following instance variables

* `byte[] content`: a byte array that contains the contents of the file in byte form
* `File file`: a File that contains the reference to actual file whose contents we are storing

## 2. Algorithms

### Setup Class

* `public void setupDirectories()`: a method that sets up the directories which are the staging directory, the blobs
  directory, the commits directory, and the .gitlet directory; this is called when the Main method recieves init as an
  argument
* `public void initialSetup()`: a method that is responsible for creating the initial commit

### Controller Class

* `public void initialize()`: a method that will be called in the Main class when the first argument passed in is `init`
  ; this method will set up the directories which are the staging directory, the blobs directory, the commits directory,
  and the .gitlet directory; it wil also create the first commit
* ` public void add(String file)`: a method that will be called in the Main class when the first argument passed
  is `add`
  and the second argument is the name of the file
* ` public void commit(String file)`: a method that will be called in the Main class when the first argument passed
  is `commit`; this method will create a new Commit object, add the files in the staging area to the commit object, and
  then add the commit object to the commit directory
* public void remove(String file): a method that will be called in the Main class when the first argument passed in
  is `rm`; this method will remove the passed in file from the staging area
* public void log(): a method that will be called in the Main class when the first argument passed in is `log`; this
  method will print the history of commits starting at the head commit; can basically iterate through the linked list of
  commits and output their information
* public void addBlobs(): we need a method that will add the blobs that are stored in the staging area to the blobs
  directory so that they can be kept there; it needs to recognize that if there is a file commited with an existing
  blob, it should not commit a new blob and just use the old one
* public void clearStage(): a method that will remove everything from the stage directory after a commit is made

## 3. Persistence

Persistence must be kept for a multiple different factors

1. When a file is added to the staging area, we need to turn it into a blob; this blob needs to contain all of the
   content of the original file
2. When a blob is added to a commit, all of the blobs need to keep all of their contents and the commit object itself
   needs to be saved
3. When a commit is finished and made, it needs to be saved and stored in the commits directory; commits need to never
   be removed and their properties should never be changed
4. When a branch is made, information about which commit is in which branch must persist

## 4. Design Diagram

![](../../../Desktop/Screen Shot 2022-04-15 at 11.43.45 PM.png)
