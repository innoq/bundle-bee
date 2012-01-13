package org.bundlebee.cli.impl;

import org.bundlebee.remoteservicecall.BundleLifecycleClient;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.osgi.framework.BundleContext;

/**
 * Install Command.
 * <p/>
 * Date: Jan 13, 2009
 * Time: 5:56:00 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class UninstallCommand extends BundleLifecycleClientCommand implements CommandProvider {

    public UninstallCommand(final BundleContext bundleContext) {
        super(bundleContext, BundleLifecycleClient.Method.UNINSTALL);
    }

    public void _bbuninstall(final CommandInterpreter commandInterpreter) {
        execute(commandInterpreter);
    }

}