
public class test090j {
  public test090j() {
  }
  public int barTimesTwo(Bar bar) {
    return (2*bar.n);
  }
  public static class Bar {
    public Bar() {
    }
    public Bar(int n) {
      this.n = n;
    }
    public int n;
  }
}