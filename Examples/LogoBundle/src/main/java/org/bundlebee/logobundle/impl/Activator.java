package org.bundlebee.logobundle.impl;

import org.bundlebee.logobundle.LogoBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


import java.util.Properties;

/**
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 */
public class Activator implements BundleActivator {

    public static final String ID = "org.bundlebee.dashboard";
    private LogoBundle dashboard;

    public void start(final BundleContext context) throws Exception {
        this.dashboard = new LogoBundleImpl(context);
        final Properties properties = new Properties();
//        context.registerService(Dashboard.class.getName(), this.dashboard, properties);
        this.dashboard.start();
    }

    public void stop(final BundleContext context) throws Exception {
        dashboard.stop();
    }
} 
