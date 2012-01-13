package org.bundlebee.registry.directory;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents what we know about the entire grid consisting of {@link Node}s.
 * Nodes in turn contain {@link Bundle}s.
 * <p>
 * This datastructure is ONLY manipulated by {@link org.bundlebee.registry.directory.RegistryDirectory}.
 * To the user ONLY read-only methods are exposed, i.e. all manipulations methods are package private.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see Node
 * @see Bundle
 * @see org.bundlebee.registry.directory.RegistryDirectory#set(org.bundlebee.registry.net.MultiCastMessage)
 */
public class Grid {

    private Map<Long, Node> nodes = new ConcurrentHashMap<Long, Node>();
    private Map<Bundle, Set<URL>> bundleManagerMap = new ConcurrentHashMap<Bundle, Set<URL>>();

    Grid() {
    }

    Node newNode(final long id) {
        return new Node(this, id);
    }

    public Node getNode(final long id) {
        return nodes.get(id);
    }

    /**
     * @return read-only collection of nodes
     */
    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    Node addNode(final Node node) {
        return nodes.put(node.getId(), node);
    }

    void register(final Bundle bundle) {
        Set<URL> managerURLs = bundleManagerMap.get(bundle);
        if (managerURLs == null) {
            managerURLs = new HashSet<URL>();
            bundleManagerMap.put(bundle, managerURLs);
        }
        final URL managerURL = bundle.getNode().getManagerURL();
        if (managerURL != null) managerURLs.add(managerURL);
        bundleManagerMap.put(bundle, managerURLs);
    }

    void unregister(final Bundle bundle) {
        Set<URL> managerURLs = bundleManagerMap.get(bundle);
        final URL managerURL = bundle.getNode().getManagerURL();
        if (managerURLs == null || managerURL == null) return;
        managerURLs.remove(managerURL);
    }

    void unregisterManager(final Node node) {
        final URL managerURL = node.getManagerURL();
        for (final Set<URL> urls : bundleManagerMap.values()) {
            urls.remove(managerURL);
        }
    }

    void registerManager(final Node node) {
        for (final Bundle bundle:node.getBundles()) {
            register(bundle);
        }
    }

    void unregisterNode(final long nodeId) {
        final Node node = nodes.remove(nodeId);
        if (node != null) {
            for (final Bundle bundle:node.getBundles()) {
                unregister(bundle);
            }
        }
    }

    /**
     * @param bundleSymbolicNameVersion name/version
     * @param bundleState state
     * @return unmodifiable set of manager urls
     */
    public Set<URL> getManagers(final String bundleSymbolicNameVersion, final int bundleState) {
        final Bundle bundle = new Bundle(null, bundleSymbolicNameVersion, bundleState);
        final Set<URL> managerURLs = bundleManagerMap.get(bundle);
        if (managerURLs == null) return Collections.emptySet();
        return Collections.unmodifiableSet(managerURLs);
    }

    /**
     * @return unmodifiable set of manager urls
     */
    public Set<URL> getManagers() {
        final Set<URL> urls = new HashSet<URL>();
        for (final Node node:nodes.values()) {
            if (node.getManagerURL() != null) {
                urls.add(node.getManagerURL());
            }
        }
        return Collections.unmodifiableSet(urls);
    }

    /**
     * @return unmodifiable set of repository urls
     */
    public Set<URL> getRepositories() {
        final Set<URL> urls = new HashSet<URL>();
        for (final Node node:nodes.values()) {
            if (node.getRepositoryURL() != null) {
                urls.add(node.getRepositoryURL());
            }
        }
        return Collections.unmodifiableSet(urls);
    }

    public Collection<Long> getNodeIds() {
        return nodes.keySet();
    }

    public String toString() {
        return "Grid[nodes=" + nodes.size() + "]";
    }

}
