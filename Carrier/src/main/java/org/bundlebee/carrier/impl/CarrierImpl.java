package org.bundlebee.carrier.impl;

import org.bundlebee.carrier.Carrier;
import org.bundlebee.carrier.DeploymentException;
import org.bundlebee.carrier.ResourceNotFoundException;
import org.bundlebee.carrier.UnsatisfiedRequirementsException;
import static org.bundlebee.carrier.impl.Activator.BUNDLE_MARKER;
import org.bundlebee.registry.impl.RegistryImpl;
import org.osgi.framework.BundleContext;
import org.osgi.service.obr.*;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Carrier implementation. Uses org.osgi.service.obr OBR. 
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class CarrierImpl implements Carrier {


    private static final Logger LOG = LoggerFactory.getLogger(CarrierImpl.class);
    private static final String DEFAULT_REPOSITORY_ROOT = new File("repository").toString();
    /**
     * System property that allows configuration of local Felix OBR root.
     * E.g. <code>~/.m2/repository</code>
     */
    public static final String ORG_BUNDLEBEE_REPOSITORY_ROOT = "org.bundlebee.repository.root";
    /**
     * System property that allows comma separated list of additional repository URLs.
     * URLs have to point to the Felix OBR conform repository file, e.g.
     * <code>http://felix.apache.org/obr/releases.xml</code>
     */
    public static final String ORG_BUNDLEBEE_REPOSITORY_URLS = "org.bundlebee.repository.urls";
    private ServiceTracker repositoryAdminTracker;
    private ServiceTracker repositoryTracker;
    private Map<URL, Long> lastModifiedMap = new ConcurrentHashMap<URL, Long>();

    public CarrierImpl(final BundleContext bundleContext) {
        this.repositoryAdminTracker = new ServiceTracker(bundleContext, org.osgi.service.obr.RepositoryAdmin.class.getName(), null);
        this.repositoryAdminTracker.open();
        this.repositoryTracker = new ServiceTracker(bundleContext, org.bundlebee.repository.Repository.class.getName(), null);
        this.repositoryTracker.open();
    }

    public void stop() {
        this.repositoryAdminTracker.close();
        this.repositoryTracker.close();
    }

    public void deploy(final String filterExpression, final boolean start) throws DeploymentException {
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "deploy(\"" + filterExpression + "\", " + start + ")");
        final RepositoryAdmin repositoryAdmin = (RepositoryAdmin) repositoryAdminTracker.getService();
        if (repositoryAdmin == null) {
            throw new DeploymentException("RepositoryAdmin not available. Make sure org.osgi.service.obr is deployed and running.");
        }
        addOrRefreshLocalRepository(repositoryAdmin);
        addOrRefreshAdditionalRepositories(repositoryAdmin);
        addOrRefreshAvailableRepositories(repositoryAdmin);

        listAllAvailableResources(repositoryAdmin);
        final Resource[] resources = repositoryAdmin.discoverResources(filterExpression);
        if (resources != null && resources.length > 0) {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Found " + resources.length + " resources.");
            for (final Resource resource:resources) {
                if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Resource: " + resource.getId());
            }
            // we try to use a local resource
            final Resource firstLocalResource = getFirstLocalResource(resources);
            final Resource resource = firstLocalResource == null ? resources[0] : firstLocalResource;
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Resolving "  + resource.getId() + ", " + resource.getURL());
            final Resolver resolver = repositoryAdmin.resolver();
            resolver.add(resource);
            final boolean success = resolver.resolve();
            if (success) {
                if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Deploying "  + resource.getId() + ", start=" + start);
                resolver.deploy(start);
                deployRemoteResourcesToLocalRepository(resolver);
            } else {
                if (LOG.isWarnEnabled()) LOG.warn(BUNDLE_MARKER, "Failed to resolve " + resource.getId());
                final Requirement[] unsatisfiedRequirements = resolver.getUnsatisfiedRequirements();
                for (final Requirement requirement:unsatisfiedRequirements) {
                    if (LOG.isWarnEnabled()) LOG.warn(BUNDLE_MARKER, "Unable to resolve: " + requirement.getName() + " " + requirement.getFilter());
                }
                throw new UnsatisfiedRequirementsException(resource, resolver.getUnsatisfiedRequirements());
            }
        } else {
            if (LOG.isErrorEnabled()) LOG.error(BUNDLE_MARKER, "Failed to find resources.");
            throw new ResourceNotFoundException(filterExpression);
        }
    }

    private boolean isLocal(final Resource resource) {
        return resource.getURL().toString().startsWith("file:");
    }

    private Resource getFirstLocalResource(final Resource[] resources) {
        for (final Resource resource:resources) {
            if (isLocal(resource)) return resource;
        }
        return null;
    }

    private void deployRemoteResourcesToLocalRepository(final Resolver resolver) {
        // show bundles that probably have been deployed...
        final Collection<Resource> resources = new HashSet<Resource>();
        resources.addAll(Arrays.asList(resolver.getRequiredResources()));
        resources.addAll(Arrays.asList(resolver.getAddedResources()));
        resources.addAll(Arrays.asList(resolver.getOptionalResources()));
        deployResourcesToLocalRepository(resources);
    }

    private void deployResourcesToLocalRepository(final Collection<Resource> resources) {
        for (final Resource resource : resources) {
            LOG.debug(BUNDLE_MARKER, "Resource " + resource + " has url " + resource.getURL());
            // only deploy non-local resources
            if (isLocal(resource)) {
                try {
                    final org.bundlebee.repository.Repository repository = (org.bundlebee.repository.Repository)repositoryTracker.getService();
                    if (repository != null) {
                        // we have to do this, because Felix OBR likes to install stuff with funky locations that
                        // do not let us retrieve the actual file. so the bundle listener we have in the
                        // repository is worthless, as it is unable to use the location to fetch the actual file.
                        // -hendrik
                        repository.install(resource.getURL(), resource.getSymbolicName() + "_" + resource.getVersion() + ".jar");
                    } else {
                        LOG.warn(BUNDLE_MARKER, "Failed to obtain BundleBee repository.");
                    }
                } catch (Exception e) {
                    LOG.error("Failed to install resource from " + resource.getURL() + " in local BundleBee repository.", e);
                }
            }
        }
    }

    /**
     * Adds repositories from the registry.
     *
     * @param repositoryAdmin repository Admin
     */
    private void addOrRefreshAvailableRepositories(final RepositoryAdmin repositoryAdmin) {
        for (final URL url : RegistryImpl.getInstance().getGrid().getRepositories()) {
            try {
                if (hasBeenModified(url) || !isRepositoryAdded(repositoryAdmin, url)) {
                    if (LOG.isDebugEnabled()) LOG.debug("Adding/refreshing repository " + url + " from registry.");
                    repositoryAdmin.addRepository(url);
                }
            } catch (Exception e) {
                LOG.error(BUNDLE_MARKER, "Failed to add registry registered repository located at "
                        + url + "\nCause: " + e, e);
            }
        }
    }

    /**
     * Adds additional repositories configured in system property {@link #ORG_BUNDLEBEE_REPOSITORY_URLS}.
     * This can be any kind of Felix/BIndex conform OBR URL.
     * <p>
     * Each time this method is called, the OBR repository file is re-fetched.
     *
     * @param repositoryAdmin repository Admin
     */
    private void addOrRefreshAdditionalRepositories(final RepositoryAdmin repositoryAdmin) {
        final String repositoryURLs = System.getProperty(ORG_BUNDLEBEE_REPOSITORY_URLS);
        if (repositoryURLs != null) {
            for (final String urlString : repositoryURLs.split(",")) {
                try {
                    final URL url = new URL(urlString);
                    if (hasBeenModified(url) || !isRepositoryAdded(repositoryAdmin, url)) {
                        if (LOG.isDebugEnabled()) LOG.debug("Adding/refreshing addition repository at " + url);
                        repositoryAdmin.addRepository(url);
                    }
                } catch (Exception e) {
                    LOG.error(BUNDLE_MARKER, "Failed to add/refresh system property configured repository located at "
                            + urlString + "\nCause: " + e, e);
                }
            }
        } else {
            if (LOG.isInfoEnabled()) {
                LOG.info(BUNDLE_MARKER, "No additional repositories configured via system property "
                        + ORG_BUNDLEBEE_REPOSITORY_URLS + ".");
            }
        }
    }

    /**
     * Adds the local repository. This should be the same repository that the local repository bundle is
     * serving bundles from.
     * <p>
     * Each time this method is called, the local OBR repository file is re-read.
     *
     * @param repositoryAdmin repository Admin
     */
    private void addOrRefreshLocalRepository(final RepositoryAdmin repositoryAdmin) {
        try {
            final URL url = new File(getRepositoryRoot().toString() + "/repository.xml").toURL();
            if (hasBeenModified(url) || !isRepositoryAdded(repositoryAdmin, url)) {
                LOG.debug(BUNDLE_MARKER, "Adding/refreshing local repository " + url.toString());
                repositoryAdmin.addRepository(url);
            }
        } catch (Exception e) {
            LOG.error(BUNDLE_MARKER, "Failed to add local repository: " + e, e);
        }
    }

    private static File getRepositoryRoot() throws FileNotFoundException {
        // Note: same mechanism is used in Repository! -hendrik
        String repositoryPath = System.getProperty(ORG_BUNDLEBEE_REPOSITORY_ROOT, DEFAULT_REPOSITORY_ROOT).trim();
        if (repositoryPath != null && repositoryPath.startsWith("~")) {
            repositoryPath = System.getProperty("user.home") + repositoryPath.substring(1);
        }
        final File repositoryRoot = new File(repositoryPath);
        if (!repositoryRoot.exists()) {
            throw new FileNotFoundException("Repository root " + repositoryRoot + " does not exist.");
        }
        if (!repositoryRoot.canRead()) {
            throw new FileNotFoundException("Repository root " + repositoryRoot + " is not readable.");
        }
        return repositoryRoot;
    }

    private void listAllAvailableResources(final RepositoryAdmin repositoryAdmin) {
        final Repository[] repositories = repositoryAdmin.listRepositories();
        for (final Repository repository:repositories) {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Repository at " + repository.getURL());
            listAllAvailableResources(repository);
        }
    }

    private void listAllAvailableResources(final Repository repository) {
        final Resource[] resources = repository.getResources();
        if (resources != null) {
            for (final Resource resource: resources) {
                if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Resource: " + resource.getPresentationName()
                        + " - " + resource.getSymbolicName() + " - " + resource.getId());
            }
        }
    }

    /**
     * Check whether the content of a URL has been modified since the last time we checked.
     *
     * @param url url
     * @return true or false
     */
    private boolean hasBeenModified(final URL url) {
        final Long cachedLastModified = lastModifiedMap.get(url);
        final Long realLastModified = getLastModified(url);
        if (!realLastModified.equals(cachedLastModified)) {
            if (realLastModified != 0L) {
                if (LOG.isDebugEnabled()) LOG.debug("URL at " + url + " has been modified.");
                lastModifiedMap.put(url, realLastModified);
            }
            return true;
        }
        return false;
    }

    /**
     * Checks date of last modification of a resource.
     * For HTTP we are using the HEAD method.
     *
     * @param url url
     * @return timestamp in ms since 1970 or 0, if the date of last modification couldn't be determined
     */
    private Long getLastModified(final URL url) {
        try {
            final URLConnection urlConnection = url.openConnection();
            urlConnection.setConnectTimeout(500);
            urlConnection.setReadTimeout(500);
            if (urlConnection instanceof HttpURLConnection) {
                ((HttpURLConnection)urlConnection).setRequestMethod("HEAD");
            }
            return urlConnection.getLastModified();
        } catch (IOException e) {
            LOG.error("Failed to get date of last modification for " + url, e);
        }
        return 0L;
    }

    private boolean isRepositoryAdded(final RepositoryAdmin repositoryAdmin, final URL url) {
        for (final Repository repository:repositoryAdmin.listRepositories()) {
            if (url.equals(repository.getURL())) return true;
        }
        return false;
    }
}
