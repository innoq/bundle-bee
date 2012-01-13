
package org.bundlebee.remoteservicecall;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/**
 *
 * @author joergp
 */
public class Call implements Externalizable {

	// @todo put callingServiceInstanceId and callingNodeId to CallDescriptor as well?
    private long				callingServiceInstanceId;
    private long				callingNodeId;

	private CallDescriptor		mCallDesc;
	private Object[]			parameters;

	transient private Object					mService;
	transient private InvalidSyntaxException	mInvSyntaxEx;

	
	/**
	 * Ctor for deserialization
	 */
	public Call()
	{
	}


	/**
	 * ctor
	 * @param srvinstid
	 * @param nodeid
	 * @param calldesc
	 * @param params
	 */
	public Call( long srvinstid, long nodeid, CallDescriptor calldesc, Object[] params ) {
		callingServiceInstanceId	= srvinstid;
		callingNodeId				= nodeid;
		mCallDesc					= calldesc;
		parameters					= params;
	}


	/**
	 * implement Externalizable
	 * @param oo
	 * @throws IOException
	 */
	public void writeExternal(ObjectOutput oo) throws IOException
	{
        oo.writeLong(callingServiceInstanceId);
        oo.writeLong(callingNodeId);
        oo.writeObject(mCallDesc);
        oo.writeObject(parameters);

	}

	/**
	 * implement Externalizable
	 *
	 * @param oi
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public void readExternal(ObjectInput oi) throws IOException, ClassNotFoundException
	{
		assert oi instanceof ObjectInputStreamWithServiceClassLoader;

        callingServiceInstanceId	= oi.readLong();
        callingNodeId				= oi.readLong();
		mCallDesc					= (CallDescriptor) oi.readObject();

		try	{
			mInvSyntaxEx = null;

			resolveService((ObjectInputStreamWithServiceClassLoader) oi);

			// if service couldn't be found, this may cause a ClassNotFoundException
			parameters				= (Object[])oi.readObject();
		}
		catch (InvalidSyntaxException ex) {

			// remember that exception and deliver it on execute()
			mInvSyntaxEx = ex;
		}


	}


	/**
	 * get the service and configure the ObjectInputStream accordingly
	 * @param oi
	 * @throws InvalidSyntaxException
	 */
	private void resolveService( ObjectInputStreamWithServiceClassLoader oi ) throws InvalidSyntaxException
	{
		ServiceReference[] refs = oi.getBundleContext().getAllServiceReferences( mCallDesc.ServiceClassName, mCallDesc.ServiceFilter);

		if( null != refs &&  0 != refs.length ) {

	//		refs[0].getBundle().getVersion();

			// @todo find right service from version
			mService = oi.getBundleContext().getService(refs[0]);

			// reconfigure the inputStream to use the correct classloader
			// see http://bundle-bee.org/issues/show/19
			oi.useClassLoader(mService.getClass().getClassLoader());
		}
	}

	public Object execute() throws InvocationTargetException, ServiceNotAvailableException, CallRuntimeException {

		//
		// if there was an issue during deserialization, throw it
		//
		if( null != mInvSyntaxEx )
			throw new ServiceNotAvailableException(mInvSyntaxEx);

		if( null == mService ) {
			throw new ServiceNotAvailableException(ServiceNotAvailableException.SERVICE_NOT_REGISTERED);
		}

		try
		{
			Class<?> serviceClass = mService.getClass();
			Class<?>[] parameterTypes = loadParameterClasses(serviceClass.getClassLoader(), mCallDesc.ParameterTypeNames);
			Method method = serviceClass.getMethod( mCallDesc.MethodName, parameterTypes);

			//
			// *very* strange
			// While writing a 'getting started' tutorial, I could not bring a simple 'hello world' to work while far more complex
			// code works flawlessly the same time. Always got a IllegalAccessException on the remote side.
			// Following http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4533479, I used setAccessible(true) which solves the issue.
			//
			// java.lang.IllegalAccessException: Class org.bundlebee.manager.impl.ManagerImpl can not access a member of class org.bundlebee.examples.hello.Hello with modifiers "public"
			// @todo find out why method might not be accessible, see ticket http://bundle-bee.org/issues/show/28
			//
			method.setAccessible(true);

			// do the actual call
			Object res = method.invoke(mService, parameters);

			return res;

		}
		catch (NoSuchMethodException ex) {
			// if we did find the service but it now does not have the method, something in BundleBee must be wrong
			throw new CallRuntimeException(ex);
		} 
		catch (SecurityException ex) {
			throw new ServiceNotAvailableException(ex);
		} 
		catch (IllegalAccessException ex) {
			throw new ServiceNotAvailableException(ex);
		} 
		catch (IllegalArgumentException ex) {
			// service is found for method/parameter names but the actual argument do not match -> something is wrong with BundleBee
			throw new CallRuntimeException(ex);
		} 
		catch (ClassNotFoundException ex) {
			throw new ServiceNotAvailableException(ex);
		}
	}


	//
	// map primitive type names to corresponding Class objects
	// http://bundle-bee.org/issues/show/20
	//
	private final static Map<String,Class> PRIMITIVETYPES = new HashMap<String,Class>() {{
		put( "int",		Integer.TYPE );
		put( "short",	Short.TYPE );
		put( "byte",	Byte.TYPE );
		put( "long",	Long.TYPE );
		put( "char",	Character.TYPE );
		put( "float",	Float.TYPE );
		put( "double",	Double.TYPE );
		put( "boolean",	Boolean.TYPE );
	}};


	/**
	 * Given the names of the arguments classes, load their resp. Class'es
	 */
    private Class<?>[] loadParameterClasses(final ClassLoader classLoader, final String[] classNames) throws ClassNotFoundException {
        final Class<?>[] parameterTypes = new Class<?>[classNames.length];
        for (int i=0; i< classNames.length; i++) {

			// try to resolve a primitive type first
			// see http://bundle-bee.org/issues/show/20
			parameterTypes[i] = PRIMITIVETYPES.get(classNames[i]);

			// if it failed, load the class from its name
			if( null == parameterTypes[i] )
				parameterTypes[i] = classLoader.loadClass(classNames[i]);
        }
        return parameterTypes;
    }

}
