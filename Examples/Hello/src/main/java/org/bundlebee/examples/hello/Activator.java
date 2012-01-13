package org.bundlebee.examples.hello;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * Activator.
 *
 * @author joergp
 */
public class Activator implements BundleActivator {

    public void start(final BundleContext context) throws Exception {

		context.registerService(Hello.class.getName(), new Hello(), null );

		context.registerService(CommandProvider.class.getName(), new HelloCommand(), null);

    }
    public void stop(final BundleContext context) throws Exception {
    }
}
