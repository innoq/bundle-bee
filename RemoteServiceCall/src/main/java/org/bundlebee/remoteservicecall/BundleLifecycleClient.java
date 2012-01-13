package org.bundlebee.remoteservicecall;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.bundlebee.remoteservicecall.BundleLifecycleClient.Method.START;
import static org.bundlebee.remoteservicecall.BundleLifecycleClient.Method.INSTALL;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class that lets you talk to a remote {@link org.bundlebee.manager.Manager}.
 * <p/>
 * Date: Dec 11, 2008
 * Time: 8:43:31 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class BundleLifecycleClient {

    private static Logger LOG = LoggerFactory.getLogger(BundleLifecycleClient.class);
    public static final String ORG_BUNDLEBEE_REMOTESERVICECALL_CONNECTTIMEOUT = "org.bundlebee.remoteservicecall.bundlelifecycleclient.connecttimeout";
    public static final String ORG_BUNDLEBEE_REMOTESERVICECALL_READTIMEOUT = "org.bundlebee.remoteservicecall.bundlelifecycleclient.readtimeout";
    private static final int DEFAULT_READ_TIMEOUT = 60000;
    private static final int DEFAULT_CONNECT_TIMEOUT = 500;
    /**
     * Default read timeout that takes system property settings into account.
     *
     * @see #ORG_BUNDLEBEE_REMOTESERVICECALL_READTIMEOUT
     */
    private static final int READ_TIMEOUT;
    /**
     * Default connect timeout that takes system property settings into account.
     *
     * @see #ORG_BUNDLEBEE_REMOTESERVICECALL_CONNECTTIMEOUT
     */
    private static final int CONNECT_TIMEOUT;
    private static final String HTTP_POST = "POST";
    
    static {
        int defConnectTimeout = DEFAULT_CONNECT_TIMEOUT;
        try {
            final String property = System.getProperty(ORG_BUNDLEBEE_REMOTESERVICECALL_CONNECTTIMEOUT, Integer.toString(DEFAULT_CONNECT_TIMEOUT));
            defConnectTimeout = Integer.parseInt(property);
            LOG.info("Default connect timeout set to " + defConnectTimeout + " ms.");
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse connect timeout: " + e.toString(), e);
        }
        CONNECT_TIMEOUT = defConnectTimeout;

        int defReadTimeout = DEFAULT_READ_TIMEOUT;
        try {
            final String property = System.getProperty(ORG_BUNDLEBEE_REMOTESERVICECALL_READTIMEOUT, Integer.toString(DEFAULT_READ_TIMEOUT));
            defReadTimeout = Integer.parseInt(property);
            LOG.info("Default read timeout set to " + defReadTimeout + " ms.");
        } catch (NumberFormatException e) {
            LOG.error("Failed to parse read timeout: " + e.toString(), e);
        }
        READ_TIMEOUT = defReadTimeout;
    }

    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private int readTimeout = DEFAULT_READ_TIMEOUT;

    public enum Method {
        START("start"), STOP("stop"), INSTALL("install"), UNINSTALL("uninstall");
        private String method;

        Method(final String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }
    }

    public BundleLifecycleClient() {
        setConnectTimeout(CONNECT_TIMEOUT);
        setReadTimeout(READ_TIMEOUT);
    }

    /**
     * Starts the bundle on the first best manager.
     * Attempts to start the bundle on the first manager in the given list and returns right
     * away should that succeed, i.e. the other manager URLs are ignored.
     *
     * @param managerURLs list of manager URLs to start the bundle on.
     * @param bundleSymbolicNameVersion bundle name / version
     * @return list with at most one manager url
     */
    public List<URL> startBundle(final Collection<URL> managerURLs, final String bundleSymbolicNameVersion) {
        if (LOG.isDebugEnabled()) LOG.debug("startBundle(" + bundleSymbolicNameVersion + ") on " + managerURLs);
        final List<URL> managersWithStartedBundle = new ArrayList<URL>();
        for (final URL url: managerURLs) {
            try {
                final int responseCodeStart = post(url, START, bundleSymbolicNameVersion);
                if (responseCodeStart == HttpURLConnection.HTTP_OK) {
                    managersWithStartedBundle.add(url);
                    break;
                }
            } catch (IOException e) {
                LOG.error(e.toString(), e);
            }
        }
        return managersWithStartedBundle;
    }


    /**
     * Installs and starts the bundle on the first best manager.
     * Attempts to install and start the bundle on the first manager in the given list and returns right
     * away should that succeed, i.e. the other manager URLs are ignored.
     *
     * @param managerURLs list of manager URLs to install and start the bundle on.
     * @param bundleSymbolicNameVersion bundle name / version
     * @return list with at most one manager url
     */
    public List<URL> installAndStartBundle(final Collection<URL> managerURLs, final String bundleSymbolicNameVersion) {
        if (LOG.isDebugEnabled()) LOG.debug("installAndStartBundle(" + bundleSymbolicNameVersion + ") on " + managerURLs);
        final List<URL> managersWithDeployedBundle = new ArrayList<URL>();
        // TODO: find a way to find the BEST manager, that is a node with low load
        for (final URL url: managerURLs) {
            try {
                final int responseCodeInstall = post(url, INSTALL, bundleSymbolicNameVersion);
                if (responseCodeInstall == HttpURLConnection.HTTP_OK) {
                    final int responseCodeStart = post(url, START, bundleSymbolicNameVersion);
                    if (responseCodeStart == HttpURLConnection.HTTP_OK) {
                        managersWithDeployedBundle.add(url);
                        break;
                    }
                }
            } catch (IOException e) {
                LOG.error(e.toString(), e);
            }
        }
        return managersWithDeployedBundle;
    }

    /**
     * Ensures that a URL ends with a slash.
     *
     * @param url url
     * @return url ending with a slash
     */
    public static URL toDirectoryURL(final URL url) throws MalformedURLException {
        if (!url.toString().endsWith("/")) return new URL(url.toString() + "/");
        return url;
    }

    /**
     * Calls a manager over HTTP.
     *
     * @param managerURL manager URL, e.g. http://myhostname:port/bundlebee/manager
     * @param method install, start, stop or uninstall
     * @param bundleSymbolicNameVersion bundle "/" version, e.g. <code>org.bundlebee.testbundle/1.0.0</code>
     * @return HTTP status code - typically 200, when things went well
     * @throws IOException if something goes wrong
     */
    public int post(final URL managerURL, final Method method, final String bundleSymbolicNameVersion) throws IOException {
        return openConnection(managerURL, method, bundleSymbolicNameVersion).getResponseCode();
    }

    /**
     * Calls a manager over HTTP.
     *
     * @param managerURL manager URL, e.g. http://myhostname:port/bundlebee/manager
     * @param method install, start, stop or uninstall
     * @param bundleSymbolicNameVersion bundle "/" version, e.g. <code>org.bundlebee.testbundle/1.0.0</code>
     * @return the open connection
     * @throws IOException if something goes wrong
     */
    public HttpURLConnection openConnection(final URL managerURL, final Method method, final String bundleSymbolicNameVersion) throws IOException {
        final URL url = new URL(toDirectoryURL(managerURL), method.getMethod() + "/" + bundleSymbolicNameVersion);
        final HttpURLConnection urlConnection = (HttpURLConnection)url.openConnection();
        urlConnection.setRequestMethod(HTTP_POST);
        urlConnection.setReadTimeout(readTimeout);
        urlConnection.setConnectTimeout(connectTimeout);
        return urlConnection;
    }

    /**
     * @see java.net.HttpURLConnection#getConnectTimeout()
     * @return timeout in ms
     */
    public int getConnectTimeout() {
        return connectTimeout;
    }

    /**
     * @param connectTimeout timeout in ms, must be positive, 0 is not permitted
     * @throws IllegalArgumentException if timeout <=0
     * @see java.net.HttpURLConnection#setConnectTimeout(int)
     */
    public void setConnectTimeout(final int connectTimeout) {
        if (connectTimeout == 0) {
            throw new IllegalArgumentException("Infinite connect timeouts are not permitted in this context.");
        }
        if (connectTimeout < 0) {
            throw new IllegalArgumentException("Negative connect timeouts are not permitted in this context.");
        }
        this.connectTimeout = connectTimeout;
    }

    /**
     * @see java.net.HttpURLConnection#getReadTimeout()
     * @return timeout in ms
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * @param readTimeout timeout in ms, must be positive, 0 is not permitted
     * @throws IllegalArgumentException if timeout <=0
     * @see java.net.HttpURLConnection#setReadTimeout(int)
     */
    public void setReadTimeout(final int readTimeout) {
        if (readTimeout == 0) {
            throw new IllegalArgumentException("Infinite read timeouts are not permitted in this context.");
        }
        if (readTimeout < 0) {
            throw new IllegalArgumentException("Negative read timeouts are not permitted in this context.");
        }
        this.readTimeout = readTimeout;
    }
}
