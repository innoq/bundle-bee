package org.bundlebee.watchdog.impl;

import org.bundlebee.registry.Registry;
import org.bundlebee.registry.impl.RegistryImpl;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Reports max/free memory to the registry on a regular basis
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Activator implements BundleActivator {

    private static Logger LOG = LoggerFactory.getLogger(Activator.class);
    public static final String ID = "org.bundlebee.watchdog";
    public static final Marker BUNDLE_MARKER = createBundleMarker();
    private static final int DELAY = 2000;
    private static final int PERIOD = 2000;

    private Timer timer;
    private long lastFreeMemory;
    private long lastMaxMemory;
    private double changeThreshold = 0.05;

    private static Marker createBundleMarker() {
        Marker bundleMarker = MarkerFactory.getMarker(ID);
        bundleMarker.add(MarkerFactory.getMarker("IS_MARKER"));
        return bundleMarker;
    }

    public void start(BundleContext context) throws Exception { 
        if (LOG.isDebugEnabled()) LOG.debug("Starting Watchdog Bundle...");
        // TODO: investigate use of ManagementFactory.getMemoryMXBean();
        timer = new Timer("Watchdog Timer", true);
        timer.schedule(new TimerTask() {
            public void run() {
                final Registry registry = RegistryImpl.getInstance();
                final Runtime runtime = Runtime.getRuntime();
                final long freeMemory = runtime.freeMemory();
                final long maxMemory = runtime.maxMemory();
                // only fire, if change is big enough
                if (Math.abs(freeMemory - lastFreeMemory) > maxMemory * changeThreshold
                        || Math.abs(maxMemory - lastMaxMemory) > maxMemory * changeThreshold) {
                    registry.registerMemory(freeMemory, maxMemory);
                    lastFreeMemory = freeMemory;
                    lastMaxMemory = maxMemory;
                }
            }
        }, DELAY, PERIOD);
    }
    public void stop(BundleContext context) throws Exception {
        if (LOG.isDebugEnabled()) LOG.debug("Stopping Watchdog Bundle...");
        timer.cancel();
    } 
} 
