package org.bundlebee.bundle.impl;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import org.bundlebee.bundle.Bundle;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Example Bundle Implementation.
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 */
public class BundleImpl implements Bundle {

    private BundleContext bundleContext;
    private ServiceTracker carrierTracker;
    private ServiceTracker httpServiceTracker;
    private ReferenceQueue<ServiceTracker> serviceTrackerReferenceQueue = new ReferenceQueue<ServiceTracker>();
    private BundleListener bundleListener;

    public BundleImpl() {
    }

    public BundleImpl(final BundleContext bundleContext) throws InvalidSyntaxException, IOException {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void start() {
    }

    public void stop() {
    }

    public static void main(String[] args) {
        Bundle bundle = new BundleImpl();
        bundle.start();
    }
}
