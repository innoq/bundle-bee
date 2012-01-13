package org.bundlebee.carrier;

import org.osgi.framework.BundleException;

/**
 * DeploymentException.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class DeploymentException extends BundleException {

    static final long serialVersionUID = 42L;

    public DeploymentException(final String message) {
        super(message);
    }
}
