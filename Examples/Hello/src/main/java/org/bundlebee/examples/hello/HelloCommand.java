package org.bundlebee.examples.hello;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * HelloCommand.
 *
 * @author joergp
 */
public class HelloCommand implements CommandProvider {

	int sequence = 0;

    public String getHelp() {
        return "\tbbhello - start hello tutorial class\n";
    }

    public void _bbhello(final CommandInterpreter commandInterpreter) {
		Hello h = new Hello();
		System.out.println("computational result: " + h.compute( sequence++ ) );
    }

}
