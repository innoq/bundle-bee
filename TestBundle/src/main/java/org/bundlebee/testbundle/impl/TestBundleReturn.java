package org.bundlebee.testbundle.impl;

import java.io.Serializable;

/**
 *
 * @author joergp
 */
public class TestBundleReturn implements Serializable {

    private final String mMessage;

    public TestBundleReturn(String message) {
        mMessage = message;
    }

    public String getMessage() {
        return "Message: " + mMessage;
    }
}
