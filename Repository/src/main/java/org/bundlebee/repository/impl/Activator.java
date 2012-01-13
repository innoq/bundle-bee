package org.bundlebee.repository.impl;

import org.bundlebee.repository.Repository;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Properties;

/**
 * Activates the repository. I.e. registers the repository file tree with the HttpService, so that it can be served
 * over HTTP. By default the maven2 repository is served, so that if the maven2 repository doubles as a Felix
 * bundle repository, the Felix OBR repository.xml file is served under
 * <code>http://servername:port/bundlebee/repository/repository.xml</code>
 * </p>
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Activator implements BundleActivator {

    private static Logger LOG = LoggerFactory.getLogger(Activator.class);
    public static final String ID = "org.bundlebee.repository";
    public static final Marker BUNDLE_MARKER = createBundleMarker();
    private static Marker createBundleMarker() {
        Marker bundleMarker = MarkerFactory.getMarker(ID);
        bundleMarker.add(MarkerFactory.getMarker("IS_MARKER"));
        return bundleMarker;
    }
    private RepositoryImpl repository;

    public void start(final BundleContext context) throws Exception {
        this.repository = new RepositoryImpl(context);
        if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "Starting BundleBee repository...");
        final Properties properties = new Properties();
        context.registerService(Repository.class.getName(), this.repository, properties);
    }

    public void stop(final BundleContext context) throws Exception {
        this.repository.stop();
        if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "Stopping BundleBee repository...");
    }
}
