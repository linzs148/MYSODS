public class Test1 {
    public static int A() {
        int x = 1;
        int y = x + 1;
        return y;
    }

    private static int B(int m, int n) {
        return m + n;
    }

    public double C(double d) {
        System.out.println("C");
        return d;
    }

    private boolean D(boolean b) {
        System.out.println("D");
        return b;
    }

    public static void main(String[] args) {
        Test1 t = new Test1();
        System.out.println("Hello World");
        double d = t.C(0.0);
        boolean b = t.D(true);
        int x = A();
        int n = 5;
        for (int i = 0; i < n; i++) {
            int y = B(x, i) + i;
        }
        int s = x + n;
        int v = s + n;
        int u = v + n;
        System.out.println(s);
    }

}
