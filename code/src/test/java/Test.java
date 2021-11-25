public class Test {
    public int Test1(int a) {
        int c = a + 2;
        int d = a - 2;
        int e = c * d;
        int f = c + e;
        int g = d + e;
        return f + g;
    }

    public int Test2(int a, int b) {
        int c = Test1(a);
        int d = a % b;
        int e = c + d;
        int f = e % c;
        int g = a + e;
        return f + g;
    }

    public int Test3(int a, int b, int c) {
        int d = Test2(a, b) - Test1(c);
        int e = d % c;
        int f = a + b;
        int g = e / f;
        return g;
    }

    public static void main(String[] args) {
        Test T = new Test();
        int a = 1;
        int b = 2;
        int c = 3;
        T.Test1(a);
        T.Test2(a, b);
        T.Test3(a, b, c);
    }
}
