package org.bundlebee.registry.net;

/**
 * MultiCastMessageListener.
 * <p/>
 * Date: Jan 15, 2009
 * Time: 8:12:10 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface MultiCastMessageListener {

    void processMessage(MultiCastMessage multiCastMessage);

}
