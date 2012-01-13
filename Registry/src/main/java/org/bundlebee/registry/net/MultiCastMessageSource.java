/*
 * =================================================
 * Copyright 2009 tagtraum industries incorporated
 * All rights reserved.
 * =================================================
 */
package org.bundlebee.registry.net;

/**
 * MultiCastMessageSource.
 * <p/>
 * Date: Jan 15, 2009
 * Time: 10:15:12 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public interface MultiCastMessageSource {
    void addMultiCastMessageListener(MultiCastMessageListener multiCastMessageListener);
}
