
public class test129j {
    public static int chk(int[][] arr) {
	arr[0][0] = 47;
	return arr.length * arr[0].length;
    }
}