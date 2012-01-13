package org.bundlebee.testbundle.cli;

import org.eclipse.osgi.framework.console.CommandProvider;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.bundlebee.testbundle.TestBundle;
import org.bundlebee.testbundle.impl.TestBundleReturn;

/**
 * EchoCommand.
 *
 * @author joergp
 */
public class EchoCommand implements CommandProvider {

    private final TestBundle mTestBundle;

    public EchoCommand(TestBundle b) {
        mTestBundle = b;
    }

    public String getHelp() {
        return "\tbbecho <message> - echos the given message on this or another node\n";
    }

    public void _bbecho(final CommandInterpreter commandInterpreter) {
		final StringBuilder sb = new StringBuilder();
		String arg;
		while ((arg = commandInterpreter.nextArgument()) != null) {
			sb.append(arg);
			sb.append(' ');
		}
		TestBundleReturn ret = mTestBundle.echo(sb.toString().trim());
		System.out.println( "Returned: " + ret.getMessage() );
    }

}
