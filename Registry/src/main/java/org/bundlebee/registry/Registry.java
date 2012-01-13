package org.bundlebee.registry;

import org.bundlebee.registry.directory.Grid;

import java.net.URL;

/**
 * Distributed registry for storing all grid information.
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface Registry {

    /**
     * Starts the Registry.
     */
    void start();
    /**
     * Stops the Registry.
     */
    void stop();

    /**
     * Registers is manager with its base URL. E.g. http://myhostname:port/bundlebee/manager
     *
     * @param url url to the manager
     */
    void registerManager(URL url);

    /**
     * Removes a manager URL from the registry.
     *
     * @param url manager URL
     */
    void unregisterManager(URL url);

    /**
     * Registers a bundle.
     * If the same bundle (url and name/version are equal) is already registered, only the state
     * has to be changed. That is, the old state is automatically unregistered and the new state is
     * registered.
     *
     * @param bundleSymbolicNameVersion BundleSymbolicName [ "/" BundleVersion ]
     * @param bundleState state the bundle is in, see {@link org.osgi.framework.Bundle#getState()}
     */
    void registerBundle(String bundleSymbolicNameVersion, int bundleState);

    /**
     * Unregisters a bundle that's hosted through a manager.
     *
     * @param bundleSymbolicNameVersion BundleSymbolicName [ "/" BundleVersion ]
     */
    void unregisterBundle(String bundleSymbolicNameVersion);

    /**
     * Registers a repository with its base URL, e.g. <code>file://Users/ernie/.m2/repository/repository.xml</code>
     * or <code>http://felix.apache.org/obr/releases.xml</code>. Obviously local repositories that are not accessible
     * from all nodes are not so welcome.
     *
     * @param url repository URL
     */
    void registerRepository(URL url);

    /**
     * Removes a repository URL from the registry.
     *
     * @param url repository url
     */
    void unregisterRepository(URL url);

    /**
     * Registers the current memory consumption on a node.
     *
     * @param freeMemory free memory in bytes
     * @param maxMemory max memory in bytes
     * @see Runtime#freeMemory()
     * @see Runtime#maxMemory()
     */
    void registerMemory(long freeMemory, long maxMemory);

    /**
     * Transient ID for this node/VM.
     * This id will probably be different, the next time the registry is started.
     *
     * @return node/VM id.
     */
    long getNodeId();

    /**
     * Removed a node from the registry.
     * (adds are implicit)
     */
    void unregisterNode();

    /**
     * Full, read-only picture of what we know about the grid.
     *
     * @return read-only grid representation
     */
    Grid getGrid();

    /**
     * Refreshes this node's registry and view of the grid.
     */
    void refresh();
}
