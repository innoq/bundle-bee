package org.bundlebee.registry.directory;

import org.bundlebee.registry.impl.RegistryImpl;
import static org.bundlebee.registry.impl.RegistryImpl.BUNDLE_MARKER;
import org.bundlebee.registry.net.MultiCastMessage;
import org.bundlebee.registry.net.MultiCastMessageListener;
import org.bundlebee.registry.net.MultiCastMessageSource;
import static org.bundlebee.registry.net.MultiCastMessage.Type.*;
import static org.bundlebee.registry.net.MultiCastMessage.*;
import static org.bundlebee.registry.net.MultiCastMessage.Operation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Central directory of the registry, responsible for processing {@link org.bundlebee.registry.net.MultiCastMessage}s
 * and manipulating the {@link Grid} accordingly.
 * <p>
 * The bulk of the work is being done in {@link #set(org.bundlebee.registry.net.MultiCastMessage)}.
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see Grid
 */
public class RegistryDirectory implements MultiCastMessageSource, MultiCastMessageListener {

    private static Logger LOG = LoggerFactory.getLogger(RegistryDirectory.class);
    private Map<String, Properties> mainDir;
    private Grid grid = new Grid();
    private Set<MultiCastMessageListener> multiCastMessageListeners = new HashSet<MultiCastMessageListener>();

    public RegistryDirectory() {
        this.mainDir = new HashMap<String, Properties>();
    }

    public synchronized void set(final MultiCastMessage message) {
        final String key = message.getNodeId().toString();
        final MultiCastMessage.Operation operation = message.getOperation();
        if (this.mainDir.containsKey(key)) {
            this.mainDir.remove(key);
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(BUNDLE_MARKER, "=> operation = " + operation + ", type = " + message.getType());
        }
        switch (operation) {
            case REMOVE:
                remove(message);
                break;
            case ADD:
                add(message);
                break;
            case UDPATE:
                // TODO implement Update-Function
                break;
            default:
        }
        //this is to prevent messages flooding the grid with incoming message,
        //stored to directory and then set to outgoing ones again ;-). philipp
        if (!Direction.IN.equals(message.getDirection())) {
            // why are we setting this to out again - is it not already out? -hendrik
            message.setDirection(Direction.OUT);
            if (LOG.isDebugEnabled()) {
                LOG.debug("RegistryDirectory.set(): " + message);
            }
            fireMessage(message);
        } else {
            LOG.debug(BUNDLE_MARKER, "Not propagated: " + message);
        }
    }
    /**
     * Adds a MultiCastMessageListener to the Directory, that is called by every Directory Update.
     * @param multiCastMessageListener added Listener.
     */
    public void addMultiCastMessageListener(final MultiCastMessageListener multiCastMessageListener) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding " + multiCastMessageListener);
        }
        this.multiCastMessageListeners.add(multiCastMessageListener);
    }

    private void fireMessage(final MultiCastMessage multiCastMessage) {
        for (final MultiCastMessageListener multiCastMessageListener : multiCastMessageListeners) {
            multiCastMessageListener.processMessage(multiCastMessage);
        }
        if (multiCastMessageListeners.isEmpty() && LOG.isDebugEnabled()) {
            LOG.debug(BUNDLE_MARKER, "No MultiCastMessageListeners registered.");
        }
    }

    /**
     * Processes a MultiCastMessage (adds the Result to the Directory if necessary).
     * @param multiCastMessage
     */
    public void processMessage(final MultiCastMessage multiCastMessage) {
        set(multiCastMessage);
    }

    private void add(final MultiCastMessage message) {
        if (message.hasType()) {
            switch (message.getType()) {
                case REPOSITORY:
                    addRepository(message);
                    break;
                case MANAGER:
                    addManager(message);
                    break;
                case BUNDLE:
                    addBundle(message);
                    break;
                case MEMORY:
                    addMemory(message);
                case HI:
                case NODE:
                    addNode(message);
            }
        }
        this.mainDir.put(message.getNodeId() + "-" + message.getType(), message.getValues());
    }

    private void addManager(final MultiCastMessage message) {
        if (message.hasURL() && message.hasNodeId()) {
            final URL url = message.getURL();
            getNode(message).setManagerURL(url);
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "adding Manager: " + url.toString());
            }
        }
    }

    private void addRepository(final MultiCastMessage message) {
        if (message.hasURL() && message.hasNodeId()) {
            final URL url = message.getURL();
            getNode(message).setRepositoryURL(url);
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "adding Repository: " + url.toString());
            }
        }
    }

    private void addBundle(final MultiCastMessage message) {
        if (message.hasName() && message.hasState() && message.hasNodeId()) {
            final Node node = getNode(message);
            final Bundle bundle = node.newBundle(message.getName(), message.getState());
            node.addBundle(bundle);
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "adding Bundle: " + bundle);
            }
        }
    }

    private void addMemory(final MultiCastMessage message) {
        if (message.hasMaxMemory() && message.hasFreeMemory() && message.hasNodeId()) {
            final Node node = getNode(message);
            node.setMaxMemory(message.getMaxMemory());
            node.setFreeMemory(message.getFreeMemory());
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "adding Memory: " + message.getFreeMemory() + "/" + message.getMaxMemory());
            }
        }
    }

    private void addNode(final MultiCastMessage message) {
        if (message.hasSender() && message.hasNodeId()) {
            try {
                final Node node = getNode(message);
                node.setPrivatePort(message.getSenderPort());
                node.setPrivateAddress(message.getSenderAddress());
                if (LOG.isInfoEnabled()) {
                    LOG.info(BUNDLE_MARKER, "adding Node: " + message.getSenderAddress() + ":" + message.getSenderPort());
                }
            } catch (UnknownHostException e) {
                LOG.error(e.toString(), e);
            }
        }
    }

    private void remove(final MultiCastMessage message) {
        if (message.hasType()) {
            switch (message.getType()) {
                case REPOSITORY:
                    removeRepository(message);
                    break;
                case MANAGER:
                    removeManager(message);
                    break;
                case BUNDLE:
                    removeBundle(message);
                    break;
                case NODE:
                    removeNode(message);
                    break;
            }
        }
        this.mainDir.remove(message.getNodeId() + "-" + message.getType());
    }

    private void removeManager(final MultiCastMessage message) {
        if (message.hasURL() && message.hasNodeId()) {
            final URL url = message.getURL();
            getNode(message).setManagerURL(null);
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "removing Manager: " + url.toString());
            }
        }
    }

    private void removeRepository(final MultiCastMessage message) {
        if (message.hasURL() && message.hasNodeId()) {
            final URL url = message.getURL();
            getNode(message).setRepositoryURL(null);
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "removing Repository: " + url.toString());
            }
        }
    }

    private void removeBundle(final MultiCastMessage message) {
        if (message.hasName() && message.hasNodeId()) {
            final Node node = getNode(message);
            node.removeBundle(message.getName());
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "removing Bundle: " + message.getName());
            }
        }
    }

    private void removeNode(final MultiCastMessage message) {
        if (message.hasNodeId()) {
            grid.unregisterNode(message.getNodeId());
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "removing Node: " + message.getNodeId());
            }
        }
    }

    private Node getNode(final MultiCastMessage message) {
        final long nodeId = message.getNodeId();
        Node node = this.grid.getNode(nodeId);
        if (node == null) {
            node = grid.newNode(nodeId);
            grid.addNode(node);
        }
        return node;
    }

    /**
     * Shows the Content of the RegistryDirectory.
     * TODO: for Debugging (needs to be removed?)
     */
    public void showContent() {
        // TODO: fix this so that it uses the Grid datastructure
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "=== UPDATED Directory ===");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(BUNDLE_MARKER, "= " + this.getList().length
                    + " entries ======================================");
        }
        if (LOG.isDebugEnabled()) {
            LOG.debug(BUNDLE_MARKER, this.mainDir.toString());
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "==================================================");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "= Managers ");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "= " + this.getGrid().getManagers().size()
                    + " entries ======================================");
        }
        for (final URL url : this.getGrid().getManagers()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, url.toString());
            }
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "==================================================");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "= Repositories ");
        }
        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "= " + this.getGrid().getRepositories().size()
                    + " entries ======================================");
        }
        for (final URL url : this.getGrid().getRepositories()) {
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, url.toString());
            }
        }

        if (LOG.isInfoEnabled()) {
            LOG.info(BUNDLE_MARKER, "==================================================");
        }
    }

    // TODO: do we need this method? does not seem to be used -hendrik
    private MultiCastMessage get(String key) {
        if (this.mainDir.containsKey(key)) {
            return new MultiCastMessage(key, this.mainDir.get(key));
        } else {
            return new MultiCastMessage();
        }
    }

    /**
     * Returns the Content of the RegistryDirectory as a List of {@link MultiCastMessage}s.
     * @return List of {@link MultiCastMessage}s
     */
    public synchronized MultiCastMessage[] getList() {
        final List<MultiCastMessage> messages = new ArrayList<MultiCastMessage>();
        for (final Node node : grid.getNodes()) {
            final long nodeId = node.getId();
            // node properties
            if (node.getRepositoryURL() != null) {
                final MultiCastMessage message = createNodeMessage(node.getPrivateAddressAndPort(), ADD);
                message.setNodeId(nodeId);
                messages.add(message);
            }
            if (node.getRepositoryURL() != null) {
                final MultiCastMessage message = createMultiCastURLMessage(REPOSITORY, node.getRepositoryURL(), ADD);
                message.setNodeId(nodeId);
                messages.add(message);
            }
            if (node.getManagerURL() != null) {
                final MultiCastMessage message = createMultiCastURLMessage(MANAGER, node.getManagerURL(), ADD);
                message.setNodeId(nodeId);
                messages.add(message);
            }
            if (node.getFreeMemory() != 0 && node.getMaxMemory() != 0) {
                final MultiCastMessage message = createMultiCastMemoryMessage(node.getFreeMemory(), node.getMaxMemory(), ADD);
                message.setNodeId(nodeId);
                messages.add(message);
            }
            // bundles
            for (final Bundle bundle : node.getBundles()) {
                final MultiCastMessage message = createMultiCastBundleMessage(bundle.getName(), bundle.getState(), ADD);
                message.setNodeId(nodeId);
                messages.add(message);
            }
        }

        return messages.toArray(new MultiCastMessage[messages.size()]);
    }

    /**
     * Returns the current {@link Grid} of the RegistryDirectory.
     * @return the current grid.
     */
    public Grid getGrid() {
        return grid;
    }

    /**
     * Shows a key=>value set as a String.
     * @return String representation of Directory Content.
     * TODO: Debug, needs to be removed?
     */
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (String s : this.mainDir.keySet()) {
            sb.append(new MultiCastMessage(s, this.mainDir.get(s)).toString() + "\n");
        }
        return sb.toString();
    }

    /**
     * Creates a {@link MultiCastMessage} with an url (Registry/Manager Job).
     * @param type
     * @param url
     * @param operation
     */
    public void setURLMessage(final MultiCastMessage.Type type, final URL url, final MultiCastMessage.Operation operation) {
        set(createMultiCastURLMessage(type, url, operation));
    }

    private static MultiCastMessage createMultiCastURLMessage(final MultiCastMessage.Type type, final URL url, final MultiCastMessage.Operation operation) {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTYKEY_TYPE, type.toString());
        properties.setProperty(PROPERTYKEY_URL, url.toString());
        final MultiCastMessage multiCastMessage = new MultiCastMessage(RegistryImpl.getInstance().getNodeId(), properties);
        multiCastMessage.setOperation(operation);
        return multiCastMessage;
    }

    /**
     * Creates a {@link MultiCastMessage} with memory info (Status Update).
     * @param freeMemory
     * @param maxMemory
     * @param operation
     */
    public void setMemoryMessage(final long freeMemory, final long maxMemory, final MultiCastMessage.Operation operation) {
        set(createMultiCastMemoryMessage(freeMemory, maxMemory, operation));
    }

    private static MultiCastMessage createMultiCastMemoryMessage(final long freeMemory, final long maxMemory, final MultiCastMessage.Operation operation) {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTYKEY_TYPE, MEMORY.toString());
        properties.setProperty(PROPERTYKEY_FREE_MEMORY, Long.toString(freeMemory));
        properties.setProperty(PROPERTYKEY_MAX_MEMORY, Long.toString(maxMemory));
        final MultiCastMessage multiCastMessage = new MultiCastMessage(RegistryImpl.getInstance().getNodeId(), properties);
        multiCastMessage.setOperation(operation);
        return multiCastMessage;
    }

    /**
     * Creates a {@link MultiCastMessage} with Bundle Information. (Adds a Bundle Information to the Registry).
     * @param name
     * @param state
     * @param operation
     */
    public void setBundleMessage(final String name, final int state, final MultiCastMessage.Operation operation) {
        set(createMultiCastBundleMessage(name, state, operation));
    }

    private static MultiCastMessage createMultiCastBundleMessage(final String name, final int state, final MultiCastMessage.Operation operation) {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTYKEY_TYPE, BUNDLE.toString());
        properties.setProperty(PROPERTYKEY_NAME, name);
        properties.setProperty(PROPERTYKEY_STATE, Integer.toString(state));
        final MultiCastMessage multiCastMessage = new MultiCastMessage(RegistryImpl.getInstance().getNodeId(), properties);
        multiCastMessage.setOperation(operation);
        return multiCastMessage;
    }

    public void setBundleMessage(final String name, final MultiCastMessage.Operation operation) {
        set(createMultiCastBundleMessage(name, operation));
    }

    private static MultiCastMessage createMultiCastBundleMessage(final String name, final MultiCastMessage.Operation operation) {
        final Properties properties = new Properties();
        properties.setProperty(PROPERTYKEY_TYPE, BUNDLE.toString());
        properties.setProperty(PROPERTYKEY_NAME, name);
        final MultiCastMessage multiCastMessage = new MultiCastMessage(RegistryImpl.getInstance().getNodeId(), properties);
        multiCastMessage.setOperation(operation);
        return multiCastMessage;
    }

    public void setNodeMessage(final Operation operation) {
        set(createNodeMessage(null, operation));
    }

    public void setNodeMessage(final String address, final Operation operation) {
        set(createNodeMessage(address, operation));
    }

    private static MultiCastMessage createNodeMessage(final String address, final Operation operation) {
        final Properties properties = new Properties();
        if (address != null) {
            properties.setProperty(MultiCastMessage.PROPERTYKEY_SENDER, address);
        }
        properties.setProperty(PROPERTYKEY_TYPE, NODE.toString());
        final MultiCastMessage multiCastMessage = new MultiCastMessage(RegistryImpl.getInstance().getNodeId(), properties);
        multiCastMessage.setOperation(operation);
        return multiCastMessage;
    }

    public void setHiMessage() {
        set(createHiMessage());
    }

    private static MultiCastMessage createHiMessage() {
        final Node node = RegistryImpl.getInstance().getGrid().getNode(RegistryImpl.getInstance().getNodeId());
        assert node != null;
        return new MultiCastMessage(RegistryImpl.getInstance().getNodeId(), Type.HI, node.getPrivateAddressAndPort());
    }
}
