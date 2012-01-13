package org.bundlebee.test.manager;

import org.bundlebee.remoteservicecall.Call;
import org.bundlebee.remoteservicecall.ServiceNotFoundException;
import org.ops4j.pax.drone.api.DroneConnector;
import static org.ops4j.pax.drone.connector.paxrunner.GenericConnector.*;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.DroneTestCase;
import org.osgi.framework.InvalidSyntaxException;

import java.io.IOException;
import java.io.NotSerializableException;
import java.io.WriteAbortedException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;


public class ManagerServiceCallTest extends DroneTestCase {

    private static final int HTTP_PORT = 0xbee;
    private static final String SIMPLE_TEST_BUNDLE_CLASS_NAME = "org.bundlebee.testbundle.TestBundle";
    private static final String SERVICE_URL = "http://localhost:" + HTTP_PORT + "/bundlebee/manager/service";
    private static final String SIMPLE_TEST_BUNDLE_FILTER = "(objectClass=org.bundlebee.testbundle.TestBundle)";

    public DroneConnector configure() {
        return create(createRunnerContext(), createBundleProvision()
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-service")
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-api")
                .addBundle("mvn:org.slf4j/com.springsource.slf4j.api/1.5.0")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.core/0.9.9")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.classic/0.9.9")
                .addBundle("mvn:org.bundlebee/org.bundlebee.manager")
                .addBundle("mvn:org.bundlebee/org.bundlebee.carrier")
                .addBundle("mvn:org.bundlebee/org.bundlebee.repository")
                //.addBundle("mvn:org.bundlebee/logging")
                .addBundle("mvn:org.apache.felix/org.apache.felix.bundlerepository/1.2.1")
                .addBundle("mvn:org.eclipse.equinox/org.eclipse.equinox.http/1.0.200.v20080421-2006")
                .addBundle("mvn:javax.servlet/servlet/2.4.0.v200806031604")
                .addBundle("mvn:org.bundlebee/org.bundlebee.testbundle")
                // in case you don't have these bundles in the local maven repository
                //.addBundle("obr:org.eclipse.equinox.http")
                //.addBundle("obr:javax.servlet")
        ).setPlatform(Platforms.EQUINOX)
                .addVMOption("-Dorg.osgi.service.http.port=" + HTTP_PORT);
    }

    public void testEmpty() throws IOException, ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InvalidSyntaxException, ServiceNotFoundException {
        assertTrue("Dummy.", true);
    }

    public void testHelloWorld() throws IOException, ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InvalidSyntaxException, ServiceNotFoundException {
        final URL url = new URL(SERVICE_URL);
        final Object result = new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                "helloWorld", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
        assertNull("Expected null result.", result);
    }

    public void testHasParameters() throws IOException, ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InvalidSyntaxException, ServiceNotFoundException {
        final URL url = new URL(SERVICE_URL);
        final Object result = new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                "hasParameters", new String[]{String.class.getName()}, new Object[]{"parameter"}).send(url, this.getClass().getClassLoader());
        assertNull("Expected null result.", result);
    }

    public void testHasReturnValue() throws IOException, ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InvalidSyntaxException, ServiceNotFoundException {
        final URL url = new URL(SERVICE_URL);
        final Object result = new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                "hasReturnValue", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
        assertEquals("Expected return value.", "A return value.", result);
    }

    public void testNonSerializableParameter() throws ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InvalidSyntaxException, ServiceNotFoundException, IOException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                    SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                    "hasParameters", new String[] {Object.class.getName()}, new Object[]{new Object()}).send(url, this.getClass().getClassLoader());
            fail("Expecteded NotSerializableException");
        } catch (NotSerializableException e) {
            // expected this, as Object is not serializable
        }
    }

    public void testNonSerializableReturnValue() throws ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, InvalidSyntaxException, ServiceNotFoundException, IOException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                    SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                    "hasNonSerializableReturnValue", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
            fail("Expecteded NotSerializableException");
        //} catch (NotSerializableException e) {
        } catch (WriteAbortedException e) {
            assertTrue(e.getCause() instanceof NotSerializableException);
            // expected this, as the returned object is not serializable
        }
    }

    public void testNoSuchMethod() throws IOException, ClassNotFoundException, InvocationTargetException,
            IllegalAccessException, InvalidSyntaxException, ServiceNotFoundException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                    SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                    "someBogusMethodNameThatDoesNotExist", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
            fail("Expecteded NoSuchMethodException");
        } catch (NoSuchMethodException e) {
            // this is what we expected
        }
    }

    public void testServiceNotFound() throws IOException, InvocationTargetException,
            IllegalAccessException, InvalidSyntaxException, NoSuchMethodException, ClassNotFoundException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call("org.bundlebee.testbundle.NonExistingClass",
                    SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                    "helloWorld", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
            fail("Expecteded ServiceNotFoundException");
        } catch (ServiceNotFoundException e) {
            // this is what we expected, because the service cannot exist
        }
    }

    public void testClassNotFound() throws IOException, InvocationTargetException,
            IllegalAccessException, InvalidSyntaxException, NoSuchMethodException, ServiceNotFoundException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                    SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                    "helloWorld", new String[]{"com.nonexisting.Klass"}, new Object[]{"someString"}).send(url, this.getClass().getClassLoader());
            fail("Expecteded ClassNotFoundException");
        } catch (ClassNotFoundException e) {
            // this is what we expected, because one of the parameters could not be found
        }
    }

    public void testInvalidSyntax() throws IOException, InvocationTargetException,
            IllegalAccessException, NoSuchMethodException, ServiceNotFoundException, ClassNotFoundException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                    "%(bogusfilter//)", 0, 0,
                    "helloWorld", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
            fail("Expecteded InvalidSyntaxException");
        } catch (InvalidSyntaxException e) {
            // this is what we expected, because the filter syntax is invalid
        }
    }

    public void testInvocationTargetException() throws IOException, IllegalAccessException, NoSuchMethodException,
            ServiceNotFoundException, ClassNotFoundException, InvalidSyntaxException {
        try {
            final URL url = new URL(SERVICE_URL);
            new Call(SIMPLE_TEST_BUNDLE_CLASS_NAME,
                    SIMPLE_TEST_BUNDLE_FILTER, 0, 0,
                    "throwException", new String[0], new Object[0]).send(url, this.getClass().getClassLoader());
            fail("Expecteded InvocationTargetException");
        } catch (InvocationTargetException e) {
            assertEquals("Target exception's message is not the expected one.",
                    "TestBundle Exception", e.getTargetException().getMessage());
            assertEquals("Target exception's class is not the expected one.",
                    Exception.class, e.getTargetException().getClass());
        }
    }

}