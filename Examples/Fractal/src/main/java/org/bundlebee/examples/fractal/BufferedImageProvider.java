package org.bundlebee.examples.fractal;

import java.awt.image.BufferedImage;
import javax.swing.JComponent;

/**
 *
 * @author innoq
 */
public interface BufferedImageProvider {

	BufferedImage get( JComponent j, BufferedImage old );

}
