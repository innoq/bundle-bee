package org.bundlebee.testbundle.impl;

import org.bundlebee.testbundle.TestBundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Properties;
import org.bundlebee.testbundle.cli.EchoCommand;
import org.bundlebee.testbundle.cli.TestCommand;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * Activator.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Activator implements BundleActivator {

    public void start(final BundleContext context) throws Exception {
        final Properties properties = new Properties();
        final TestBundle testBundle = new TestBundleImpl();
        context.registerService(TestBundle.class.getName(), testBundle, properties);
        System.out.println("Starting BundleBee TestBundle ...\n");

        context.registerService(CommandProvider.class.getName(), new EchoCommand(testBundle), null);
        context.registerService(CommandProvider.class.getName(), new TestCommand(testBundle), null);

    }

    public void stop(final BundleContext context) throws Exception {
        System.out.println("Stopping BundleBee TestBundle...\n");
    }
}
