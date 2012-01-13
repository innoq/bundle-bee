package org.bundlebee.bundle.impl;

import org.bundlebee.bundle.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


import java.util.Properties;

/**
 * Example Bundle Interface.
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 */
public class Activator implements BundleActivator {

    public static final String ID = "org.bundlebee.bundle";

    private Bundle bundle;

    public void start(final BundleContext context) throws Exception {
        this.bundle = new BundleImpl(context);
        final Properties properties = new Properties();
        this.bundle.start();
    } 
    public void stop(final BundleContext context) throws Exception {
        bundle.stop();
    }
} 
