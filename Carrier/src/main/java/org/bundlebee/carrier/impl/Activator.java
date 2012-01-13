package org.bundlebee.carrier.impl;

import org.bundlebee.carrier.Carrier;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.util.Properties;

/**
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Activator implements BundleActivator {


    private static Logger LOG = LoggerFactory.getLogger(Activator.class);
    public static final String ID = "org.bundlebee.carrier";
    public static final Marker BUNDLE_MARKER = createBundleMarker();
    private static Marker createBundleMarker() {
        Marker bundleMarker = MarkerFactory.getMarker(ID);
        bundleMarker.add(MarkerFactory.getMarker("IS_MARKER"));
        return bundleMarker;
    }
    private CarrierImpl carrier;

    public void start(final BundleContext context) throws Exception {
        if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "Starting BundleBee carrier...");
        this.carrier = new CarrierImpl(context);
        final Properties properties = new Properties();
        context.registerService(Carrier.class.getName(), this.carrier, properties);
    }
    
    public void stop(final BundleContext context) throws Exception {
        carrier.stop();
        if (LOG.isInfoEnabled()) LOG.info(BUNDLE_MARKER, "Stopping BundleBee carrier...");
    } 
} 
