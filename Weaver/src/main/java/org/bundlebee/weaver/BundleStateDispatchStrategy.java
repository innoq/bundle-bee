package org.bundlebee.weaver;

import org.bundlebee.registry.Registry;
import org.bundlebee.remoteservicecall.BundleLifecycleClient;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Strategy for finding Manager URIs that prioritizes Managers that have
 * the bundle already ACTIVE. If none are found the bundle is activated and
 * if necessary also deployed and installed on a Manager.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class BundleStateDispatchStrategy implements ServiceCallDispatchStrategy {

    private static Logger LOG = LoggerFactory.getLogger(BundleStateDispatchStrategy.class);
    private Registry registry;
    private BundleLifecycleClient bundleLifecycleClient = new BundleLifecycleClient();

    public void setRegistry(final Registry registry) {
        this.registry = registry;
    }

    public void setServiceCallStats(final ServiceCallStats serviceCallStats) {
    }

    public URI getManagerURI(final String bundleSymbolicNameVersion, final Object service, final String methodname,
                             final Class[] parameterTypes) {
        final List<URL> managerURLs = getManagersWithActiveBundle(bundleSymbolicNameVersion);
        if (managerURLs.isEmpty()) {
            final List<URL> managersWithInstalledBundles = getManagersWithInstalledBundle(bundleSymbolicNameVersion);
            if (!managersWithInstalledBundles.isEmpty()) {
                managerURLs.addAll(bundleLifecycleClient.startBundle(managersWithInstalledBundles, bundleSymbolicNameVersion));
            }
        }
        if (managerURLs.isEmpty()) {
            final List<URL> managersWithDeployedBundles = getManagersWithDeployedBundle(bundleSymbolicNameVersion);
            if (!managersWithDeployedBundles.isEmpty()) {
                managerURLs.addAll(bundleLifecycleClient.installAndStartBundle(managersWithDeployedBundles, bundleSymbolicNameVersion));
            }
        }
        if (managerURLs.isEmpty()) {
            final Set<URL> allManagers = registry.getGrid().getManagers();
            if (!allManagers.isEmpty()) {
                managerURLs.addAll(bundleLifecycleClient.installAndStartBundle(allManagers, bundleSymbolicNameVersion));
            }
        }
        if (!managerURLs.isEmpty()) {
            try {
                // TODO: randomize this?
                return managerURLs.get(0).toURI();
            } catch (URISyntaxException e) {
                LOG.error(e.toString(), e);
            }
        }
        return null;
    }

    /**
     * Returns a list of manager URLs that have the bundle RESOLVED or INSTALLED.
     *
     * @param bundleSymbolicNameVersion symbolic bundlename / version
     * @return list of manager URLs that have the bundle RESOLVED or INSTALLED
     */
    private List<URL> getManagersWithInstalledBundle(final String bundleSymbolicNameVersion) {
        final List<URL> managerURLs = new ArrayList<URL>();
        managerURLs.addAll(registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.RESOLVED));
        managerURLs.addAll(registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.INSTALLED));
        return managerURLs;
    }

    /**
     * Returns a list of manager URLs that have an ACTIVE version of the bundle.
     *
     * @param bundleSymbolicNameVersion symbolic bundlename / version
     * @return list of manager URLs that have the bundle ACTIVE
     */
    private List<URL> getManagersWithActiveBundle(final String bundleSymbolicNameVersion) {
        final List<URL> managerURLs = new ArrayList<URL>();
        managerURLs.addAll(registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.ACTIVE));
        return managerURLs;
    }

    /**
     * Returns a list of manager URLs that have the bundle in their local
     * repositories/bundle caches.
     *
     * @param bundleSymbolicNameVersion symbolic bundlename / version
     * @return list of manager URLs that have the bundle in the UINSTALLED state
     */
    private List<URL> getManagersWithDeployedBundle(final String bundleSymbolicNameVersion) {
        final List<URL> managerURLs = new ArrayList<URL>();
        managerURLs.addAll(registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.UNINSTALLED));
        return managerURLs;
    }


}
