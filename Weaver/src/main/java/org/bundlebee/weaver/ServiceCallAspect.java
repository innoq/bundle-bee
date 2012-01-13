package org.bundlebee.weaver;

import org.bundlebee.registry.Registry;
import org.bundlebee.registry.impl.RegistryImpl;
import org.bundlebee.remoteservicecall.BundleLifecycleClient;
import org.bundlebee.remoteservicecall.RemotingException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.*;
import java.util.*;
import org.bundlebee.remoteservicecall.Call;
import org.bundlebee.remoteservicecall.StaticCallContext;
import org.bundlebee.remoteservicecall.CallDescriptor;
import org.bundlebee.remoteservicecall.Caller;

/**
 *
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @author <a href="mailto:joerg.plewe@innoq.com">Jörg Plewe</a>
 */
public class ServiceCallAspect {

    private final static Logger LOG = LoggerFactory.getLogger(ServiceCallAspect.class);
    private final static ServiceCallStats serviceCallStats = new ServiceCallStats();
    private final static Registry registry;
    private final static ServiceCallDispatchStrategy serviceCallDispatchStrategy;
    public final static URI LOCAL_URI;

	//
	// static class setup
	//
    static {

		//
		// determine 'local URI'
		//
        URI uri = null;
        try {
            uri = new URI("vm://" + RegistryImpl.getInstance().getNodeId() + "/");
        } catch (URISyntaxException e) {
            LOG.error(e.toString(), e);
			//
			// exception must not occur!
			//
			throw new RuntimeException(e);
        }
        LOCAL_URI = uri;

		// get hold of registry
        registry = RegistryImpl.getInstance();

		// create a registry
		serviceCallDispatchStrategy = DispatchStrategyFactory.create(registry, serviceCallStats);
    }


	/**
	 * ctor
	 */
    private ServiceCallAspect() {
    }


	/**
	 * retrieve statistics
	 * @return ServiceCallStats
	 */
    public static ServiceCallStats getServiceCallStats() {
        return serviceCallStats;
    }

	
    private static void unregisterManager( URI uri,  Exception e) {
        try {
            registry.unregisterManager(uri.toURL());
            LOG.info("Manager at " + uri + " has been blacklisted/unregistered because of a " + e.toString());
        } catch (MalformedURLException e1) {
            LOG.error(e1.toString(), e1);
        }
    }


    private static void logLocalCall(final Object service, final String methodName, final Class[] parameterTypes, final long duration) {
        if (LOG.isDebugEnabled()) LOG.debug("Local call: " + duration + " nano seconds");
        serviceCallStats.logLocalCall(service,  methodName, parameterTypes, duration);
    }

    private static void logRemoteCall(final URI uri, final Object service, final String methodName, final Class[] parameterTypes, final long duration) {
        if (LOG.isDebugEnabled()) LOG.debug("Remote call to " + uri +": " + duration + " nano seconds");
        serviceCallStats.logCall(uri, service,  methodName, parameterTypes, duration);
    }

    private static String getServiceClassName(final Object localService) {
		return localService.getClass().getName();
    }

    private static String getServiceFilter(final Object localService, final String methodname, final Class[] parameterTypes) {
        final List<String> possibleObjectClassNames = new ArrayList<String>();
        possibleObjectClassNames.add(localService.getClass().getName());
        for (final Class iface:localService.getClass().getInterfaces()) {
            try {
                iface.getMethod(methodname, parameterTypes);
                possibleObjectClassNames.add(iface.getName());
            } catch (NoSuchMethodException e) {
                // ignore on purpose
            }
        }
        final String serviceFilter;
        if (possibleObjectClassNames.size() <= 1) {
            serviceFilter = null;
        } else {
            final StringBuilder sb = new StringBuilder();
            sb.append("(|");
            for (final String objectClassName:possibleObjectClassNames) {
                sb.append('(');
                sb.append(Constants.OBJECTCLASS);
                sb.append('=');
                sb.append(objectClassName);
                sb.append(')');
            }
            sb.append(')');
            serviceFilter = sb.toString();
        }
        return serviceFilter;
    }


	/** Tagging object describing that a remote call could not happen for some reason. */
	public final static Object CANNOT_EXECUTE_REMOTELY = new Object();
	
	/**
	 * Try to do a remote call.
	 * 
	 * @param service
	 * @param methodname
	 * @param bundleSymbolicNameVersion
	 * @param paramtypes
	 * @param args
	 * @return remote result (can be null) or CANNOT_EXECUTE_REMOTELY
	 */
	public static Object tryRemote( Object service, String methodname, String bundleSymbolicNameVersion, Class[] paramtypes, Object[] args ) throws Exception  {

		// if the static context says I may not go remote, I don't
		if (StaticCallContext.isForceLocal()) {

			// clear the flag so that subsequent calls in this thread can be remoted again
			StaticCallContext.clearForceLocal();

			// remember when the local call is going to start
			StaticCallContext.setTimeStamp();

			return CANNOT_EXECUTE_REMOTELY;
		}


		// timestamp to measure remote execution
		long remoteStartTime = System.nanoTime();


		//
		// get a service filter and classname. The latter is only used if no filter is set
		//
		String serviceFilter = getServiceFilter(service, methodname, paramtypes);
		String serviceClassName = (null == serviceFilter) ? getServiceClassName(service) : null;

		URI mgrURI = null;
		URL serviceURL = null;
		try
		{
			//
			// retrieve the URL where to find the service
			//
			mgrURI = serviceCallDispatchStrategy.getManagerURI(bundleSymbolicNameVersion, service, methodname, paramtypes);
			serviceURL = new URL(BundleLifecycleClient.toDirectoryURL(mgrURI.toURL()), "service");
			if (LOG.isDebugEnabled())
			{
				LOG.debug("Service URL: " + serviceURL);
			}
		}
		catch (MalformedURLException ex) {
			// shouldn't happen
			throw new RuntimeException(ex);
		}


		try
		{
			CallDescriptor desc = new CallDescriptor(serviceClassName, serviceFilter, methodname, paramtypes);

			Call c = new Call(RegistryImpl.getInstance().getNodeId(), System.identityHashCode(service), desc, args);

			Object ret = Caller.executeCall(c, serviceURL, service.getClass().getClassLoader());

			// update statistics
			logRemoteCall( mgrURI, service, methodname, paramtypes, System.nanoTime()-remoteStartTime );

			return ret;
		}
		catch (RemotingException ex) {

			//
			// @todo something went wrong on remoting, have to react?
			//
			unregisterManager(mgrURI, ex);
		}
		catch (Exception ex) {
			//
			// ex either is:
			// * a RemoteTargetException thrown intentionally by the worker code
			// * a RuntimeException caused by BundleBee errors
			//
			throw ex;
		}

		// remember when the local call is going to start
		StaticCallContext.setTimeStamp();

		return CANNOT_EXECUTE_REMOTELY;
	}


	/**
	 * Call this at the end of a local call. Depends on the fact that {@link tryRemote(Object,String,String,Class[],Object[])} has been called before.
	 * 
	 * @param service
	 * @param methodname
	 * @param paramtypes
	 */
	public static void finishLocal( Object service, String methodname, Class[] paramtypes ) {
		logLocalCall( service, methodname, paramtypes, System.nanoTime()-StaticCallContext.getTimeStamp() );
	}
}
