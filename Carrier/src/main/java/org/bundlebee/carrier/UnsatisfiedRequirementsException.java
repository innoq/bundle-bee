package org.bundlebee.carrier;

import org.osgi.service.obr.Resource;
import org.osgi.service.obr.Requirement;

/**
 * UnsatisfiedRequirementsException.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class UnsatisfiedRequirementsException extends DeploymentException {

    static final long serialVersionUID = 42L;

    private static final String LINE_SEP = System.getProperty("line.separator");
    private Resource resource;
    private Requirement[] requirements;

    public UnsatisfiedRequirementsException(final Resource resource, final Requirement[] requirements) {
        super(createMessage(resource, requirements));
        this.requirements = requirements;
        this.resource = resource;
    }

    public Resource getResource() {
        return resource;
    }

    public Requirement[] getRequirements() {
        return requirements;
    }

    private static String createMessage(final Resource resource, final Requirement[] requirements) {
        final StringBuilder sb = new StringBuilder();
        sb.append("Failed to satisfy requirements for " + resource.getId() + ":");
        for (final Requirement requirement:requirements) {
            sb.append(LINE_SEP)
                    .append('\t')
                    .append(requirement.getFilter())
                    .append(", ")
                    .append(requirement.getName());
        }
        return sb.toString();
    }
}
