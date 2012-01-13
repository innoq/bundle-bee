package org.bundlebee.manager.impl;

import org.bundlebee.carrier.Carrier;
import org.bundlebee.manager.Manager;
import static org.bundlebee.manager.impl.Activator.BUNDLE_MARKER;
import org.bundlebee.registry.impl.RegistryImpl;
import org.osgi.framework.*;
import org.osgi.service.http.HttpContext;
import org.osgi.service.http.HttpService;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;


/**
 * Manages the lifecycles of bundles on this node and publishes their state to the registry.
 * Note, that the interface of this class is not exposed through Java, but HTTP.
 * <p/>
 * Date: Dec 1, 2008
 * Time: 8:36:35 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ManagerImpl implements Manager {


    private static final Logger LOG = LoggerFactory.getLogger(ManagerImpl.class);
    public static final String ORG_BUNDLEBEE_MANAGER_ALIAS = "org.bundlebee.manager.alias";
    private static final String DEFAULT_BUNDLEBEE_MANAGER_ALIAS = "/bundlebee/manager";
    private static final String ORG_BUNDLEBEE_REGISTRY_LOCALADDRESS = "org.bundlebee.registry.localaddress";
    private static final String ORG_BUNDLEBEE_MANAGER_LOCALADDRESS = "org.bundlebee.manager.localaddress";
    private BundleContext bundleContext;
    private ServiceTracker carrierTracker;
    private ServiceTracker httpServiceTracker;
    private Integer port = null;
    private BundleListener bundleListener;
    private String localAddress = System.getProperty(ORG_BUNDLEBEE_MANAGER_LOCALADDRESS,
            System.getProperty(ORG_BUNDLEBEE_REGISTRY_LOCALADDRESS, InetAddress.getLocalHost().getHostAddress()));

    public ManagerImpl(final BundleContext bundleContext) throws InvalidSyntaxException, IOException {
        this.bundleContext = bundleContext;
        this.carrierTracker = new ServiceTracker(bundleContext, Carrier.class.getName(), null);
        this.carrierTracker.open();
        this.httpServiceTracker = new HttpServiceTracker(bundleContext,
                //bundleContext.createFilter("(&(objectClass=" + HttpService.class.getName() + ")(org.osgi.service.http.port=3054))"),
                this);
        this.httpServiceTracker.open();
        this.bundleListener = new BundleListener() {
            public void bundleChanged(final BundleEvent event) {
                registerBundle(event.getBundle());
            }
        };
        bundleContext.addBundleListener(bundleListener);
        registerExistingBundles();
    }

    private void registerExistingBundles() {
        for (final Bundle bundle : bundleContext.getBundles()) {
            registerBundle(bundle);
        }
    }

    private void unregisterExistingBundles() {
        for (final Bundle bundle : bundleContext.getBundles()) {
            unregisterBundle(bundle);
        }
    }

    private void registerBundle(final Bundle bundle) {
        RegistryImpl.getInstance().registerBundle(getSymbolicNameVersion(bundle), bundle.getState());
    }

    private String getSymbolicNameVersion(final Bundle bundle) {
        return bundle.getSymbolicName() + "/" + bundle.getHeaders().get(Constants.BUNDLE_VERSION);
    }

    private void unregisterBundle(final Bundle bundle) {
        RegistryImpl.getInstance().unregisterBundle(getSymbolicNameVersion(bundle));
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void stop() {
        this.httpServiceTracker.close();
        this.carrierTracker.close();
        bundleContext.removeBundleListener(bundleListener);
        unregisterExistingBundles();
    }

    public URL getManagerURL() {
        if (port == null) {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Port is null");
            return null;
        }
        try {
            return new URL("http://" + localAddress + ":" + port + getManagerAlias());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String getManagerAlias() {
        return System.getProperty(ORG_BUNDLEBEE_MANAGER_ALIAS, DEFAULT_BUNDLEBEE_MANAGER_ALIAS);
    }



    /**
     *
     * @param symbolicName symbolicname as defined in the manifest
     * @param version bundle version as defined in the manifest
     * @throws org.bundlebee.carrier.DeploymentException if deployment fails
     */
    public void install(final String symbolicName, final String version) throws BundleException {
        if (!isInstalled(symbolicName, version)) {
            if (LOG.isDebugEnabled()) LOG.debug("Trying to install bundle " + symbolicName + "/" + version);
            // get carrier service and install.
            final Carrier carrier = (Carrier)carrierTracker.getService();
            final String expression = "(&(symbolicname=" + symbolicName + ")(version=" + version + "))";
            carrier.deploy(expression, false);
            if (LOG.isDebugEnabled()) LOG.debug("Installed bundle " + symbolicName + "/" + version + " with no exception.");
        } else {
            if (LOG.isDebugEnabled()) LOG.debug("Did not install bundle " + symbolicName
                    + "/" + version + ", because it is already installed.");
        }
    }

    public void uninstall(final String symbolicName, final String version) throws BundleException {
        final Bundle bundle = getBundle(symbolicName, version);
        if (bundle != null) {
            if (LOG.isDebugEnabled()) LOG.debug("Trying to uninstall bundle " + symbolicName + "/" + version);
            bundle.uninstall();
            if (LOG.isDebugEnabled()) LOG.debug("Uninstalled bundle " + symbolicName + "/" + version + " with no exception.");
        } else {
            if (LOG.isDebugEnabled()) LOG.debug("Did not uninstall bundle " + symbolicName
                    + "/" + version + ", because it does not seem to be installed.");
        }
    }

    public void start(final String symbolicName, final String version) throws BundleException {
        Bundle bundle = getBundle(symbolicName, version);
        if (bundle == null) {
            if (LOG.isDebugEnabled()) LOG.debug("Bundle " + symbolicName + "/" + version
                    + " cannot be started, as it is not installed, yet. Attempting installation.");
            install(symbolicName, version);
            bundle = getBundle(symbolicName, version);
        }
        if (bundle != null) {
            if (bundle.getState() != Bundle.ACTIVE) {
                if (LOG.isDebugEnabled()) LOG.debug("Trying to start bundle " + symbolicName + "/" + version);
                bundle.start();
                if (LOG.isDebugEnabled()) LOG.debug("Started bundle " + symbolicName + "/" + version + " with no exception.");
            } else {
                if (LOG.isDebugEnabled()) LOG.debug("No need to start bundle " + symbolicName + "/" + version
                        + " as it is already active.");
            }
        } else {
            if (LOG.isDebugEnabled()) LOG.debug("Did not start bundle " + symbolicName
                    + "/" + version + ", because it is not installed.");
        }
    }

    public void stop(final String symbolicName, final String version) throws BundleException {
        final Bundle bundle = getBundle(symbolicName, version);
        if (bundle != null) {
            if (LOG.isDebugEnabled()) LOG.debug("Trying to stop bundle " + symbolicName + "/" + version);
            bundle.stop();
            if (LOG.isDebugEnabled()) LOG.debug("Stopped bundle " + symbolicName + "/" + version + " with no exception.");
        } else {
            if (LOG.isDebugEnabled()) LOG.debug("Did not stop bundle " + symbolicName
                    + "/" + version + ", because it is not installed.");
        }
    }

    private boolean isInstalled(final String symbolicName, final String version) {
        return getBundle(symbolicName, version) != null;
    }

    private Bundle getBundle(final String symbolicName, final String version) {
        final Bundle[] installedBundles = bundleContext.getBundles();
        Bundle bundle = null;
        for (final Bundle installedBundle:installedBundles) {
            final String installedSymbolicname = installedBundle.getSymbolicName();
            final Object installedVersion = installedBundle.getHeaders().get(Constants.BUNDLE_VERSION);
            if (symbolicName.equals(installedSymbolicname) && version.equals(installedVersion)) {
                bundle = installedBundle;
                break;
            }
        }
        return bundle;
    }

    /**
     * Tracks the existence and availability of the HttpService and un/installs our
     * method and lifecycle handler servlets accordingly.
     * Also un/registers the manager with the distr. registry.
     *
     * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
     * @see org.bundlebee.manager.impl.BundleLifecycleServlet
     * @see org.bundlebee.manager.impl.ServiceMethodCallServlet
     * @see RegistryImpl#registerManager(java.net.URL)
     */
    private static class HttpServiceTracker extends ServiceTracker {

        private static final int DEFAULT_PORT = 8080;
        private ManagerImpl manager;

        public HttpServiceTracker(final BundleContext bundleContext, final ManagerImpl manager) {
            super(bundleContext, HttpService.class.getName(), null);
            this.manager = manager;
        }

		@Override
        public Object addingService(final ServiceReference serviceReference) {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "addingService " + serviceReference);
            // register servlets
            final HttpService httpService = (HttpService) manager.getBundleContext().getService(serviceReference);
            Integer port = (Integer)serviceReference.getProperty("http.port");
            // is this right? - hendrik
            if (port == null) manager.port = DEFAULT_PORT;
            else manager.port = port;
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "httpService " + httpService);
            final HttpContext defaultHttpContext = httpService.createDefaultHttpContext();
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "defaultHttpContext " + defaultHttpContext);
            try {
                httpService.registerServlet(manager.getManagerAlias() + "/install", new BundleLifecycleServlet() {
                    public void post(final String symbolicName, final String version) throws BundleException {
                        manager.install(symbolicName, version);
                    }
                }, null, defaultHttpContext);

                httpService.registerServlet(manager.getManagerAlias() + "/uninstall", new BundleLifecycleServlet() {
                    public void post(final String symbolicName, final String version) throws BundleException {
                        manager.uninstall(symbolicName, version);
                    }
                }, null, defaultHttpContext);

                httpService.registerServlet(manager.getManagerAlias() + "/start", new BundleLifecycleServlet() {
                    public void post(final String symbolicName, final String version) throws BundleException {
                        manager.start(symbolicName, version);
                    }
                }, null, defaultHttpContext);

                httpService.registerServlet(manager.getManagerAlias() + "/stop", new BundleLifecycleServlet() {
                    public void post(final String symbolicName, final String version) throws BundleException {
                        manager.stop(symbolicName, version);
                    }
                }, null, defaultHttpContext);

                httpService.registerServlet(manager.getManagerAlias() + "/service", new ServiceMethodCallServlet(manager),
                        null, defaultHttpContext);

                registerAtDistributedRegistry();
            } catch (Exception e) {
                e.printStackTrace();
            }
            return httpService;
        }

        private void registerAtDistributedRegistry() {
            final URL managerURL = manager.getManagerURL();
            if (managerURL != null) RegistryImpl.getInstance().registerManager(managerURL);
        }

		@Override
        public void modifiedService(final ServiceReference serviceReference, final Object service) {
            // do nothing?
        }

		@Override
        public void removedService(final ServiceReference serviceReference, final Object service) {
            unregisterFromDistributedRegistry();
            manager.port = null;
            final HttpService httpService = (HttpService)service;
            httpService.unregister(manager.getManagerAlias() + "/install");
            httpService.unregister(manager.getManagerAlias() + "/uninstall");
            httpService.unregister(manager.getManagerAlias() + "/start");
            httpService.unregister(manager.getManagerAlias() + "/stop");
            httpService.unregister(manager.getManagerAlias() + "/service");
        }

        private void unregisterFromDistributedRegistry() {
            final URL managerURL = manager.getManagerURL();
            if (managerURL != null) RegistryImpl.getInstance().unregisterManager(managerURL);
        }

		@Override
        public synchronized void close() {
            manager.port = null;
            unregisterFromDistributedRegistry();
            super.close();
        }

    }

}
