package org.bundlebee.test.repository;

import org.bundlebee.repository.Repository;
import org.ops4j.pax.drone.api.DroneConnector;
import static org.ops4j.pax.drone.connector.paxrunner.GenericConnector.*;
import org.ops4j.pax.drone.connector.paxrunner.Platforms;
import org.ops4j.pax.drone.spi.junit.DroneTestCase;
import org.osgi.framework.ServiceReference;
import org.osgi.service.obr.RepositoryAdmin;
import org.osgi.service.obr.Resource;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;

/**
 * Repository Test.
 * <p/>
 * Date: Jan 14, 2009
 * Time: 4:34:02 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class RepositoryTest extends DroneTestCase {

    private static final String SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME = "org.bundlebee.testbundle";
    private static final int HTTP_PORT = 0xbbee; // that is 48110

    public DroneConnector configure() {
        return create(createRunnerContext(), createBundleProvision()
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-service")
                //.addBundle("mvn:org.ops4j.pax.logging/pax-logging-api")
                .addBundle("mvn:org.slf4j/com.springsource.slf4j.api/1.5.0")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.core/0.9.9")
                .addBundle("mvn:ch.qos.logback/com.springsource.ch.qos.logback.classic/0.9.9")
                //.addBundle("mvn:org.bundlebee/org.bundlebee.carrier")
                .addBundle("mvn:org.bundlebee/org.bundlebee.repository")
                .addBundle("mvn:org.apache.felix/org.apache.felix.bundlerepository/1.2.1")
                .addBundle("mvn:org.eclipse.equinox/org.eclipse.equinox.http/1.0.200.v20080421-2006")
                .addBundle("mvn:javax.servlet/servlet/2.4.0.v200806031604")
        ).setPlatform(Platforms.EQUINOX)
                .addVMOption("-Dorg.bundlebee.repository.root=testrepo" + System.currentTimeMillis())
                .addVMOption("-Dorg.osgi.service.http.port=" + HTTP_PORT)
        ;
    }

    public void testInstallFromURL() throws Exception {
        final ServiceReference repositoryRef = bundleContext.getServiceReference(Repository.class.getName());
        final Repository repository = (Repository)bundleContext.getService(repositoryRef);

        final URL repositoryURL = repository.getRepositoryURL();
        assertNotNull("repo url was null", repositoryURL);

        final ServiceReference repositoryAdminRef = bundleContext.getServiceReference(RepositoryAdmin.class.getName());
        final RepositoryAdmin repositoryAdmin = (RepositoryAdmin)bundleContext.getService(repositoryAdminRef);
        repositoryAdmin.addRepository(repositoryURL);

        final String testBundleFilterExpr = "(symbolicname=" + SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME + ")";
        final Resource[] preResources = repositoryAdmin.discoverResources(testBundleFilterExpr);
        assertTrue("expected to not find testbundle", preResources == null || preResources.length == 0);

        final File testbundle = getATestBundleFile();
        if (testbundle == null) fail("Couldn't find TestBundle jar in local maven repo");
        repository.install(testbundle.toURL(), SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME + ".jar");

        // refresh repo
        repositoryAdmin.addRepository(repositoryURL);
        final Resource[] postResources = repositoryAdmin.discoverResources(testBundleFilterExpr);
        assertNotNull("expected to find at least one resource", postResources);
        assertTrue("expected to find at least one resource", postResources.length > 0);
        assertTrue("expected to find testbundle", SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(postResources[0].getSymbolicName()));
    }

    public void testInstallFromFile() throws Exception {
        final ServiceReference repositoryRef = bundleContext.getServiceReference(Repository.class.getName());
        final Repository repository = (Repository)bundleContext.getService(repositoryRef);

        final URL repositoryURL = repository.getRepositoryURL();
        assertNotNull("repo url was null", repositoryURL);

        final ServiceReference repositoryAdminRef = bundleContext.getServiceReference(RepositoryAdmin.class.getName());
        final RepositoryAdmin repositoryAdmin = (RepositoryAdmin)bundleContext.getService(repositoryAdminRef);
        repositoryAdmin.addRepository(repositoryURL);

        final String testBundleFilterExpr = "(symbolicname=" + SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME + ")";
        final Resource[] preResources = repositoryAdmin.discoverResources(testBundleFilterExpr);
        assertTrue("expected to not find testbundle", preResources == null || preResources.length == 0);

        final File testbundle = getATestBundleFile();
        if (testbundle == null) fail("Couldn't find TestBundle jar in local maven repo");
        repository.install(testbundle);

        // refresh repo
        repositoryAdmin.addRepository(repositoryURL);
        final Resource[] postResources = repositoryAdmin.discoverResources(testBundleFilterExpr);
        assertNotNull("expected to find at least one resource", postResources);
        assertTrue("expected to find at least one resource", postResources.length > 0);
        assertTrue("expected to find testbundle", SIMPLE_TEST_BUNDLE_SYMBOLIC_NAME.equals(postResources[0].getSymbolicName()));
    }

    private static File getATestBundleFile() {
        // somehow there's got to be a better way for this... -hendrik
        final File versionDir = new File(System.getProperty("user.home") + "/.m2/repository/org/bundlebee/org.bundlebee.testbundle");
        File testbundle = null;
        final File[] subDirs = versionDir.listFiles(new FileFilter() {
            public boolean accept(final File pathname) {
                return pathname.isDirectory();
            }
        });
        if (subDirs.length != 0) {
            final File[] jars = subDirs[0].listFiles(new FileFilter() {
                public boolean accept(final File pathname) {
                    return pathname.toString().endsWith(".jar");
                }
            });
            if (jars.length != 0) {
                testbundle = jars[0];
            }
        }
        return testbundle;
    }

}
