package org.bundlebee.repository.impl;

import org.bundlebee.registry.impl.RegistryImpl;
import org.bundlebee.repository.Repository;
import static org.bundlebee.repository.impl.Activator.BUNDLE_MARKER;
import org.osgi.framework.*;
import org.osgi.impl.bundle.obr.resource.BundleInfo;
import org.osgi.impl.bundle.obr.resource.ResourceImpl;
import org.osgi.impl.bundle.obr.resource.Tag;
import org.osgi.service.http.HttpService;
import org.osgi.service.http.NamespaceException;
import org.osgi.service.obr.Resource;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.*;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * Repository Impl.
 * <p/>
 * Date: Jan 8, 2009
 * Time: 10:27:19 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class RepositoryImpl implements Repository {

    private static Logger LOG = LoggerFactory.getLogger(RepositoryImpl.class);
    /**
     * System property that allows configuration of local Felix OBR root.
     * E.g. <code>~/.m2/repository</code>
     */
    public static final String ORG_BUNDLEBEE_REPOSITORY_ROOT = "org.bundlebee.repository.root";
    public static final String ORG_BUNDLEBEE_REPOSITORY_ALIAS = "org.bundlebee.repository.alias";
    private static final String ORG_BUNDLEBEE_REGISTRY_LOCALADDRESS = "org.bundlebee.registry.localaddress";
    private static final String ORG_BUNDLEBEE_REPOSITORY_LOCALADDRESS = "org.bundlebee.repository.localaddress";
    private static final String JAR_FILE_EXTENSION = ".jar";
    private static final String DEFAULT_REPOSITORY_ROOT = new File("repository").toString();
    private static final String DEFAULT_BUNDLEBEE_REPOSITORY_ALIAS = "/bundlebee/repository";
    private static final int DEFAULT_PORT = 8080;
    private static final String REPOSITORY_XML = "repository.xml";
    private String localAddress = System.getProperty(ORG_BUNDLEBEE_REPOSITORY_LOCALADDRESS,
            System.getProperty(ORG_BUNDLEBEE_REGISTRY_LOCALADDRESS, InetAddress.getLocalHost().getHostAddress()));
    private ServiceTracker httpServiceTracker;
    private Integer port;
    private BundleListener bundleListener;
    private BundleContext bundleContext;


    public RepositoryImpl(final BundleContext bundleContext) throws Exception {
        this.bundleContext = bundleContext;
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Starting BundleBee repository...");
        final File repositoryRoot = getRepositoryRootDir();
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Repository root : " + repositoryRoot);
        final String repositoryAlias = getRepositoryAlias();
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Repository alias: " + repositoryAlias);
        indexRepository(repositoryRoot);
        httpServiceTracker = new HttpServiceTracker(bundleContext, repositoryRoot, repositoryAlias);
        httpServiceTracker.open();

        this.bundleListener = new BundleListener() {
            public void bundleChanged(final BundleEvent event) {
                final List<Bundle> list = new ArrayList<Bundle>();
                list.add(event.getBundle());
                try {
                    install(list);
                } catch (Exception e) {
                    LOG.error(e.toString(), e);
                }
            }
        };
        bundleContext.addBundleListener(bundleListener);
        try {
            install(Arrays.asList(bundleContext.getBundles()));
        } catch (Exception e) {
            LOG.error(e.toString(), e);
        }
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "BundleBee repository started.");
    }

    // TODO: we need to add a bundlelistener that copies all locally installed bundles that have a reasonable URL
    // over to our own repo.

    public void stop() {
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Stopping BundleBee repository...");
        httpServiceTracker.close();
        bundleContext.removeBundleListener(bundleListener);
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "BundleBee repository stopped.");
    }

    public synchronized void install(final File resourceFile) throws Exception {
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Installing resource from path " + resourceFile.getAbsolutePath());
        if (!resourceFile.toString().toLowerCase().endsWith(JAR_FILE_EXTENSION)) throw new IllegalArgumentException("Bundle filename must be a JAR.");

        // put in local directory
        final File rootFile = new File(getRepositoryRootDir().getAbsolutePath() + "/");
        final File to = new File(rootFile, resourceFile.getName());
        if (!to.getCanonicalFile().equals(resourceFile.getCanonicalFile())) {
            copy(resourceFile, to);
            indexRepository(rootFile);
        } else {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Resource " + resourceFile + " is already in this repository.");
        }
    }

    public synchronized void install(final URL resourceURL, final String name) throws Exception {
        if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Installing resource from URL " + resourceURL);
        if (!name.toLowerCase().endsWith(JAR_FILE_EXTENSION)) throw new IllegalArgumentException("Bundle filename must be a JAR.");

        // put in local directory
        final File rootFile = new File(getRepositoryRootDir().getAbsolutePath() + "/");
        final File to = new File(rootFile, name);
        if (!to.exists()) {
            copy(resourceURL, to);
            indexRepository(rootFile);
        } else {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "A resource with the same name as " + name + " already exists.");
        }
    }

    private synchronized void install(final Collection<Bundle> bundles) throws Exception {
        // put in local directory
        final File rootFile = new File(getRepositoryRootDir().getAbsolutePath() + "/");
        boolean installedNewFile = false;
        for (final Bundle bundle : bundles) {
            final String name = bundle.getSymbolicName() + "_" + bundle.getHeaders().get(Constants.BUNDLE_VERSION) + JAR_FILE_EXTENSION;
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Installing resource named " + name + " from location " + bundle.getLocation());
            String location = bundle.getLocation();
            location = removeInitialReferenceProtocol(location);
            try {
                final URL locationURL = new URL(location);
                final File to = new File(rootFile, name);
                if (!to.exists()) {
                    copy(locationURL, to);
                    installedNewFile = true;
                } else {
                    if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "A resource with the same name as " + name + " already exists.");
                }
            } catch (MalformedURLException e) {
                LOG.info(BUNDLE_MARKER, "Unable to copy bundle from location " + bundle.getLocation() + ": " + e.toString());
            } catch (Exception e) {
                LOG.error(BUNDLE_MARKER, e.toString(), e);
            }
        }
        if (installedNewFile) {
            indexRepository(rootFile);
        }
    }

    private String removeInitialReferenceProtocol(String location) {
        if (location != null && location.startsWith("initial@reference:") && location.endsWith("/")) {
            location = location.substring("initial@reference:".length(), location.length() - 1);
        } else if (location != null && location.startsWith("initial@reference:")) {
            location = location.substring("initial@reference:".length());
        }
        return location;
    }

    /**
     * Run bindex over whole the repository and write the resulting file to <code>repository.xml</code>.
     *
     * @param root repository root
     * @throws Exception if things go wrong.
     * @see <a href="http://www.osgi.org/Repository/BIndex">http://www.osgi.org/Repository/BIndex</a>
     */
    private void indexRepository(final File root) throws Exception {
        final URL rootURL = root.toURL();
        final org.osgi.impl.bundle.obr.resource.RepositoryImpl osgiRepository
                = new org.osgi.impl.bundle.obr.resource.RepositoryImpl(rootURL);
        final List<Resource> resources = getRepositoryResources(root, osgiRepository);
        final Tag xmlRepository = buildXmlRepository(resources);
        final File tempRepositoryFile = new File(getRepositoryRootDir(), REPOSITORY_XML + ".tmp");
        final File repositoryFile = new File(getRepositoryRootDir(), REPOSITORY_XML);
        final File bakRepositoryFile = new File(getRepositoryRootDir(), REPOSITORY_XML + ".bak");
        // write to temp
        writeRepository(xmlRepository, tempRepositoryFile);
        // make current backup
        repositoryFile.renameTo(bakRepositoryFile);
        // make temp current
        tempRepositoryFile.renameTo(repositoryFile);
    }

    /**
     * Returns the resources in our own local repository.
     * All jars in the repository directory count as resources. We do not recurse.
     *
     * @param rootFile repository root
     * @param osgiRepository osgi repository representation of our repository
     * @return list of resources containing all jars/bundles in the repository directory.
     * @throws Exception if something goes wrong.
     */
    private List<Resource> getRepositoryResources(final File rootFile,
                                                    final org.osgi.impl.bundle.obr.resource.RepositoryImpl osgiRepository) throws Exception {
        final File[] jarFiles = rootFile.listFiles(new FileFilter() {
            public boolean accept(final File pathname) {
                return pathname.getName().toLowerCase().endsWith(JAR_FILE_EXTENSION);
            }
        });
        final List<Resource> resources = new ArrayList<Resource>();
        for (final File jarFile:jarFiles) {
            if (LOG.isDebugEnabled()) LOG.debug("Adding resource " + jarFile + " to " + REPOSITORY_XML + "...");
            final BundleInfo info = new BundleInfo(osgiRepository, jarFile);
            final ResourceImpl resource = info.build();
            resource.setURL(jarFile.toURL());
            resources.add(resource);
        }
        Collections.sort(resources, new SymbolicNameComparator());
        return resources;
    }

    private Tag buildXmlRepository(final List<Resource> resources) {
        Tag xmlRepository = new Tag("repository");
        xmlRepository.addAttribute("lastmodified", new Date());
        xmlRepository.addAttribute("name", "BundleBee Repository " + RegistryImpl.getInstance().getNodeId());

        for (final Object resource : resources) {
            final ResourceImpl resourceImpl = (ResourceImpl) resource;
            xmlRepository.addContent(resourceImpl.toXML());
        }
        return xmlRepository;
    }

    private void writeRepository(final Tag xmlRepository, final File repositoryFile) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        final PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
        pw.println("<?xml version='1.0' encoding='utf-8'?>");
        pw.println("<?xml-stylesheet type='text/xsl' href='http://www2.osgi.org/www/obr2html.xsl'?>");

        xmlRepository.print(0, pw);
        pw.close();
        byte buffer[] = out.toByteArray();
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(repositoryFile);
            fileOutputStream.write(buffer);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    LOG.error(e.toString(), e);
                }
            }
        }
    }

    /**
     * Dir the repo is in.
     *
     * @return dir
     * @throws FileNotFoundException if it can neither be found not made.
     */
    private static File getRepositoryRootDir() throws FileNotFoundException {
        // Note: same mechanism is used in Carrier! -hendrik
        String repositoryPath = System.getProperty(ORG_BUNDLEBEE_REPOSITORY_ROOT, DEFAULT_REPOSITORY_ROOT).trim();
        if (repositoryPath != null && repositoryPath.startsWith("~")) {
            repositoryPath = System.getProperty("user.home") + repositoryPath.substring(1);
        }
        final File repositoryRoot = new File(repositoryPath);
        repositoryRoot.mkdirs();
        if (!repositoryRoot.exists()) {
            throw new FileNotFoundException("Repository root " + repositoryRoot + " does not exist.");
        }
        if (!repositoryRoot.canRead()) {
            throw new FileNotFoundException("Repository root " + repositoryRoot + " is not readable.");
        }
        return repositoryRoot;
    }

    private String getRepositoryAlias() {
        return System.getProperty(ORG_BUNDLEBEE_REPOSITORY_ALIAS, DEFAULT_BUNDLEBEE_REPOSITORY_ALIAS);
    }

    /**
     * The URL pointing to our repository.xml file.
     *
     * @return url pointing to a resource
     */
    public URL getRepositoryURL() {
        if (port == null) {
            if (LOG.isDebugEnabled()) LOG.debug(BUNDLE_MARKER, "Port is null");
            return null;
        }
        try {
            return new URL("http://" + localAddress
                    + ":" + port + getRepositoryAlias() + "/" + REPOSITORY_XML);
        } catch (MalformedURLException e) {
            LOG.error(BUNDLE_MARKER, e.toString(), e);
        }
        return null;
    }

    /**
     * Keeps track of the availability of the HttpService and un/registers the repository accordingly.
     *
     * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
     * @see org.bundlebee.registry.impl.RegistryImpl#registerRepository(java.net.URL)
     */
    private class HttpServiceTracker extends ServiceTracker {

        private final BundleContext bundleContext;
        private final File repositoryRoot;
        private final String repositoryAlias;

        public HttpServiceTracker(final BundleContext bundleContext, final File repositoryRoot, final String repositoryAlias) {
            super(bundleContext, HttpService.class.getName(), null);
            this.bundleContext = bundleContext;
            this.repositoryRoot = repositoryRoot;
            this.repositoryAlias = repositoryAlias;
        }

        public Object addingService(final ServiceReference serviceReference) {
            final Integer port = (Integer)serviceReference.getProperty("http.port");
            // is this right? - hendrik
            if (port == null) RepositoryImpl.this.port = DEFAULT_PORT;
            else RepositoryImpl.this.port = port;
            final HttpService httpService = (HttpService) bundleContext.getService(serviceReference);
            if (httpService != null) {
                try {
                    final SimpleFileHttpContext simpleFileHttpContext
                            = new SimpleFileHttpContext(httpService.createDefaultHttpContext(), repositoryRoot);
                    httpService.registerResources(repositoryAlias, repositoryRoot.toString(), simpleFileHttpContext);
                    registerAtDistributedRegistry();
                } catch (IOException e) {
                    LOG.error(BUNDLE_MARKER, e.toString(), e);
                } catch (NamespaceException e) {
                    LOG.error(BUNDLE_MARKER, e.toString(), e);
                }
            }
            return httpService;
        }

        private void registerAtDistributedRegistry() {
            final URL repositoryURL = RepositoryImpl.this.getRepositoryURL();
            if (repositoryURL != null) RegistryImpl.getInstance().registerRepository(repositoryURL);
        }

        public void removedService(final ServiceReference serviceReference, final Object o) {
            unregisterFromDistributedRegistry();
            HttpService httpService = (HttpService)o;
            if (httpService != null) {
                httpService.unregister(DEFAULT_BUNDLEBEE_REPOSITORY_ALIAS);
            }
            RepositoryImpl.this.port = null;
        }

        private void unregisterFromDistributedRegistry() {
            final URL repositoryURL = RepositoryImpl.this.getRepositoryURL();
            if (repositoryURL != null) RegistryImpl.getInstance().unregisterRepository(repositoryURL);
        }

        public synchronized void close() {
            RepositoryImpl.this.port = null;
            unregisterFromDistributedRegistry();
            super.close();
        }
    }

    /**
     * Copy a file with a channel.
     *
     * @param from file to copy
     * @param to file to copy to
     * @throws java.io.IOException if copying fails
     */
    private static void copy(final File from, final File to) throws IOException {
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(from);
            out = new FileOutputStream(to);
            long inPos = 0;
            long count = from.length();
            final FileChannel inChannel = in.getChannel();
            final FileChannel outChannel = out.getChannel();
            while (count != 0) {
                final long transferred = inChannel.transferTo(inPos, count, outChannel);
                inPos += transferred;
                count -= transferred;
                inChannel.position(inPos);
            }
            to.setLastModified(from.lastModified());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.toString(), e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error(e.toString(), e);
                }
            }
        }
    }

    /**
     * Copy a URL to a file.
     *
     * @param from url to copy
     * @param to file to copy to
     * @throws java.io.IOException if copying fails
     */
    private static void copy(final URL from, final File to) throws IOException {
        InputStream in = null;
        FileOutputStream out = null;
        try {
            final URLConnection urlConnection = from.openConnection();
            in = urlConnection.getInputStream();
            out = new FileOutputStream(to);
            final byte[] buf = new byte[64 * 1024];
            int lastRead;
            while ((lastRead = in.read(buf)) != -1) {
                out.write(buf, 0, lastRead);
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    LOG.error(e.toString(), e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    LOG.error(e.toString(), e);
                }
            }
        }
    }

    private static class SymbolicNameComparator implements Comparator<Resource> {
        public int compare(Resource r1, Resource r2) {
            final String s1 = getSymbolicName(r1);
            final String s2 = getSymbolicName(r2);
            return s1.compareTo(s2);
        }

        private String getSymbolicName(final Resource resource) {
            final String s = resource.getSymbolicName();
            return s == null ? "no-symbolic-name" : s;
        }
    }
}
