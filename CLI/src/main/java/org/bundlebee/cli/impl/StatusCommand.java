package org.bundlebee.cli.impl;

import org.bundlebee.registry.directory.Bundle;
import org.bundlebee.registry.directory.Grid;
import org.bundlebee.registry.directory.Node;
import org.bundlebee.registry.impl.RegistryImpl;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * BundleBee status command.
 * <p/>
 * Date: Jan 13, 2009
 * Time: 4:08:21 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class StatusCommand implements CommandProvider {

    public void _bbstatus(final CommandInterpreter commandInterpreter) {
        final String argument = commandInterpreter.nextArgument();
        if (argument == null) {
            // show status of the whole grid
            final Grid grid = RegistryImpl.getInstance().getGrid();
            commandInterpreter.println();
            println(commandInterpreter, grid);
            commandInterpreter.println();
            final List<Long> nodeIds = new ArrayList<Long>(grid.getNodeIds());
            Collections.sort(nodeIds);
            for (final Long id:nodeIds) {
                final Node node = grid.getNode(id);
                println(commandInterpreter, node);
            }
        } else {
            try {
                // show status of a specific node
                final Grid grid = RegistryImpl.getInstance().getGrid();
                final Node node = grid.getNode(Long.parseLong(argument));
                if (node == null) {
                    commandInterpreter.println("BundleBee node " + argument + " not found.");
                } else {
                    commandInterpreter.println();
                    println(commandInterpreter, node);
                    commandInterpreter.println();
                    commandInterpreter.println("Bundles on node " + node.getId() + ":");
                    commandInterpreter.println();
                    commandInterpreter.println("State\tBundle");
                    final List<Bundle> bundles = new ArrayList<Bundle>(node.getBundles());
                    Collections.sort(bundles);
                    for (final Bundle bundle:bundles) {
                        println(commandInterpreter, bundle);
                    }
                }
            } catch (NumberFormatException e) {
                commandInterpreter.println("BundleBee node " + argument + " not found.");
            }

        }
    }

    private void println(final CommandInterpreter ci, final Bundle bundle) {
        ci.println(bundle.getStateDescription() + "\t" + bundle.getName());
    }

    private void println(final CommandInterpreter ci, final Node node) {
        ci.print("BundleBee node " + node.getId());
        if (node.isLocalNode()) ci.print(" (local)");
        ci.println(":");
        ci.println("\tAddress\t\t" + node.getPrivateAddressAndPort());
        ci.println("\tBundles\t\t" + node.getBundles().size());
        ci.println("\tManager URL\t" + node.getManagerURL());
        ci.println("\tRepository URL\t" + node.getRepositoryURL());
        ci.println("\tMemory\t\t" + (node.getFreeMemory() >> 10)  + "kb of " + (node.getMaxMemory() >> 10) + "kb are free");
    }

    private void println(final CommandInterpreter ci, final Grid grid) {
        ci.println("BundleBee grid with " + grid.getNodes().size() + " node(s).");
    }

    public String getHelp() {
        return "\tbbstatus [nodeId] - shows the status of the BundleBee grid or a single node.\n";
    }

}
