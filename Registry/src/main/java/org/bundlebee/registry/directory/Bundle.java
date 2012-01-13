package org.bundlebee.registry.directory;

/**
 * Represents a bundle in {@link Node}. Because instances of
 * this class are used as hash keys, this class is immutable.
 * <p>
 * To create a bundle instance, call {@link Node#newBundle(String, int)}.
 * <p>
 * Note, that the private Node member is not taken into account
 * in {@link #equals(Object)} or {@link #hashCode()} on purpose.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 * @see Grid
 * @see Node
 */
public class Bundle implements Comparable<Bundle> {

    private int state;
    private String name;
    private Node node;

    Bundle(final Node node, final String name, final int state) {
        this.node = node;
        this.name = name;
        this.state = state;
    }

    Node getNode() {
        return node;
    }

    public int getState() {
        return state;
    }

    public String getName() {
        return name;
    }

    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        final Bundle bundle = (Bundle) o;

        if (state != bundle.state) return false;
        if (name != null ? !name.equals(bundle.name) : bundle.name != null)
            return false;

        return true;
    }

    public int hashCode() {
        int result;
        result = state;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        return result;
    }

    public int compareTo(final Bundle that) {
        if (this.state != that.state) return this.name.compareTo(that.name);
        return this.state - that.state;
    }

    public String getStateDescription() {
        switch (state) {
            case org.osgi.framework.Bundle.ACTIVE: return "ACTIVE";
            case org.osgi.framework.Bundle.INSTALLED: return "INSTALLED";
            case org.osgi.framework.Bundle.RESOLVED: return "RESOLVED";
            case org.osgi.framework.Bundle.STARTING: return "STARTING";
            case org.osgi.framework.Bundle.STOPPING: return "STOPPING";
            case org.osgi.framework.Bundle.UNINSTALLED: return "UNINSTALLED";
            default: return "" + state;
        }
    }


    public String toString() {
        return "Bundle[name=" + name +",state=" + getStateDescription() + "]";
    }
}
