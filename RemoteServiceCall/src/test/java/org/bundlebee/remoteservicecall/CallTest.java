/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bundlebee.remoteservicecall;

import org.bundlebee.remoteservicecall.Call;
import org.bundlebee.remoteservicecall.CallRuntimeException;
import org.bundlebee.remoteservicecall.ObjectInputStreamWithServiceClassLoader;
import org.bundlebee.remoteservicecall.Caller;
import org.bundlebee.remoteservicecall.ServiceNotAvailableException;
import org.bundlebee.remoteservicecall.CallDescriptor;
import org.bundlebee.remoteservicecall.Callee;
import org.bundlebee.remoteservicecall.RemotingException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Dictionary;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import static org.junit.Assert.*;
import org.osgi.framework.BundleException;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Filter;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 *
 * @author joergp
 */
public class CallTest
{

	public CallTest()
	{
	}

	@BeforeClass
	public static void setUpClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass() throws Exception
	{
	}

	@Before
	public void setUp()
	{
	}

	@After
	public void tearDown()
	{
	}

	class BundleCtxMock implements BundleContext
	{

		private final Object mService;

		public BundleCtxMock(Object service)
		{
			mService = service;
		}

		public String getProperty(String string)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Bundle getBundle()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Bundle installBundle(String string) throws BundleException
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Bundle installBundle(String string, InputStream in) throws BundleException
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Bundle getBundle(long l)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Bundle[] getBundles()
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void addServiceListener(ServiceListener sl, String string) throws InvalidSyntaxException
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void addServiceListener(ServiceListener sl)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void removeServiceListener(ServiceListener sl)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void addBundleListener(BundleListener bl)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void removeBundleListener(BundleListener bl)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void addFrameworkListener(FrameworkListener fl)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public void removeFrameworkListener(FrameworkListener fl)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public ServiceRegistration registerService(String[] strings, Object o, Dictionary dctnr)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public ServiceRegistration registerService(String string, Object o, Dictionary dctnr)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public ServiceReference[] getServiceReferences(String string, String string1) throws InvalidSyntaxException
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public ServiceReference[] getAllServiceReferences(final String clazz, String filter) throws InvalidSyntaxException
		{
			ServiceReference ref = new ServiceReference()
			{

				public Object getProperty(String string)
				{
					return clazz;
				}

				public String[] getPropertyKeys()
				{
					throw new UnsupportedOperationException("Not supported yet.");
				}

				public Bundle getBundle()
				{
					throw new UnsupportedOperationException("Not supported yet.");
				}

				public Bundle[] getUsingBundles()
				{
					throw new UnsupportedOperationException("Not supported yet.");
				}

				public boolean isAssignableTo(Bundle bundle, String string)
				{
					throw new UnsupportedOperationException("Not supported yet.");
				}

				public int compareTo(Object o)
				{
					throw new UnsupportedOperationException("Not supported yet.");
				}
			};
			return mService.getClass().getName().equals( clazz ) ? new ServiceReference[] {	ref } : null;
		}

		public ServiceReference getServiceReference(String string)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Object getService(ServiceReference sr)
		{
			return mService.getClass().getName().equals( sr.getProperty(null)) ? mService : null;
		}

		public boolean ungetService(ServiceReference sr)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public File getDataFile(String string)
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}

		public Filter createFilter(String string) throws InvalidSyntaxException
		{
			throw new UnsupportedOperationException("Not supported yet.");
		}
	}


	static class WorkerException extends Exception {

		public WorkerException() {
		}

	}

	static class Worker implements Serializable {
		public void doit() {
			System.out.println("doit");
		}

		public Worker myself() {
			return this;
		}

		public void throwEx() throws WorkerException {
			throw new WorkerException();
		}
	}

	static class Worker2 implements Serializable {
		public void doit() {
			System.out.println("doit");
		}

		public Worker2 myself() {
			return this;
		}
	}

	/**
	 * Test of readExternal method, of class Call.
	 */
	@Test
	public void testExternalize() throws Exception
	{
		PipedOutputStream os = new PipedOutputStream();
		PipedInputStream is = new PipedInputStream(os);

		BundleCtxMock ctx = new BundleCtxMock( new Worker() );

		ObjectOutputStream oos = new ObjectOutputStream(os);
		CallDescriptor desc = new CallDescriptor(Worker.class.getName(), null, "myself", new Class[0] );
		Call clocal = new Call(1L, 2L, desc, null);
		oos.writeObject(clocal);

		ObjectInputStreamWithServiceClassLoader ois = new ObjectInputStreamWithServiceClassLoader(is, ctx);
		Call cremote = (Call) ois.readObject();

		Object r = cremote.execute();
		assertEquals(Worker.class, r.getClass());
	}


	/**
	 * perform a call and return either result or any exception that occurred
	 * @param c
	 * @return
	 */
	Object doCall( Call c ) {
		try
		{
			PipedOutputStream oslocal = new PipedOutputStream();
			PipedInputStream isremote = new PipedInputStream(oslocal,1000000);
			PipedOutputStream osremote = new PipedOutputStream();
			PipedInputStream islocal = new PipedInputStream(osremote,1000000);
			BundleCtxMock ctx = new BundleCtxMock( new Worker() );

			Caller.sendCall(c, oslocal);
			Callee.executeCall(isremote, osremote, ctx);
			Object result = Caller.readResult(islocal,null);
			return result;
		}
		catch (Exception ex) {
			return ex;
		}
	}

	
	@Test
	public void testCallerCallee() throws IOException {

		CallDescriptor desc = new CallDescriptor(Worker.class.getName(), null, "myself", new Class[0] );
		Call c = new Call(1L, 2L, desc, null);

		//
		// do the plain call
		//
		Object r = doCall( c );
		assertEquals(Worker.class, r.getClass());

		//
		// provide an argument that does not match
		// this is the case if code is not properly generated, so it leads to an runtime exception!
		//
		c = new Call(1L, 2L, desc, new String[]{"foo"});
		r = doCall( c );
		assertEquals(CallRuntimeException.class, r.getClass());
		assertEquals(IllegalArgumentException.class, ((CallRuntimeException)r).getCause().getClass());

		//
		// call a method that does not exist
		//
		desc = new CallDescriptor(Worker.class.getName(), null, "dumdidum", new Class[0] );
		c = new Call(1L, 2L, desc, new String[]{"foo"});
		r = doCall( c );
		assertEquals(CallRuntimeException.class, r.getClass());
		assertEquals(NoSuchMethodException.class, ((CallRuntimeException)r).getCause().getClass());

		//
		// call a method that throws an exception
		//
		desc = new CallDescriptor(Worker.class.getName(), null, "throwEx", new Class[0] );
		c = new Call(1L, 2L, desc, null);
		r = doCall( c );
		assertEquals(WorkerException.class, r.getClass());

		//
		// calling without an object being registered
		//
		desc = new CallDescriptor(Worker2.class.getName(), null, "myself", new Class[0] );
		c = new Call(1L, 2L, desc, null);
		r = doCall( c );
		assertEquals(RemotingException.class, r.getClass());
		assertEquals(ServiceNotAvailableException.class, ((RemotingException)r).getCause().getClass());
	}
}
