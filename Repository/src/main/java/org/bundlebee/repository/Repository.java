package org.bundlebee.repository;

import java.io.File;
import java.net.URL;

/**
 * Local file based OSGi bundle repository (OBR).
 * <p/>
 * Date: Jan 8, 2009
 * Time: 10:28:16 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface Repository {

    /**
     * Adds a resource to the local OBR.
     *
     * @param resourceFile resource file
     * @throws Exception in the case of failure
     */
    void install(File resourceFile) throws Exception;

    /**
     * Adds a resource to the local OBR.
     *
     * @param resourceURL resource URL
     * @param bundleFileName file name, typically <code>symbolicname_version.jar</code>
     * @throws Exception in the case of failure
     */
    void install(URL resourceURL, String bundleFileName) throws Exception;

    /**
     * The URL pointing to our repository.xml file.
     *
     * @return url pointing to a resource, may be null, if the HTTP server is not started/available
     */
    URL getRepositoryURL();
}
