
public class test250j {
    public int __getitem__(int idx) {
       return idx*2;
    }

    public String __getattr__(String name) {
        return name;
    }
}
