
package org.bundlebee.remoteservicecall;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

/**
 * Handles remote execution of a Call object.
 *
 * @author joergp
 */
public class Caller {



	public static void sendCall( Call c, OutputStream os ) throws RemotingException {
		ObjectOutputStream oos = null;
		try
		{
			oos = new ObjectOutputStream(os);
			oos.writeObject(c);
		} 
		catch (IOException ex) {
			throw new RemotingException(ex);
		}
		finally {
			try	{ oos.close(); } catch (IOException ex) {}
		}
	}

	public static Object readResult( InputStream is, ClassLoader cl ) throws RemotingException, Exception {
		ObjectInputStream ois = null;
		try
		{
			//
			// create an ObjectInputStream with classes being resolved by the given classloader
			// the Call object itself may reside in a different classloader and can leave classes unresolvable
			//
			ois = new ObjectInputStreamWithServiceClassLoader(is,cl);

			Result r = (Result) ois.readObject();
			switch (r.getType())
			{
				case REGULAR:
					return r.getValue();
				case SERVICEEXCEPTION:
					throw new RemotingException((Throwable) r.getValue());
				case TARGETEXCEPTION:
					throw (Exception) r.getValue();
				case CALLRUNTIMEXCEPTION:
					throw (RuntimeException) r.getValue();
			}
			throw new RuntimeException("result type has unexpected value: " + r.getType().name());
		} 
		catch (IOException ex) {
			throw new RemotingException(ex);
		}
		finally {
			try	{ ois.close(); } catch (IOException ex) {}
		}
	}


	/**
	 * Execute Call remotely using an out/input pair of streams.
	 * The regular output is either returning the result object OR throwing any exception
	 * that the working code has thrown in a valid path of execution.
	 *
	 * Exception that are caused by the remoting itself, unexpected OSGi- or BundleBee
	 * behavior are reported as a RemotingException.
	 *
	 * @param c
	 * @param os
	 * @param is
	 * @return
	 * @throws RemotingException
	 * @throws Exception
	 */
	public static Object executeCall( Call c, OutputStream os, InputStream is, ClassLoader cl ) throws RemotingException, Exception {
		sendCall(c, os);
		return readResult(is,cl);
	}


    public static final String	CONNECTTIMEOUT_KEY		= "org.bundlebee.remoteservicecall.call.connecttimeout";
    public static final String	READTIMEOUT_KEY			= "org.bundlebee.remoteservicecall.call.readtimeout";
    private static final int	DEFAULT_READ_TIMEOUT	= 60000;
    private static final int	DEFAULT_CONNECT_TIMEOUT = 500;

	    /**
     * Default read timeout that takes system property settings into account.
     *
     * @see #READTIMEOUT_KEY
     */
    private static final int	READ_TIMEOUT;
    /**
     * Default connect timeout that takes system property settings into account.
     *
     * @see #CONNECTTIMEOUT_KEY
     */
    private static final int	CONNECT_TIMEOUT;
    private static final String HTTP_POST				= "POST";

  	//
	// determine constants from the environment
	//
    static {
        int defConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
        try {
            final String property = System.getProperty(CONNECTTIMEOUT_KEY, Integer.toString(DEFAULT_CONNECT_TIMEOUT));
            defConnectTimeout = Integer.parseInt(property);
        }
		catch (NumberFormatException e) {}
        CONNECT_TIMEOUT = defConnectTimeout;

        int defReadTimeout = DEFAULT_READ_TIMEOUT;
        try {
            final String property = System.getProperty(READTIMEOUT_KEY, Integer.toString(DEFAULT_READ_TIMEOUT));
            defReadTimeout = Integer.parseInt(property);
        }
		catch (NumberFormatException e) {}
        READ_TIMEOUT = defReadTimeout;
    }


    public static Object executeCall( Call c, URL url, ClassLoader classloader ) throws RemotingException, Exception {
		OutputStream out = null;
		try
		{
			final HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setConnectTimeout(CONNECT_TIMEOUT);
			connection.setReadTimeout(READ_TIMEOUT);
			connection.setRequestMethod(HTTP_POST);
			connection.setDoOutput(true);
			connection.setDoInput(true);
			out = connection.getOutputStream();

			sendCall(c, out);

			final InputStream in = connection.getInputStream(); //getInputStream(connection);

			Object result = readResult( in, classloader );

			return result;
		}
		catch (ProtocolException ex) {
			throw new RuntimeException(ex);
		}
		catch (IOException ex) {
			throw new RemotingException(ex);
		}
		finally {
			try { out.close(); } catch (IOException ex)	{}
		}
    }


}
