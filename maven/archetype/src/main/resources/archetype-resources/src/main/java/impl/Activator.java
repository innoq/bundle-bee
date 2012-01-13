#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package}.impl;

import ${package}.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;


import java.util.Properties;

/**
 * Example Bundle Interface.
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 */
public class Activator implements BundleActivator {

    public static final String ID = "${package}";

    private Bundle ${artifactId};

    public void start(final BundleContext context) throws Exception {
        this.${artifactId} = new BundleImpl(context);
        final Properties properties = new Properties();
        this.${artifactId}.start();
    } 
    public void stop(final BundleContext context) throws Exception {
        ${artifactId}.stop();
    }
} 
