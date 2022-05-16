package gitlet;

import java.io.File;

/**
 * Driver class for Gitlet, the tiny stupid version-control system.
 *
 * @author Rahul Kumar
 */
public class Main {

    /**
     * Usage: java gitlet.Main ARGS, where ARGS contains
     * <COMMAND> <OPERAND> ....
     */

    private static Controller controller = new Controller();
    /**
     * User dir.
     */
    static final File CWS = new File(System.getProperty("user.dir"));
    /**
     * Gitlet path.
     */
    static final File GITLET_PATH = Utils.join(CWS, ".gitlet");

    public static void main(String... args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        }
        if (args[0].equals("init")) {
            controller.initialize();
        } else if (!GITLET_PATH.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        } else {
            helper(args);
        }
    }

    public static void helper(String[] args) {
        if (args[0].equals("add")) {
            controller.add(args[1]);
        } else if (args[0].equals("commit")) {
            controller.commit(args[1]);
        } else if (args[0].equals("rm")) {
            controller.remove(args[1]);
        } else if (args[0].equals("log")) {
            controller.log();
        } else if (args[0].equals("global-log")) {
            controller.globalLog();
        } else if (args[0].equals("checkout") && args.length == 3) {
            if (args[1].equals("--")) {
                controller.checkoutCase1(args[2]);
            } else {
                System.out.println("Incorrect operands.");
                return;
            }
        } else if (args[0].equals("checkout") && args.length == 2) {
            controller.checkoutCase3(args[1]);
        } else if (args[0].equals("checkout") && args.length == 4) {
            if (args[2].equals("--")) {
                controller.checkoutCase2(args[1], args[3]);
            } else {
                System.out.println("Incorrect operands.");
                return;
            }
        } else if (args[0].equals("checkout")) {
            System.out.println("Incorrect operands.");
        } else if (args[0].equals("find")) {
            controller.find(args[1]);
        } else if (args[0].equals("branch")) {
            controller.branch(args[1]);
        } else if (args[0].equals("reset")) {
            controller.reset(args[1]);
        } else if (args[0].equals("rm-branch")) {
            controller.removeBranch(args[1]);
        } else if (args[0].equals("status")) {
            controller.status();
        } else if (args[0].equals("merge")) {
            controller.merge(args[1]);
        } else if (args[0].equals("add-remote")) {
            controller.addRemote(args[1], "");
        } else if (args[0].equals("rm-remote")) {
            controller.removeRemote(args[1]);
        } else if (args[0].equals("push")) {
            controller.push("", "");
        } else if (args[0].equals("fetch")) {
            controller.fetch("", "");
        } else {
            System.out.println("No command with that name exists.");
        }
    }
}
