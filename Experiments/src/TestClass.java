/**
 * @author Derrick Lockwood
 * @created 5/15/18.
 */
public class TestClass {

    private int methodToMock(Foo foo1, Foo foo2) {
        return foo1.test(foo2);
    }

    public int methodToMock1(int one, int two) {
        int three = one + two;
        three += one * two;
        return three;
    }
}