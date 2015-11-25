package org.bundlebee.weaver.equinox;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;
import ch.qos.logback.core.joran.spi.JoranException;
import javassist.*;
import org.bundlebee.registry.impl.RegistryImpl;
import org.eclipse.osgi.baseadaptor.BaseAdaptor;
import org.eclipse.osgi.baseadaptor.BaseData;
import org.eclipse.osgi.baseadaptor.HookConfigurator;
import org.eclipse.osgi.baseadaptor.HookRegistry;
import org.eclipse.osgi.baseadaptor.bundlefile.BundleEntry;
import org.eclipse.osgi.baseadaptor.hooks.AdaptorHook;
import org.eclipse.osgi.baseadaptor.hooks.ClassLoadingHook;
import org.eclipse.osgi.baseadaptor.loader.BaseClassLoader;
import org.eclipse.osgi.baseadaptor.loader.ClasspathEntry;
import org.eclipse.osgi.baseadaptor.loader.ClasspathManager;
import org.eclipse.osgi.framework.adaptor.BundleProtectionDomain;
import org.eclipse.osgi.framework.adaptor.ClassLoaderDelegate;
import org.eclipse.osgi.internal.loader.BundleLoader;
import org.eclipse.osgi.framework.log.FrameworkLog;
import org.eclipse.osgi.util.ManifestElement;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URLConnection;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;


/**
 * Allows us to hook into the Equinox bundle classloading mechanism.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see <a href="http://wiki.eclipse.org/index.php/Adaptor_Hooks">Equinox Adaptor Hooks</a>
 */
public class BundleBeeClassLoadingHook implements ClassLoadingHook, HookConfigurator, AdaptorHook {

    private static Logger LOG = LoggerFactory.getLogger(BundleBeeClassLoadingHook.class);
    
    private final ClassPool classPool;
    private final String dynamicImportPackages;

	// for the actual code weaving of a certain class
	private final Weaver mWeaver = new Weaver();

	//
	// setup logging
	//
	static {
        // make sure the standard log dir exists
        new File("logs").mkdirs();
        /*
        By default logback uses the threadcontextclassloader to load
        the logback.xml configuration. Unfortunately that's the
        Equinox launcher's loader in our case. That's why we have to make sure
        that we use the correct classloader and reconfigure here.
        uGlY, but it works. -hendrik
         */
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
        try {
            lc.shutdownAndReset();
            ContextInitializer.autoConfig(lc, Logger.class.getClassLoader());
        } catch (JoranException e) {
            LOG.error("Failure while configuring logback : " + e.toString(), e);
        }
    }


	/**
	 * ctor
	 */
    public BundleBeeClassLoadingHook() {
        this.classPool = ClassPool.getDefault();
        this.dynamicImportPackages = computeDynamicImportPackages(classPool);
    }

	
    /**
     * Weaves our grid code/aspect into a defined set of classes.
     *
     * @param classname        classname
     * @param classbytes       bytes of the class file
     * @param classpathEntry   classpathEntry
     * @param bundleEntry      bundle entry
     * @param classpathManager manager
     * @return the new byte array or null in case something went wrong, which will result in a behavior that
     *         is the same as if the hook wasn't there at all.
     * @see org.bundlebee.weaver.ServiceCallAspect
     */
    public byte[] processClass(final String classname, final byte[] classbytes, final ClasspathEntry classpathEntry,
                               final BundleEntry bundleEntry, final ClasspathManager classpathManager) {

		if (LOG.isDebugEnabled()) LOG.debug("Processing class " + classname + "...");

		// get the classloader that is used to load the bundle, which is different from mine!
		ClassLoader cl = (ClassLoader)classpathManager.getBaseClassLoader();

		return mWeaver.weaveClass(classbytes, classpathEntry, cl);
    }


    public boolean addClassPathEntry(final ArrayList arrayList, final String s, final ClasspathManager classpathManager,
                                     final BaseData baseData, final ProtectionDomain protectionDomain) {
        // no entry added
        return false;
    }

    public String findLibrary(final BaseData baseData, final String s) {
        return null;
    }

    public ClassLoader getBundleClassLoaderParent() {
        return null;
    }

    /**
     * When creating the classloader, we make sure to add the packages that we are using in the weaved-in
     * code, so that the class can actually be loaded by the bundle classloader. We do this by simply
     * adding dynamic import packages to the {@link ClassLoaderDelegate}, which is in fact a
     * {@link BundleLoader}.
     *
     * @param classLoader classloader
     * @param classLoaderDelegate delegate
     * @param bundleProtectionDomain domain
     * @param baseData basedata
     * @param strings some sort of strings
     * @return we always return null, as we are only manipulating the given ClassLoaderDelegate
     * @see org.eclipse.osgi.framework.internal.core.BundleLoader#addDynamicImportPackage(org.eclipse.osgi.util.ManifestElement[])
     */
    public BaseClassLoader createClassLoader(final ClassLoader classLoader,
                                             final ClassLoaderDelegate classLoaderDelegate,
                                             final BundleProtectionDomain bundleProtectionDomain,
                                             final BaseData baseData, final String[] strings) {
        final BundleLoader bundleLoader = (BundleLoader) classLoaderDelegate;
        try {
            if (LOG.isDebugEnabled()) LOG.debug("Adding dynamic import packages: " + dynamicImportPackages);
            // see http://www.eclemma.org/research/instrumentingosgi/index.html
            bundleLoader.addDynamicImportPackage(ManifestElement.parseHeader(Constants.DYNAMICIMPORT_PACKAGE, dynamicImportPackages));
        } catch (BundleException e) {
            LOG.error(e.toString(), e);
        }
        return null;
    }

    public void initializedClassLoader(final BaseClassLoader baseClassLoader, final BaseData baseData) {
    }

    /**
     * Registers this hook with the frameworks hook registry.
     *
     * @param hookRegistry the registry
     */
    public void addHooks(final HookRegistry hookRegistry) {
        if (LOG.isDebugEnabled()) LOG.debug("Adding " + this.getClass().getName() + " as hook to HookRegistry...");
        final BundleBeeClassLoadingHook hook = this;//new BundleBeeClassLoadingHook();
        hookRegistry.addClassLoadingHook(hook);
        hookRegistry.addAdaptorHook(hook);
    }

    private String computeDynamicImportPackages(final ClassPool classPool) {
        final StringBuilder sb = new StringBuilder();
        for (final Iterator packageIterator = classPool.getImportedPackages(); packageIterator.hasNext();) {
            final Object o = packageIterator.next();
            if (!"java.lang".equals(o)) {
                sb.append(o).append(',');
            }
        }
        if (sb.length() > 1) sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public void initialize(final BaseAdaptor baseAdaptor) {
    }

    public void frameworkStart(final BundleContext bundleContext) throws BundleException {
    }

    public void frameworkStop(final BundleContext bundleContext) throws BundleException {
        if (LOG.isInfoEnabled()) LOG.info("Stopping BundleBee Registry.");
        RegistryImpl.getInstance().stop();
    }

    public void frameworkStopping(final BundleContext bundleContext) {
    }

    public void addProperties(final Properties properties) {
    }

    public URLConnection mapLocationToURLConnection(final String s) throws IOException {
        return null;
    }

    public void handleRuntimeError(final Throwable throwable) {
    }

    public boolean matchDNChain(final String s, final String[] strings) {
        return false;
    }

    public FrameworkLog createFrameworkLog() {
        return null;
    }
}
