package org.bundlebee.remoteservicecall;

import java.io.Serializable;


/**
 * Result.
 *
 */
public class Result implements Serializable {

	public enum ValueType { REGULAR, TARGETEXCEPTION, SERVICEEXCEPTION, CALLRUNTIMEXCEPTION };
	ValueType mType;
    private Object value;
    public static final Result NULL_RESULT = new Result();


    /**
     * Represents a <code>null</code> result.
     */
    private Result() {
        this((Object)null, ValueType.REGULAR );
    }

    public Result(final Object value, ValueType type ) {
        this.value = value;
		mType = type;
    }


    public Object getValue() {
        return value;
    }

	public ValueType getType() {
		return mType;
	}
}
