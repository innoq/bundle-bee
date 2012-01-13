
package org.bundlebee.remoteservicecall;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import org.osgi.framework.BundleContext;

/**
 *
 * @author joergp
 */
public class Callee {


	public static Result executeCall( InputStream is, OutputStream os, BundleContext ctx ) throws IOException
	{
		Result r;


		// I run the call on remote invokation, so I need to execute it locally
		StaticCallContext.forceLocal();

		try {
			ObjectInputStreamWithServiceClassLoader ois = new ObjectInputStreamWithServiceClassLoader(is, ctx);

			r = execute(ois);
			new ObjectOutputStream(os).writeObject( r );
		}
		finally {
			
			// in any case, clear the forceLocal-flag
			StaticCallContext.clearForceLocal();
		}

		return r;
	}

	
	private static Result execute( ObjectInputStreamWithServiceClassLoader ois ) throws IOException {
		try
		{
			Call cremote = (Call) ois.readObject();
			Object r = cremote.execute();

			return new Result(r,Result.ValueType.REGULAR);
			
		} 
		catch (InvocationTargetException ex) {
			return new Result(ex.getCause(),Result.ValueType.TARGETEXCEPTION);
		} 
		catch (ServiceNotAvailableException ex) {
			return new Result(ex,Result.ValueType.SERVICEEXCEPTION);
		}
		catch (CallRuntimeException ex) {
			return new Result(ex,Result.ValueType.CALLRUNTIMEXCEPTION);
		}
		catch (ClassNotFoundException ex) {
			return new Result(ex,Result.ValueType.CALLRUNTIMEXCEPTION);
		}
	}


}
