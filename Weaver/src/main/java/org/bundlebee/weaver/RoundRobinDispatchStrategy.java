package org.bundlebee.weaver;

import org.bundlebee.registry.Registry;
import org.bundlebee.remoteservicecall.BundleLifecycleClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.Bundle;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Strategy for finding Manager URIs following a round robin approach.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class RoundRobinDispatchStrategy implements ServiceCallDispatchStrategy {

    private static Logger LOG = LoggerFactory.getLogger(RoundRobinDispatchStrategy.class);
    private Registry registry;
    private List<URL> managerList = new ArrayList<URL>();
    private int index;
    private final BundleLifecycleClient bundleLifecycleClient = new BundleLifecycleClient();

    public RoundRobinDispatchStrategy() {
        LOG.info("Initializing RoundRobinDispatchStrategy");
    }

    public void setRegistry(final Registry registry) {
        this.registry = registry;
    }

    public void setServiceCallStats(final ServiceCallStats serviceCallStats) {
    }

    public synchronized URI getManagerURI(final String bundleSymbolicNameVersion, final Object service, final String methodname,
                             final Class[] parameterTypes) {
        try {
            updateManagerList();
            if (managerList.isEmpty()) return null;
            index = index % managerList.size();
            final URL url = managerList.get(index);
            index++;
            // make sure bundle is installed and started on remote node
            final URL url2 = installAndStartBundle(bundleSymbolicNameVersion, url);
            // found an active one
            if (url2 == null) return null;
            return url2.toURI();
        } catch (URISyntaxException e) {
            LOG.error(e.toString(), e);
        }
        return null;
    }

    private URL installAndStartBundle(final String bundleSymbolicNameVersion, final URL url) {
        final List<URL> urls = new ArrayList<URL>();
        urls.add(url);
        if (registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.INSTALLED).contains(url)
                || registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.RESOLVED).contains(url)) {
            // still have to start it
            urls.retainAll(bundleLifecycleClient.startBundle(urls, bundleSymbolicNameVersion));
        } else if (registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.UNINSTALLED).contains(url)) {
            // still have to install and start it
            urls.retainAll(bundleLifecycleClient.installAndStartBundle(urls, bundleSymbolicNameVersion));
        } else {
            // still have to deploy, install and start it
            urls.retainAll(bundleLifecycleClient.installAndStartBundle(urls, bundleSymbolicNameVersion));
        }
        if (urls.isEmpty()) return null;
        return urls.iterator().next();
    }

    private void updateManagerList() {
        final Set<URL> currentManagerURLs = registry.getGrid().getManagers();
        managerList.retainAll(currentManagerURLs);
        for (final URL url:currentManagerURLs) {
                if (!managerList.contains(url)) managerList.add(url);
            }
    }
}