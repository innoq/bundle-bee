package org.bundlebee.examples.fractal.osgi;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.util.Properties;
import org.bundlebee.examples.fractal.mandelbrot.MandelbrotAlgorithm;
import org.eclipse.osgi.framework.console.CommandProvider;

/**
 * Activator.
 *
 * @author joergp
 */
public class Activator implements BundleActivator {

    public void start(final BundleContext context) throws Exception {
        final Properties properties = new Properties();

		// register MandelbrotAlgorithm as a service
		MandelbrotAlgorithm mandel = new MandelbrotAlgorithm();
		context.registerService(MandelbrotAlgorithm.class.getName(), mandel, properties);

		context.registerService(CommandProvider.class.getName(), new FactralUICommand(), null);

    }
    public void stop(final BundleContext context) throws Exception {
    }
}
