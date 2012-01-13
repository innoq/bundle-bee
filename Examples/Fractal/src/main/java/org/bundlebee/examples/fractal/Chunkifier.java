package org.bundlebee.examples.fractal;

import java.awt.Rectangle;
import java.util.Collection;

/**
 *
 * @author innoq
 */
public interface Chunkifier {

	Collection<Rectangle> chunkify( Rectangle r );
}
