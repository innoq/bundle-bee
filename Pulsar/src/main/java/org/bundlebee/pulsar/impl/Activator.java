package org.bundlebee.pulsar.impl;

import org.bundlebee.testbundle.TestBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Starts a thread that peridiodically calls a method on the TestBundle service.
 * This is for simple testing purposes.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Activator implements BundleActivator {

    private ServiceTracker testBundleTracker;
    private boolean running;

    public void start(final BundleContext bundleContext) throws Exception {
        this.testBundleTracker = new ServiceTracker(bundleContext, TestBundle.class.getName(), null);
        this.testBundleTracker.open();
        // start thread that gets testbundle and class it periodically
        new Thread(new Runnable() {
            public void run() {
                try {
                    setRunning(true);
                    while (isRunning()) {
                        final TestBundle testBundle = (TestBundle)testBundleTracker.getService();
                        if (testBundle == null) System.out.println("TestBundle not found.");
                        else {
                            testBundle.beep();
                        }
                        Thread.sleep(1000);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        ).start();
    }

    public void stop(final BundleContext bundleContext) throws Exception {
        //stop that thread
        setRunning(false);
        this.testBundleTracker.close();
    }

    public synchronized boolean isRunning() {
        return running;
    }

    public synchronized void setRunning(final boolean running) {
        this.running = running;
    }
}
