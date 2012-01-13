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
public class GlowingBorderEffect {


//	private static final Color clrGlowInnerHi = new Color(253, 239, 175, 148);
//	private static final Color clrGlowInnerLo = new Color(255, 209, 0);
//	private static final Color clrGlowOuterHi = new Color(253, 239, 175, 124);
//	private static final Color clrGlowOuterLo = new Color(255, 179, 0);

	private final Rectangle mRect;
//	private final Shape mShape;
	private final long mStartTime;

	private final static long TTL = 15L * 100000000L;
	private boolean mFinished = false;

	private final static int GLOWWIDTH = 4;

	public GlowingBorderEffect( Rectangle r ) {
		mRect = r;
		mRect.grow( GLOWWIDTH, GLOWWIDTH );
//		mShape = createClipShape( r );
		mStartTime = System.nanoTime();
	}



//	private static Shape createClipShape( Rectangle r ) {
//		float border = 10.0f;
//
//		float x1 = r.x + border;
//		float y1 = r.y + border;
//		float x2 = r.x + r.width - border;
//		float y2 = r.y + r.height - border;
//
//		float adj = 3.0f; // helps round out the sharp corners
//		float arc = 8.0f;
//		float dcx = 0.18f * r.width;
//		float cx1 = x1-dcx;
//		float cy1 = 0.40f * r.height;
//		float cx2 = x1+dcx;
//		float cy2 = 0.50f * r.height;
//
//		GeneralPath gp = new GeneralPath();
//		gp.moveTo(x1-adj, y1+adj);
//		gp.quadTo(x1, y1, x1+adj, y1);
//		gp.lineTo(x2-arc, y1);
//		gp.quadTo(x2, y1, x2, y1+arc);
//		gp.lineTo(x2, y2-arc);
//		gp.quadTo(x2, y2, x2-arc, y2);
//		gp.lineTo(x1+adj, y2);
//		gp.quadTo(x1, y2, x1, y2-adj);
////		gp.curveTo(cx2, cy2, cx1, cy1, x1-adj, y1+adj);
//		gp.closePath();
//		return gp;
//	}


//	private static Color getMixedColor(Color c1, float pct1, Color c2, float pct2) {
//		float[] clr1 = c1.getComponents(null);
//		float[] clr2 = c2.getComponents(null);
//		for (int i = 0; i < clr1.length; i++) {
//			clr1[i] = (clr1[i] * pct1) + (clr2[i] * pct2);
//		}
//		return new Color(clr1[0], clr1[1], clr1[2], clr1[3]);
//	}

	private void paintBorderGlow(Graphics2D g2, int glowWidth, float opacity ) {
		GlowingRoundRect.paint(g2, mRect, 16, Color.YELLOW, glowWidth, opacity);
	}


	public void display( Graphics2D g2 ) {
		long dt = System.nanoTime()-mStartTime;
		mFinished = dt>TTL;
		float opacity = (float) Math.max(0.0, 1.0 - (double)(dt)/(double)TTL);
		paintBorderGlow( g2, GLOWWIDTH, opacity );
	}

	public boolean isFinished() {
		return mFinished;
	}

	public Rectangle getRectangle() {
		return mRect;
	}
}
