
package org.bundlebee.remoteservicecall;

/**
 * Thrown whenever an exception occurs that is caused by an error in the BundleBee code.
 * E.g. when paramtertypenames and paramters do not match, which denotes an error in the weaved code.
 * Such an exception should trigger an runtime exception on the calling side while it is treated as an
 * declared exception in the called side.
 * 
 * @author joergp
 */
public class CallRuntimeException extends RuntimeException {

	public CallRuntimeException( Throwable t ) {
		super( t );
	}


}
