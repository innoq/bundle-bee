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
import java.util.Random;

/**
 * Strategy for finding random Manager URLs.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class RandomDispatchStrategy implements ServiceCallDispatchStrategy {

    private static Logger LOG = LoggerFactory.getLogger(RandomDispatchStrategy.class);
    private Registry registry;
    private Random random = new Random();
    private BundleLifecycleClient bundleLifecycleClient = new BundleLifecycleClient();

    public void setRegistry(final Registry registry) {
        this.registry = registry;
    }

    public void setServiceCallStats(final ServiceCallStats serviceCallStats) {
        // this is random, so we don't need stats
    }

    public URI getManagerURI(final String bundleSymbolicNameVersion, final Object service, final String methodname,
                             final Class[] parameterTypes) {
        final List<URL> managerURLs = new ArrayList<URL>(registry.getGrid().getManagers());
        if (managerURLs.isEmpty()) return null;
        final URL chosenManagerURL = managerURLs.get(random.nextInt(managerURLs.size()));
        final List<URL> chosenManagerURLs = new ArrayList<URL>();
        chosenManagerURLs.add(chosenManagerURL);

        if (registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.INSTALLED).contains(chosenManagerURL)
                || registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.RESOLVED).contains(chosenManagerURL)) {
            // still have to start it
            chosenManagerURLs.retainAll(bundleLifecycleClient.startBundle(chosenManagerURLs, bundleSymbolicNameVersion));
        } else if (registry.getGrid().getManagers(bundleSymbolicNameVersion, Bundle.UNINSTALLED).contains(chosenManagerURL)) {
            // still have to install and start it
            chosenManagerURLs.retainAll(bundleLifecycleClient.installAndStartBundle(chosenManagerURLs, bundleSymbolicNameVersion));
        } else {
            // still have to deploy, install and start it
            chosenManagerURLs.retainAll(bundleLifecycleClient.installAndStartBundle(chosenManagerURLs, bundleSymbolicNameVersion));
        }
        // found an active one
        if (chosenManagerURLs.isEmpty()) return null;
        if (!chosenManagerURLs.isEmpty()) {
            try {
                return chosenManagerURLs.get(0).toURI();
            } catch (URISyntaxException e) {
                LOG.error(e.toString(), e);
            }
        }
        return null;
    }
}
