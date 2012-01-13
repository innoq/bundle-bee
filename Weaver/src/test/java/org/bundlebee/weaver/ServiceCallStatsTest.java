package org.bundlebee.weaver;

import junit.framework.TestCase;

/**
 * ServiceCallStatsTest.
 * <p/>
 * Date: Dec 17, 2008
 * Time: 10:48:29 AM
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class ServiceCallStatsTest extends TestCase {

    public void testOverflow() {
        final ServiceCallStats stats = new ServiceCallStats(Integer.MAX_VALUE, 1);
        stats.logLocalCall(new Object(), "method", new Class[0], Long.MAX_VALUE - 200);
        stats.logLocalCall(new Object(), "method", new Class[0], Long.MAX_VALUE - 10000000);
        assertEquals(Long.MAX_VALUE - 10000000, stats.getLocalCallMean(new Object(), "method", new Class[0]));
    }


    public void testRegularMean() {
        final ServiceCallStats stats = new ServiceCallStats(Integer.MAX_VALUE, 1);
        stats.logLocalCall(new Object(), "method", new Class[0], 4);
        stats.logLocalCall(new Object(), "method", new Class[0], 6);
        assertEquals(5, stats.getLocalCallMean(new Object(), "method", new Class[0]));
    }
}
