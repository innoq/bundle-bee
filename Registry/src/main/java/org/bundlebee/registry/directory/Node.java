package org.bundlebee.registry.directory;

import org.bundlebee.registry.impl.RegistryImpl;

import java.net.InetAddress;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a node in the {@link Grid}. Each node contains {@link Bundle}s.
 * <p>
 * To create a node instance, call {@link Grid#newNode(long)}.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Node {

    private long id;
    private URL managerURL;
    private URL repositoryURL;
    private long freeMemory;
    private long maxMemory;
    private InetAddress privateAddress;
    private int privatePort;
    private Map<String, Bundle> bundles = new ConcurrentHashMap<String, Bundle>();
    private Grid grid;

    /**
     * Utility class to sort nodes by the amount of free memory they have.
     */
    public static class FreeMemoryComparator implements Comparator<Node> {
        public int compare(final Node node1, final Node node2) {
            // TODO: check, is the right way around? we want descending. -hendrik
            return ((Long)node1.freeMemory).compareTo(node2.freeMemory);
        }
    }

    Node(final Grid grid, final long id) {
        this.grid = grid;
        this.id = id;
    }

    Bundle newBundle(final String name, final int state) {
        return new Bundle(this, name, state);
    }

    Grid getGrid() {
        return grid;
    }

    public long getId() {
        return id;
    }

    public boolean isLocalNode() {
        return id == RegistryImpl.getInstance().getNodeId();
    }

    public URL getManagerURL() {
        return managerURL;
    }

    void setManagerURL(final URL managerURL) {
        final URL oldURL = this.managerURL;
        if (managerURL == null && oldURL != null && grid != null) {
            this.grid.unregisterManager(this);
            this.managerURL = managerURL;
        }
        if (managerURL != null && oldURL == null && grid != null) {
            this.managerURL = managerURL;
            this.grid.registerManager(this);
        }
        if (managerURL != null && !managerURL.equals(oldURL) && grid != null) {
            this.grid.unregisterManager(this);
            this.managerURL = managerURL;
            this.grid.registerManager(this);
        }
    }

    public URL getRepositoryURL() {
        return repositoryURL;
    }

    void setRepositoryURL(final URL repositoryURL) {
        this.repositoryURL = repositoryURL;
    }

    public long getFreeMemory() {
        return freeMemory;
    }

    void setFreeMemory(final long freeMemory) {
        this.freeMemory = freeMemory;
    }

    public long getMaxMemory() {
        return maxMemory;
    }

    void setMaxMemory(final long maxMemory) {
        this.maxMemory = maxMemory;
    }

    void addBundle(final Bundle bundle) {
        final Bundle removedBundle = bundles.put(bundle.getName(), bundle);
        if (removedBundle != null) grid.unregister(removedBundle);
        grid.register(bundle);
    }

    boolean removeBundle(final Bundle bundle) {
        return removeBundle(bundle.getName());
    }

    boolean removeBundle(final String bundleName) {
        final Bundle removedBundle = bundles.remove(bundleName);
        if (removedBundle != null) grid.unregister(removedBundle);
        return removedBundle != null;
    }

    public Collection<Bundle> getBundles() {
        return Collections.unmodifiableCollection(bundles.values());
    }

    public InetAddress getPrivateAddress() {
        return privateAddress;
    }

    void setPrivateAddress(final InetAddress privateAddress) {
        this.privateAddress = privateAddress;
    }

    public int getPrivatePort() {
        return privatePort;
    }

    void setPrivatePort(final int privatePort) {
        this.privatePort = privatePort;
    }

    public String getPrivateAddressAndPort() {
        final String address;
        if (privateAddress != null && privatePort != 0) {
            address = privateAddress.getHostAddress() + ":" + privatePort;
        } else {
            address = null;
        }
        return address;
    }

    public String toString() {
        return "Node[id=" + id
                + ",manager=" + managerURL
                + ",repository=" + repositoryURL
                + ",memory=" + freeMemory + "/" + maxMemory
                + ",bundles=" + bundles.size() + "]";
    }
}
