

public class test239j2 implements Runnable {
    int myTestInt = 0;
    int mySleepTime = 0;

    public test239j2(test239j1 config) {
        myTestInt = config.theInt;
        mySleepTime = config.theSleepTime;
    }

    public void run() { }

}
