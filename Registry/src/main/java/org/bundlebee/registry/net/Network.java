package org.bundlebee.registry.net;


import static org.bundlebee.registry.impl.RegistryImpl.BUNDLE_MARKER;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Network {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(Network.class);
    private Set<MultiCastNode> multiCastNodes;
    private MultiCastMessageSource multiCastMessageSource;
    private MultiCastMessageListener multiCastMessageListener;

    public Network(final MultiCastMessageSource multiCastMessageSource, final MultiCastMessageListener multiCastMessageListener, final long nodeId) throws IOException {
        this.multiCastNodes = Collections.synchronizedSet(new HashSet<MultiCastNode>()) ;
        this.multiCastMessageSource = multiCastMessageSource;
        this.multiCastMessageListener = multiCastMessageListener;
        initMultiCastNode(nodeId);
    }

    public void stop() {
        if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "sending stop to multiCastNodes:");
        for (final MultiCastNode multiCastNode : this.multiCastNodes) {
            multiCastNode.stop();
        }
    }

    private void initMultiCastNode(final long nodeId) throws IOException {
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "initMultiCastNode....");
        final MultiCastNode multiCastNode = new MultiCastNode(nodeId);
        if (multiCastNode.isRunning()) {
            addMultiCastNode(multiCastNode);
        }
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "initMultiCastNode.... DONE.");
    }

    private void addMultiCastNode(final MultiCastNode multiCastNode) {
        final boolean success = multiCastNodes.add(multiCastNode);
        if (success) {
            multiCastMessageSource.addMultiCastMessageListener(multiCastNode);
            multiCastNode.addMultiCastMessageListener(multiCastMessageListener);
        }
    }

}
