package org.bundlebee.test.manager;

import org.ops4j.pax.drone.api.DroneConnector;
import static org.ops4j.pax.drone.connector.paxrunner.GenericConnector.*;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.DroneTestCase;
import org.osgi.framework.Bundle;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;


public class ManagerLifecycleTest extends DroneTestCase {

    private static final String SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME = "org.bundlebee.testbundle";
    private static final int HTTP_PORT = 0xbbee; // that is 48110

    public DroneConnector configure() {
        return create(createRunnerContext(), createBundleProvision()
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-service")
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-api")
                .addBundle("mvn:org.slf4j/com.springsource.slf4j.api/1.5.0")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.core/0.9.9")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.classic/0.9.9")
                //.addBundle("mvn:org.bundlebee/logging")
                .addBundle("mvn:org.bundlebee/org.bundlebee.manager")
                .addBundle("mvn:org.bundlebee/org.bundlebee.carrier")
                .addBundle("mvn:org.bundlebee/org.bundlebee.repository")
                .addBundle("mvn:org.apache.felix/org.apache.felix.bundlerepository/1.2.1")
                .addBundle("mvn:org.eclipse.equinox/org.eclipse.equinox.http/1.0.200.v20080421-2006")
                .addBundle("mvn:javax.servlet/servlet/2.4.0.v200806031604")
                // in case you don't have these bundles in the local maven repository
                //.addBundle("obr:org.eclipse.equinox.http")
                //.addBundle("obr:javax.servlet")
        ).setPlatform(Platforms.EQUINOX)
                .addVMOption("-Dorg.osgi.service.http.port=" + HTTP_PORT)
                .addVMOption("-Dorg.bundlebee.repository.urls="
                + "file://" + System.getProperty("user.home") + "/.m2/repository/repository.xml")
                ;
    }

    public void testLifecycle() throws IOException {
        install();
        start();
        stop();
        uninstall();
    }

    private void install() throws IOException {
        final int responseCode = getResponseCode("install");
        assertEquals("HTTP request returned non-200 code.", HttpURLConnection.HTTP_OK, responseCode);
        for (final Bundle bundle: bundleContext.getBundles()) {
            if (SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                assertEquals("Bundle is not installed", Bundle.INSTALLED, bundle.getState());
                return;
            }
        }
        fail("Bundle was not deployed.");
    }

    private void start() throws IOException {
        final int responseCode = getResponseCode("start");
        assertEquals("HTTP request returned non-200 code.", HttpURLConnection.HTTP_OK, responseCode);
        for (final Bundle bundle: bundleContext.getBundles()) {
            if (SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                assertEquals("Bundle is not started", Bundle.ACTIVE, bundle.getState());
                return;
            }
        }
        fail("Bundle was not deployed.");
    }

    private void stop() throws IOException {
        final int responseCode = getResponseCode("stop");
        assertEquals("HTTP request returned non-200 code.", HttpURLConnection.HTTP_OK, responseCode);
        for (final Bundle bundle: bundleContext.getBundles()) {
            if (SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                assertEquals("Bundle is not stopped", Bundle.RESOLVED, bundle.getState());
                return;
            }
        }
        fail("Bundle is not deployed.");
    }

    private void uninstall() throws IOException {
        final int responseCode = getResponseCode("uninstall");
        assertEquals("HTTP request returned non-200 code.", HttpURLConnection.HTTP_OK, responseCode);
        for (final Bundle bundle: bundleContext.getBundles()) {
            if (SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                fail("Bundle was not undeployed.");
            }
        }
    }

    private int getResponseCode(final String method) throws IOException {
        final URL url = new URL("http://localhost:" + HTTP_PORT
                + "/bundlebee/manager/" + method + "/" + SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME + "/0.5.2.SNAPSHOT");
        final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setRequestMethod("POST");
        return urlConnection.getResponseCode();
    }
}
