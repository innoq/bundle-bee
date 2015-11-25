/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bundlebee.remoteservicecall;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

/**
 *
 * @author joergp
 */
public class CallTest {

    private BundleContext ctx;

    public CallTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
        ctx = FrameworkUtil.getBundle(this.getClass()).getBundleContext();
    }

    @After
    public void tearDown() {
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
    // TODO fixme
    //@Test
    public void testExternalize() throws Exception {
        PipedOutputStream os = new PipedOutputStream();
        PipedInputStream is = new PipedInputStream(os);

        ObjectOutputStream oos = new ObjectOutputStream(os);
        CallDescriptor desc = new CallDescriptor(Worker.class.getName(), null, "myself", new Class[0]);
        Call clocal = new Call(1L, 2L, desc, null);
        oos.writeObject(clocal);

        ObjectInputStreamWithServiceClassLoader ois = new ObjectInputStreamWithServiceClassLoader(is, ctx);
        Call cremote = (Call) ois.readObject();

        Object r = cremote.execute();
        assertEquals(Worker.class, r.getClass());
    }

    /**
     * perform a call and return either result or any exception that occurred
     *
     * @param c
     * @return
     */
    Object doCall(Call c) {
        try {
            PipedOutputStream oslocal = new PipedOutputStream();
            PipedInputStream isremote = new PipedInputStream(oslocal, 1000000);
            PipedOutputStream osremote = new PipedOutputStream();
            PipedInputStream islocal = new PipedInputStream(osremote, 1000000);

            Caller.sendCall(c, oslocal);
            Callee.executeCall(isremote, osremote, ctx);
            Object result = Caller.readResult(islocal, null);
            return result;
        } catch (Exception ex) {
            return ex;
        }
    }

    // TODO fixme
    //@Test
    public void testCallerCallee() throws IOException {

        CallDescriptor desc = new CallDescriptor(Worker.class.getName(), null, "myself", new Class[0]);
        Call c = new Call(1L, 2L, desc, null);

        //
        // do the plain call
        //
        Object r = doCall(c);
        assertEquals(Worker.class, r.getClass());

        //
        // provide an argument that does not match
        // this is the case if code is not properly generated, so it leads to an runtime exception!
        //
        c = new Call(1L, 2L, desc, new String[]{"foo"});
        r = doCall(c);
        assertEquals(CallRuntimeException.class, r.getClass());
        assertEquals(IllegalArgumentException.class, ((CallRuntimeException) r).getCause().getClass());

        //
        // call a method that does not exist
        //
        desc = new CallDescriptor(Worker.class.getName(), null, "dumdidum", new Class[0]);
        c = new Call(1L, 2L, desc, new String[]{"foo"});
        r = doCall(c);
        assertEquals(CallRuntimeException.class, r.getClass());
        assertEquals(NoSuchMethodException.class, ((CallRuntimeException) r).getCause().getClass());

        //
        // call a method that throws an exception
        //
        desc = new CallDescriptor(Worker.class.getName(), null, "throwEx", new Class[0]);
        c = new Call(1L, 2L, desc, null);
        r = doCall(c);
        assertEquals(WorkerException.class, r.getClass());

        //
        // calling without an object being registered
        //
        desc = new CallDescriptor(Worker2.class.getName(), null, "myself", new Class[0]);
        c = new Call(1L, 2L, desc, null);
        r = doCall(c);
        assertEquals(RemotingException.class, r.getClass());
        assertEquals(ServiceNotAvailableException.class, ((RemotingException) r).getCause().getClass());
    }
}
