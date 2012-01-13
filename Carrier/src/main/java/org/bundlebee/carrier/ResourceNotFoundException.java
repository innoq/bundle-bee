package org.bundlebee.carrier;

/**
 * ResourceNotFoundException.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ResourceNotFoundException extends DeploymentException {
    
    static final long serialVersionUID = 42L;

    public ResourceNotFoundException(final String filterExpression) {
        super("Failed to find resource for " + filterExpression);
    }
}
