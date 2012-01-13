package org.bundlebee.cli.impl;

import org.bundlebee.registry.directory.Grid;
import org.bundlebee.registry.directory.Node;
import org.bundlebee.registry.impl.RegistryImpl;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * BundleBee refresh command.
 * <p/>
 * Date: Jan 13, 2009
 * Time: 4:08:21 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class RefreshCommand implements CommandProvider {

    public void _bbrefresh(final CommandInterpreter commandInterpreter) {
        final String argument = commandInterpreter.nextArgument();
        if (argument == null) {
            RegistryImpl.getInstance().refresh();
            commandInterpreter.println();
            commandInterpreter.println("Refreshing BundleBee grid status asynchronously...");
        } else {
            try {
                // show status of a specific node
                final Grid grid = RegistryImpl.getInstance().getGrid();
                final Node node = grid.getNode(Long.parseLong(argument));
                if (node == null) {
                    commandInterpreter.println("BundleBee node " + argument + " not found.");
                } else {
                    // TODO
                    commandInterpreter.println("Not implemented.");
                }
            } catch (NumberFormatException e) {
                commandInterpreter.println("BundleBee node " + argument + " not found.");
            }

        }
    }

    public String getHelp() {
        return "\tbbrefresh - Requests fresh info about the grid from various nodes. The results are coming in asynchronously.\n";
    }

}