package org.bundlebee.remoteservicecall;


/**
 * Thrown when a service cannot be executed remotely for a given reason.
 */
public class ServiceNotAvailableException extends Exception {

//    static final long serialVersionUID = 42L;

	public final static String SERVICE_NOT_REGISTERED = "service not registered";

	public ServiceNotAvailableException( Exception reason ) {
		super(reason);
	}

	public ServiceNotAvailableException( String reason ) {
		super(reason);
	}
  
}
