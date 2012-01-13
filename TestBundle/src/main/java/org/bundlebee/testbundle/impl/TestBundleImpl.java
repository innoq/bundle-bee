package org.bundlebee.testbundle.impl;

import java.io.Serializable;
import org.bundlebee.testbundle.TestBundle;

/**
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class TestBundleImpl implements TestBundle {

    public TestBundleReturn echo(final String message) {
        System.out.println(message);

        TestBundleReturn f = new TestBundleReturn(message);
        return f;
    }

    public void helloWorld() {
        System.out.println("Hello World");
    }

    //
    // make it harder, use a custom Exception class
    //
    static class TestBundleImplException extends Exception implements Serializable {

        private TestBundleImplException(String message) {
            super(message);
        }
    }

    public void throwException() throws Exception {
        System.out.println("throwException(): throwing exception");

        // make it harder: throw an exception of a different class as declared
        throw new TestBundleImplException("TestBundle Exception");
    }

    public void hasParameters(final String parameter) {
        System.out.println("Parameter: " + parameter);
    }

    public String hasReturnValue() {
        return "A return value.";
    }

    public Object hasNonSerializableReturnValue() {
        return new Object();
    }

    public String[] returnsObjectArray() {
        return new String[0];
    }

    public int[] returnsPrimitiveArray() {
        return new int[0];
    }

    public String[][] returnsTwoDimensionalObjectArray() {
        return new String[0][];
    }

    public void beep() {
        System.out.println("beep...");
    }

    public int returnIntPrimitive(int v) {
        return v;
    }

    public short returnShortPrimitive(short v) {
        return v;
    }

    public long returnLongPrimitive(long v) {
        return v;
    }

    public float returnFloatPrimitive(float v) {
        return v;
    }

    public double returnDoublePrimitive(double v) {
        return v;
    }

    public boolean returnBooleanPrimitive(boolean v) {
        return v;
    }

    public byte returnBytePrimitive(byte v) {
        return v;
    }

    public char returnCharPrimitive(char v) {
        return v;
    }

    public TestBundleReturn echoReturn(TestBundleReturn r) {
        System.out.println("echoReturn returning: " + r);
        return r;
    }

    /**
     * should not be weaved
     */
    public static void staticMethod() {
        // intentionally empty
    }
}
