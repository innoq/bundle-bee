package org.bundlebee.registry.directory;

import junit.framework.TestCase;

import java.net.URL;
import java.net.MalformedURLException;
import java.util.Set;

/**
 * GridTest.
 *
 * @author <a href="mailto:hs@tagtraum.com">Hendrik Schreiber</a>
 */
public class GridTest extends TestCase {

    private Grid simpleGrid;
    private static final String BUNDLE_NAME = "TestBundle";
    private static final int BUNDLE_STATE = 0;
    private static final URL MANAGER_URL;
    private static final int NODE_ID = 0;

    static {
        URL url = null;
        try {
            url = new URL("http://mymanager/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        MANAGER_URL = url;
    }

    protected void setUp() throws Exception {
        simpleGrid = createSimpleGrid(BUNDLE_NAME, BUNDLE_STATE, MANAGER_URL);
    }

    public void testGetNonExistingNode() {
        final Grid grid = new Grid();
        final Node node = grid.getNode(0);
        assertNull(node);
    }

    public void testAddNode() {
        final Grid grid = new Grid();
        final Node node = grid.newNode(0);
        grid.addNode(node);
        assertEquals(0, node.getId());
        assertSame(grid, node.getGrid());
    }

    public void testGetManagersWithNonExistingBundle() {
        final Grid grid = new Grid();
        final Set<URL> urls = grid.getManagers("nonexisting", 200);
        assertNotNull("getManagers(...) should never return null, but has.", urls);
        assertTrue("getManagers(...) for bogus bundle was not empty: " + urls, urls.isEmpty());
    }

    public void testGetManagersWithExistingBundle() throws MalformedURLException {
        final Set<URL> urls = simpleGrid.getManagers(BUNDLE_NAME, BUNDLE_STATE);
        assertNotNull("getManagers(...) should never return null, but has.", urls);
        assertEquals("getManagers(" + BUNDLE_NAME + ", " + BUNDLE_STATE + ") did not return the manager url: " + urls,
                MANAGER_URL, urls.iterator().next());
    }

    public void testGetManagersWithExistingBundleButWrongState() throws MalformedURLException {
        final Set<URL> urls = simpleGrid.getManagers(BUNDLE_NAME, BUNDLE_STATE + 1);
        assertTrue("getManagers(" + BUNDLE_NAME + ", " + BUNDLE_STATE + 1 + ") did return a manager url: " + urls, urls.isEmpty());
    }

    public void testGetManagersWithUnregisteredManager() throws MalformedURLException {
        simpleGrid.getNode(NODE_ID).setManagerURL(null);
        final Set<URL> urls = simpleGrid.getManagers(BUNDLE_NAME, BUNDLE_STATE);
        assertNotNull("getManagers(...) should never return null, but has.", urls);
        assertTrue("getManagers(" + BUNDLE_NAME + ", " + BUNDLE_STATE
                + ") did return the manager url, even though it should have been removed: " + urls,
                urls.isEmpty());
        assertTrue("getManagers() should have been empty.", simpleGrid.getManagers().isEmpty());
    }

    public void testGetManagersWithReRegisteredManager() throws MalformedURLException {
        simpleGrid.getNode(NODE_ID).setManagerURL(null);
        simpleGrid.getNode(NODE_ID).setManagerURL(MANAGER_URL);
        final Set<URL> urls = simpleGrid.getManagers(BUNDLE_NAME, BUNDLE_STATE);
        assertNotNull("getManagers(...) should never return null, but has.", urls);
        assertEquals("getManagers(" + BUNDLE_NAME + ", " + BUNDLE_STATE + ") did not return the manager url: " + urls,
                MANAGER_URL, urls.iterator().next());
    }

    public void testUnregisterNode() {
        final Set<URL> urls = simpleGrid.getManagers(BUNDLE_NAME, BUNDLE_STATE);
        assertNotNull("getManagers(...) should never return null, but has.", urls);
        simpleGrid.unregisterNode(simpleGrid.getNodeIds().iterator().next());
        final Set<URL> urls2 = simpleGrid.getManagers(BUNDLE_NAME, BUNDLE_STATE);
        assertTrue(urls2.isEmpty());
    }


    private static Grid createSimpleGrid(final String bundleName, final int bundleState, final URL managerURL) {
        final Grid grid = new Grid();
        final Node node = grid.newNode(NODE_ID);
        grid.addNode(node);
        node.setManagerURL(managerURL);
        final Bundle bundle = node.newBundle(bundleName, bundleState);
        node.addBundle(bundle);
        return grid;
    }

}
