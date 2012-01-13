package org.bundlebee.cli.impl;

import org.bundlebee.registry.directory.Node;
import org.bundlebee.registry.impl.RegistryImpl;
import org.bundlebee.remoteservicecall.BundleLifecycleClient;
import org.eclipse.osgi.framework.console.CommandInterpreter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.BundleException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.HashSet;
import java.util.Set;

/**
 * BundleLifecycleClientCommand.
 * <p/>
 * Date: Jan 13, 2009
 * Time: 6:36:01 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public abstract class BundleLifecycleClientCommand {

    private BundleContext bundleContext;
    private BundleLifecycleClient.Method method;

    public BundleLifecycleClientCommand(final BundleContext bundleContext, final BundleLifecycleClient.Method method) {
        this.bundleContext = bundleContext;
        this.method = method;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    protected Set<URL> getManagerURLs(final CommandInterpreter commandInterpreter) {
        final Set<URL> managerURLs = new HashSet<URL>();
        String remoteNodeId = commandInterpreter.nextArgument();
        if (remoteNodeId == null) {
            managerURLs.addAll(RegistryImpl.getInstance().getGrid().getManagers());
        } else {
            final URL url = getManagerURL(commandInterpreter, remoteNodeId);
            if (url != null) managerURLs.add(url);
            while ((remoteNodeId = commandInterpreter.nextArgument()) != null)  {
                final URL additionalURL = getManagerURL(commandInterpreter, remoteNodeId);
                if (additionalURL != null) managerURLs.add(additionalURL);
            }
        }
        return managerURLs;
    }

    private URL getManagerURL(final CommandInterpreter commandInterpreter, final String remoteNodeId) {
        URL url = null;
        try {
            final Long nodeId = Long.parseLong(remoteNodeId);
            final Node node = RegistryImpl.getInstance().getGrid().getNode(nodeId);
            if (node != null) {
                if (node.getManagerURL() != null) {
                    url = node.getManagerURL();
                } else {
                    commandInterpreter.println("Node " + nodeId + " does not seem to have a manager URL.");
                }
            } else {
                commandInterpreter.println("Failed to find node with id " + nodeId);
            }
        } catch (NumberFormatException e) {
            commandInterpreter.println("Node id " + remoteNodeId + "  seems to be invalid.");
        }
        return url;
    }

    protected String getBundleName(final CommandInterpreter commandInterpreter, final String nameVersionOrBundleIdOrURL) {
        String nameVersion = nameVersionOrBundleIdOrURL;
        try {
            final long bundleId = Long.parseLong(nameVersionOrBundleIdOrURL);
            final Bundle bundle = bundleContext.getBundle(bundleId);
            if (bundle != null) {
                nameVersion = bundle.getSymbolicName() + "/" + bundle.getHeaders().get(Constants.BUNDLE_VERSION);
            }
        } catch (NumberFormatException e) {
            try {
                final URL url = new URL(nameVersionOrBundleIdOrURL);
                // install bundle in local node, this will make sure, that our repository can serve it
                // this is not the best way to do it (as we are installing when we may want to uninstall),
                // but for now the easiest. -hendrik
                final Bundle bundle = getBundleContext().installBundle(url.toString());
                nameVersion = bundle.getSymbolicName() + "/" + bundle.getHeaders().get(Constants.BUNDLE_VERSION);
            } catch (MalformedURLException e1) {
                // assume it's nameVersion
            } catch (BundleException e1) {
                commandInterpreter.println("Failed to install bundle locally: " + e1);
                nameVersion = null;
            }
        }
        return nameVersion;
    }

    public String getHelp() {
        return "\tbb" + method.getMethod() + " symbolicname/version | localBundleId | url [nodeId]* - " + method.getMethod() + "s a bundle on all or specific nodes\n";
    }

    protected void execute(final CommandInterpreter commandInterpreter) {
        final String nameVersionOrBundleIdOrURL = commandInterpreter.nextArgument();
        if (nameVersionOrBundleIdOrURL == null) {
            commandInterpreter.print(getHelp());
        } else {
            final String nameVersion = getBundleName(commandInterpreter, nameVersionOrBundleIdOrURL);
            if (nameVersion != null) {
                execute(commandInterpreter, nameVersion);
            }
        }
    }

    private void execute(final CommandInterpreter commandInterpreter, final String nameVersion) {
        final Set<URL> managerURLs = getManagerURLs(commandInterpreter);
        if (managerURLs.isEmpty()) {
            commandInterpreter.println("No valid nodes specified.");
        } else {
            commandInterpreter.println(method + "ing " + nameVersion + " ...");
            for (final URL url:managerURLs) {
                commandInterpreter.print("\ton " + url + ": ");
                try {
                    final HttpURLConnection urlConnection = new BundleLifecycleClient().openConnection(url, method, nameVersion);
                    final int responseCode = urlConnection.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        commandInterpreter.println("SUCCESS");
                    } else {
                        commandInterpreter.println("FAILURE - Code: " + responseCode);
                        printStream(commandInterpreter, urlConnection);
                    }
                } catch (IOException e) {
                    commandInterpreter.println("FAILURE");
                    commandInterpreter.printStackTrace(e);
                }
            }
            commandInterpreter.println("Operation completed.");
        }
    }

    private void printStream(final CommandInterpreter commandInterpreter, final HttpURLConnection urlConnection) {
        InputStream inputStream = null;
        try {
            inputStream = urlConnection.getErrorStream();
            if (inputStream == null) inputStream = urlConnection.getInputStream();
            if (inputStream != null) {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "ASCII"));
                String line;
                while ((line = reader.readLine()) != null) {
                    commandInterpreter.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
