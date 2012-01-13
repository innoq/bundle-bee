package org.bundlebee.examples.fractal;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/**
 *
 * @author innoq
 */
public class ZoomRectangle extends Rectangle {

	private boolean mIsVisible = false;

	public void setVisible( boolean flag ) {
		mIsVisible = flag;
	}

	public void display( Graphics2D g )
	{
		if( mIsVisible ) {
			g.setColor( Color.CYAN );
			g.drawRoundRect(x, y, width-1, height-1, 10, 10);
		}
	}

}
