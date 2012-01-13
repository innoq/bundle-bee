package org.bundlebee.cli.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * BundleBee Target command.
 * <p/>
 * Date: Jan 13, 2010
 * Time: 12:24:21 AM
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Haussleiter</a>
 */
public class MethodsCommand implements CommandProvider {
	
	private static final String INSTRUMENTEDMETHODS = "org.bundlebee.weaver.instrumentedmethods";


    public void _bbmethods(final CommandInterpreter commandInterpreter) {
        final String argument = commandInterpreter.nextArgument();
        if (argument == null) {
            commandInterpreter.println(getHelp());
        } else {
            if (argument.equals("get")) {
                commandInterpreter.println();
                println(commandInterpreter);
                commandInterpreter.println();
            }
            if (argument.equals("set")) {
                final String propSet = commandInterpreter.nextArgument();
                if (propSet == null) {
                    commandInterpreter.println(getHelp());
                } else {
                    commandInterpreter.println();
                    set(commandInterpreter, propSet);
                    commandInterpreter.println();
                }
            }
            if (argument.equals("load")) {
                final String url = commandInterpreter.nextArgument();
                if (url == null) {
                    commandInterpreter.println(getHelp());
                } else {
                    commandInterpreter.println();
                    load(commandInterpreter, url);
                    commandInterpreter.println();
                }
            }
        }
    }

    private void println(final CommandInterpreter ci) {
        final String instrumented = System.getProperty(INSTRUMENTEDMETHODS);
        if (instrumented != null) {
            ci.println("org.bundlebee.weaver.instrumentedmethods=" + instrumented);
        }
    }

    private void set(final CommandInterpreter ci, final String propSet){
        final String prop[] = propSet.split("=");
        if(prop.length == 2 && prop[0].trim().equals(INSTRUMENTEDMETHODS)){
            System.setProperty(INSTRUMENTEDMETHODS, prop[1]);
        }
        ci.println("org.bundlebee.weaver.instrumentedmethods="+System.getProperty(INSTRUMENTEDMETHODS));
    }

    private void load(final CommandInterpreter ci, final String url) {
        final String instrumented = System.getProperty(INSTRUMENTEDMETHODS);
        if (instrumented != null) {
            ci.println("loading from: " + url);
        }
        try {
            final URL propsUrl = new URL(url);
            Properties properties = new Properties();
            properties.load(new FileInputStream(new File(propsUrl.getFile())));
            System.setProperty(INSTRUMENTEDMETHODS, properties.getProperty( INSTRUMENTEDMETHODS, instrumented));
            ci.println(INSTRUMENTEDMETHODS+"="+System.getProperty(INSTRUMENTEDMETHODS));
        } catch (MalformedURLException ex) {
            ci.println(url + " is not a valid URL.");
        } catch (FileNotFoundException fnfe) {
            ci.println("File not found: "+url);
        } catch (IOException ioe) {
            ci.println("Could not open or read: "+url);
        }
    }

    public String getHelp() {
        return "\tbbmethods | get | set <property>  | load <java.net.URL> |  - gets/sets the istrumentedMethods property of the BundleBee grid node.\n";
    }
}
