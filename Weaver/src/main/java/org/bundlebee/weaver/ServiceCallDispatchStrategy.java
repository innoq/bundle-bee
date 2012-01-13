package org.bundlebee.weaver;

import org.bundlebee.registry.Registry;

import java.net.URI;

/**
 * Returns an ordered list of Managers that can be used for executing a
 * remote service call.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface ServiceCallDispatchStrategy {

    void setRegistry(Registry registry);

    void setServiceCallStats(final ServiceCallStats serviceCallStats);

    /**
     * Returns a list of manager URIs that can be used for a remote call.
     * The Managers have to have the bundle installed and activated.
     * This strategy may cause installation and activation.
     *
     * @param bundleSymbolicNameVersion bundle / version
     * @param service service instance
     * @param methodname method name
     * @param parameterTypes parameter types of the method
     * @return a remote manager that can be used for the remote call,
     * {@link org.bundlebee.weaver.ServiceCallAspect#LOCAL_URI} to signal a local call
     *  or null, if none is found
     */
    URI getManagerURI(final String bundleSymbolicNameVersion, final Object service, final String methodname,
                             final Class[] parameterTypes);

}
