package test240p;
public class test240j2 implements Runnable {
    int myTestInt = 0;
    int mySleepTime = 0;

    public test240j2(test240j1 config) {
        myTestInt = config.theInt;
        mySleepTime = config.theSleepTime;
    }

    public void run() { }

}
