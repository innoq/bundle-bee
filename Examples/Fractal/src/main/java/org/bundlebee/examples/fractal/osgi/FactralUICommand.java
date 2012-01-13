package org.bundlebee.examples.fractal.osgi;

import org.bundlebee.examples.fractal.FractalApp;
import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.CommandInterpreter;

/**
 * FactralUICommand.
 *
 * @author joergp
 */
public class FactralUICommand implements CommandProvider {

    public String getHelp() {
        return "\tbbfractalui - start fractal UI\n";
    }

    public void _bbfractalui(final CommandInterpreter commandInterpreter) {
		final StringBuilder sb = new StringBuilder();
		String arg;
		while ((arg = commandInterpreter.nextArgument()) != null) {
			sb.append(arg);
			sb.append(' ');
		}

		Thread t = new Thread(new Runnable() {

			public void run() {
				FractalApp.main(null);
			}

		}, "Swing launcher thread" );
		t.start();
    }

}
