package org.bundlebee.registry.impl;

import org.bundlebee.registry.Registry;
import org.bundlebee.registry.directory.Grid;
import org.bundlebee.registry.directory.RegistryDirectory;
import org.bundlebee.registry.net.MultiCastMessage;
import static org.bundlebee.registry.net.MultiCastMessage.Operation.ADD;
import static org.bundlebee.registry.net.MultiCastMessage.Operation.REMOVE;
import static org.bundlebee.registry.net.MultiCastMessage.Type.MANAGER;
import static org.bundlebee.registry.net.MultiCastMessage.Type.REPOSITORY;
import org.bundlebee.registry.net.Network;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


/**
 * Implements the {@link org.bundlebee.registry.Registry} interface  and serves
 * mostly as a front for the actual service. Many tasks are delegated to
 * other classes.
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see org.bundlebee.registry.directory.RegistryDirectory
 */
public class RegistryImpl implements Registry {

    private static org.slf4j.Logger LOG = LoggerFactory.getLogger(RegistryImpl.class);
    public static final String ID = "org.bundlebee.registry";
    public static final Marker BUNDLE_MARKER = createBundleMarker();
    private static final String ORG_BUNDLEBEE_REGISTRY_REFRESHPERIOD = "org.bundlebee.registry.refreshperiod";
    private static final int DEFAULT_REFRESH_PERIOD = 60000;
    private Network network;
    private final Thread shutdownHook = new Thread("BundleBeeRegistryShutdown") {
        public void run() {
            RegistryImpl.this.stop();
        }
    };

    private static Marker createBundleMarker() {
        Marker bundleMarker = MarkerFactory.getMarker(ID);
        bundleMarker.add(MarkerFactory.getMarker("IS_MARKER"));
        return bundleMarker;
    }

    private static Registry instance = new RegistryImpl();
    private RegistryDirectory registryDirectory;
    private final long nodeId = Math.abs(UUID.randomUUID().getLeastSignificantBits());
    private Timer periodicRefresh;
    private long refreshPeriod = initRefreshPeriod();

    private RegistryImpl() {
        if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "NODE_ID = " + nodeId);
        try {
            this.registryDirectory = new RegistryDirectory();
            this.network = new Network(registryDirectory, registryDirectory, nodeId);
            Runtime.getRuntime().addShutdownHook(shutdownHook);
            start();
        } catch (Exception e) {
            LOG.error(BUNDLE_MARKER, "Failed to start bundlebee registry: " + e.toString(), e);
            throw new RuntimeException("Failed to start bundlebee registry", e);
        }
    }

    public MultiCastMessage[] getList() {
        return registryDirectory.getList();
    }

    public long getNodeId() {
        return nodeId;
    }

    public static Registry getInstance() {
        return instance;
    }

    public void start() {
        this.periodicRefresh = new Timer("PeriodicRegistryRefresh");
        this.periodicRefresh.schedule(new TimerTask() {
            public void run() {
                if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Starting periodic refresh...");
                refresh();
            }
        }, refreshPeriod, refreshPeriod);
    }

    private static long initRefreshPeriod() {
        final String s = System.getProperty(ORG_BUNDLEBEE_REGISTRY_REFRESHPERIOD, "" + DEFAULT_REFRESH_PERIOD);
        long refreshPeriod = DEFAULT_REFRESH_PERIOD;
        try {
            refreshPeriod = Long.parseLong(s);
        } catch (NumberFormatException e) {
            LOG.error(e.toString(), e);
        }
        return refreshPeriod;
    }

    public void stop() {
        this.periodicRefresh.cancel();
        unregisterNode();
        this.network.stop();
    	if (LOG.isDebugEnabled()) if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "...done");
        if (Thread.currentThread() != shutdownHook) {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        }
    }

    public void registerManager(final URL url) {
        this.registryDirectory.setURLMessage(MANAGER, url, ADD);
    }

    public void unregisterManager(final URL url) {
        this.registryDirectory.setURLMessage(MANAGER, url, REMOVE);
    }

    public void registerRepository(final URL url) {
        this.registryDirectory.setURLMessage(REPOSITORY, url, ADD);
    }

    public void unregisterRepository(final URL url) {
        this.registryDirectory.setURLMessage(REPOSITORY, url, REMOVE);
    }

    public void registerBundle(final String bundleSymbolicNameVersion, final int bundleState) {
        if (LOG.isDebugEnabled()) if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "registerBundle(" + bundleSymbolicNameVersion + ", " + bundleState +")");
        this.registryDirectory.setBundleMessage(bundleSymbolicNameVersion, bundleState, ADD);
    }

    public void unregisterBundle(final String bundleSymbolicNameVersion) {
        if (LOG.isDebugEnabled()) if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "unregisterBundle(" + bundleSymbolicNameVersion + ")");
        this.registryDirectory.setBundleMessage(bundleSymbolicNameVersion, REMOVE);
    }

    public void registerMemory(final long freeMemory, final long maxMemory) {
        if (LOG.isDebugEnabled()) if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "registerMemory(" + freeMemory + ", " + maxMemory + ")");
        this.registryDirectory.setMemoryMessage(freeMemory, maxMemory, ADD);
    }

    public Grid getGrid() {
        return this.registryDirectory.getGrid();
    }

    public void refresh() {
        this.registryDirectory.setHiMessage();
    }

    public void unregisterNode() {
        if (LOG.isDebugEnabled()) if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "unregisterNode()");
        this.registryDirectory.setNodeMessage(REMOVE);
    }

    public void test(){
        //new Thread(new addAndRemoveRegistryAndManager(this)).start();
		try {
			URL url = new URL("http://" + InetAddress.getLocalHost().getHostAddress() + ":8080");
		    this.registerRepository(url);
			this.registerManager(url);  
			Thread.sleep(10000);
			this.unregisterManager(url);
			this.unregisterRepository(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}        	
    }
}
