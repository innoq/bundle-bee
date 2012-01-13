package org.bundlebee.carrier;

/**
 * Is capable of downloading and deploying bundles from repositories.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface Carrier {

    /**
     * Obtain bundle, deploy it and optionally start it.
     * The given filter expression can use attributes from the repository tag in the OBR repository file.<br>
     * Examples:
     * <xmp>
     * (id=org.bundlebee.watchdog/1.0.0)
     * (symbolicname=org.bundlebee.watchdog)
     * (|(id=org.bundlebee.watchdog/1.0.0)(symbolicname=org.bundlebee.watchdog))
     * </xmp>
     * The first match for a corresponding search is deployed.
     *
     * @param filterExpression filter expression, e.g. (symbolicname=org.bundlebee.watchdog)
     *  filters are defined in OSGi R4 Core 3.2.6
     * @param start if true, start bundle.
     * @see <a href="http://felix.apache.org/site/apache-felix-osgi-bundle-repository.html">Apache Felix OBR</a>
     */
    void deploy(String filterExpression, boolean start) throws DeploymentException;

}
