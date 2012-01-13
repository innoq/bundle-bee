package org.bundlebee.cli.impl;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Activator.
 * <p/>
 * Date: Jan 13, 2009
 * Time: 3:58:37 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class Activator implements BundleActivator {

    public void start(final BundleContext context) throws Exception {
        context.registerService(CommandProvider.class.getName(), new StatusCommand(), null);
        context.registerService(CommandProvider.class.getName(), new RefreshCommand(), null);
        context.registerService(CommandProvider.class.getName(), new InstallCommand(context), null);
        context.registerService(CommandProvider.class.getName(), new UninstallCommand(context), null);
        context.registerService(CommandProvider.class.getName(), new StartCommand(context), null);
        context.registerService(CommandProvider.class.getName(), new StopCommand(context), null);
        context.registerService(CommandProvider.class.getName(), new MethodsCommand(), null);
    }

    public void stop(final BundleContext context) throws Exception {
    }
}
