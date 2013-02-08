
package org.python.util.install;

/**
 * An example child process that generates some output.
 */
public class ChildProcessExample {

  public ChildProcessExample() {
    System.out.println("[ChildProcessExample] is now here.");
  }

  public static void main(String args[]) {
    int i = 0;
    new ChildProcessExample();
    for (i = 0; i < 10; i++) {
      System.out.println("[ChildProcessExample] printing to stdout " + i);
      // occasionally print to stderr, too
      if (i % 3 == 0) {
        System.err.println("[ChildProcessExample] printing to stderr " + i);
      }
      try {
        Thread.sleep(500);
      } catch (InterruptedException ie) {
        ie.printStackTrace();
      }
    }
    System.out.println("[ChildProcessExample] Exiting");
    System.exit(0);
  }

}