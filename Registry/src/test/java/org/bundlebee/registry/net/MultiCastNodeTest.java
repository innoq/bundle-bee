/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bundlebee.registry.net;

import java.io.IOException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author joergp
 */
public class MultiCastNodeTest {

    public MultiCastNodeTest() {
    }

	@BeforeClass
	public static void setUpClass() throws Exception
	{
	}

	@AfterClass
	public static void tearDownClass() throws Exception
	{
	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

	/**
	 * Test of addMultiCastMessageListener method, of class MultiCastNode.
	 */
	@Test
	public void testPortAssignment() throws IOException {

		MultiCastNode n1 = new MultiCastNode(1L);
		MultiCastNode n2 = new MultiCastNode(2L);

		assertTrue(n1.isRunning());
		assertTrue(n2.isRunning());

		int p1 = n1.getPort();
		int p2 = n2.getPort();

		assertTrue( p2>=MultiCastNode.LOCAL_PORT);
		assertTrue("n2 port must be bigger then n1 port", p2>p1);
	}

}