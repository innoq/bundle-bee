#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.impl;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import ${package}.Bundle;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleListener;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Example Bundle Implementation.
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 */
public class BundleImpl implements Bundle {

    private BundleContext ${artifactId}Context;
    private ServiceTracker carrierTracker;
    private ServiceTracker httpServiceTracker;
    private ReferenceQueue<ServiceTracker> serviceTrackerReferenceQueue = new ReferenceQueue<ServiceTracker>();
    private BundleListener ${artifactId}Listener;

    public BundleImpl() {
    }

    public BundleImpl(final BundleContext ${artifactId}Context) throws InvalidSyntaxException, IOException {
        this.${artifactId}Context = ${artifactId}Context;
    }

    public BundleContext getBundleContext() {
        return ${artifactId}Context;
    }

    public void start() {
    }

    public void stop() {
    }

    public static void main(String[] args) {
        Bundle ${artifactId} = new BundleImpl();
        ${artifactId}.start();
    }
}
