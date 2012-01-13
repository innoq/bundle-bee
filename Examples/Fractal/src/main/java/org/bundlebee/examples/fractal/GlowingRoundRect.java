
package org.bundlebee.examples.fractal;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

/**
 *
 * @author innoq
 */
public class GlowingRoundRect {

	// Here's the trick... To render the glow, we start with a thick pen
	// of the "inner" color and stroke the desired shape.  Then we repeat
	// with increasingly thinner pens, moving closer to the "outer" color
	// and increasing the opacity of the color so that it appears to
	// fade towards the interior of the shape.  We rely on the "clip shape"

	// having been rendered into our destination image already so that
	// the SRC_ATOP rule will take care of clipping out the part of the
	// stroke that lies outside our shape.

	static public void paint(Graphics2D g2, Rectangle rect, int arc, Color color, int glowWidth, float opacity ) {
		int gw = glowWidth*2;

		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2.setColor(color);
		for (int i=gw; i >= 2; i-=2) {
			float pct = (float)(gw - i) / (gw - 1);

			// See my "Java 2D Trickery: Soft Clipping" entry for more
			// on why we use SRC_ATOP here
			g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, pct * opacity ));
			g2.setStroke(new BasicStroke(i));
			g2.drawRoundRect(rect.x+glowWidth, rect.y+glowWidth, rect.width-gw, rect.height-gw, arc, arc);
		}
	}

}
