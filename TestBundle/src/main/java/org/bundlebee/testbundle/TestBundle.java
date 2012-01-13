package org.bundlebee.testbundle;

import org.bundlebee.testbundle.impl.TestBundleReturn;

/**
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface TestBundle {

    void helloWorld();

    void throwException() throws Exception;

    void hasParameters(String parameter);

    String hasReturnValue();

    Object hasNonSerializableReturnValue();

    String[] returnsObjectArray();

    int[] returnsPrimitiveArray();

    String[][] returnsTwoDimensionalObjectArray();

    void beep();

    TestBundleReturn echo(String message);

    TestBundleReturn echoReturn(TestBundleReturn r);

    int returnIntPrimitive(int v);

    short returnShortPrimitive(short v);

    long returnLongPrimitive(long v);

    float returnFloatPrimitive(float v);

    double returnDoublePrimitive(double v);

    boolean returnBooleanPrimitive(boolean v);

    byte returnBytePrimitive(byte v);

    char returnCharPrimitive(char v);
}
