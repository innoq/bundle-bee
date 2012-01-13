package org.bundlebee.remoteservicecall;

/**
 *
 * @author joergp
 */
public class StaticCallContext {

	private final static ThreadLocal<StaticCallContext> ME = new ThreadLocal<StaticCallContext>();



	private static StaticCallContext me() {
		if (null == ME.get()) ME.set( new StaticCallContext() );
		return ME.get();
	}

	/**
	 * used to determine whether a call is performed remotely or local
	 * the flag is normally false, just for the time where an execution from remote takes place it is set to true
	 * to avoid double remoting.
	 *
	 */
	boolean mForceLocal = false;


	long mTimeStamp;


    /**
     * Indicates whether this call MUST be executed locally.
     * Is usually true, if the Call object was deserialized.
     * Note that the value is a thread local, i.e. its value
     * differs depending which thread is calling the method.
     * This allows us to access the value from within the aspect
     * woven into instrumented service implementations.
     *
     * @return true or false
     */
    public static boolean isForceLocal() {
        return me().mForceLocal;
    }

    /**
     * Should be called for cleanup after the Call was executed.
     */
    public static void clearForceLocal() {
        me().mForceLocal = false;
    }


    /**
     * Mark a call to be executed locally.
     */
    public static void forceLocal() {
        me().mForceLocal = true;
    }


	public static long setTimeStamp() {
		me().mTimeStamp = System.nanoTime();
		return getTimeStamp();
	}


	public static long getTimeStamp() {
		return me().mTimeStamp;
	}

}
