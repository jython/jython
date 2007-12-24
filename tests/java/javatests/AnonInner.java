package javatests;

import java.util.Date;

public class AnonInner
{
    public int doit() {
        Date d = new Date() {
                public int hashCode() {
                    return 2000;
                }
            };
        return d.hashCode();
    }
}
