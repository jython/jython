// Inheritance*.java are used to test for http://bugs.jython.org/issue2104
//
// However this inheritance hierarchy as constructed is still not
// sufficient to reproduce the issue raised by Jorgo Bakker and Jaime
// Saiz, so need to figure out a patch that does reproduce the problem
// before we can commit even what appears to be a trivial bug fix.

package javatests;

public interface Inheritance {
    Inheritance replicateMe();
    Inheritance replicateParent();
    String whoAmI();
    String root();
    String notInAbstract();
    String everyOther();
}
