package org.bundlebee.test.carrier;

import org.bundlebee.carrier.Carrier;
import org.bundlebee.carrier.DeploymentException;
import org.ops4j.pax.drone.api.DroneConnector;
import static org.ops4j.pax.drone.connector.paxrunner.GenericConnector.*;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.DroneTestCase;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceReference;


public class CarrierTest extends DroneTestCase {
    private static final String SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME = "org.bundlebee.testbundle";

    public DroneConnector configure() {
        return create(createRunnerContext(), createBundleProvision()
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-service")
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-api")
                .addBundle("mvn:org.slf4j/com.springsource.slf4j.api/1.5.0")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.core/0.9.9")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.classic/0.9.9")
                .addBundle("mvn:org.bundlebee/org.bundlebee.carrier")
                .addBundle("mvn:org.bundlebee/org.bundlebee.repository")
                //.addBundle("mvn:org.bundlebee/logging")
                .addBundle("mvn:org.apache.felix/org.apache.felix.bundlerepository/1.2.1")
                .addBundle("mvn:javax.servlet/servlet/2.4.0.v200806031604")
        ).setPlatform(Platforms.EQUINOX)
                .addVMOption("-Dorg.bundlebee.repository.urls="
                + "file://" + System.getProperty("user.home") + "/.m2/repository/repository.xml")
        ;
    }


    public void testDeploy() throws DeploymentException {
        final ServiceReference carrierRef = bundleContext.getServiceReference(Carrier.class.getName());
        final Carrier carrier = (Carrier)bundleContext.getService(carrierRef);
        carrier.deploy("(symbolicname=" + SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME + ")", false);
        for (final Bundle bundle: bundleContext.getBundles()) {
            if (SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                assertTrue("Bundle is not resolved or installed.", bundle.getState() == Bundle.INSTALLED || bundle.getState() == Bundle.RESOLVED);
                return;
            }
        }
        fail("Bundle was not deployed.");
    }

    public void testDeployAndStart() throws DeploymentException {
        final ServiceReference carrierRef = bundleContext.getServiceReference(Carrier.class.getName());
        final Carrier carrier = (Carrier)bundleContext.getService(carrierRef);
        carrier.deploy("(symbolicname=" + SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME + ")", true);
        for (final Bundle bundle: bundleContext.getBundles()) {
            if (SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(bundle.getSymbolicName())) {
                assertEquals("Bundle is not started", Bundle.ACTIVE, bundle.getState());
                return;
            }
        }
        fail("Bundle was not deployed.");
    }
}
