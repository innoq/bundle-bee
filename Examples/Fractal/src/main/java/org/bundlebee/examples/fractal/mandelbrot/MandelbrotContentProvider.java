package org.bundlebee.examples.fractal.mandelbrot;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.Collection;
import org.bundlebee.examples.fractal.AsynchronousContentRequest;
import org.bundlebee.examples.fractal.ContentProvider;
import org.bundlebee.examples.fractal.GlowingRoundRect;
import org.bundlebee.examples.fractal.HalfsizingChunkifier;

/**
 *
 * @author innoq
 */
public class MandelbrotContentProvider implements ContentProvider{

	public final static int DEFAULT_MAXDEPTH	= 64;
	public final static int DEFAULT_CHUNKS		= 16;

	private final MandelbrotAlgorithm mMandel	= new MandelbrotAlgorithm();
	
	private Complex mTopLeft	= new Complex(-2.0,-2.0);
	private Complex mSize		= new Complex(4,4);
	private int mMaxDepth		= DEFAULT_MAXDEPTH;
	private int mChunks			= DEFAULT_CHUNKS;
	
	private HalfsizingChunkifier mChunker	= new HalfsizingChunkifier(DEFAULT_CHUNKS);

	public void setTopLeft( Complex topleft ) {
		mTopLeft = topleft;
	}

	public void setSize( Complex size ) {
		mSize = size;
	}

	public void setArea( ComplexArea area ) {
		mTopLeft	= area.topleft();
		mSize		= area.size();
	}

	public void setMaxDepth( int maxdepth ) {
		mMaxDepth = maxdepth;
	}
	public void setChunks( int chunks ) {
		mChunks = chunks;
	}

	public void request( final AsynchronousContentRequest r ) {

		BufferedImage img = r.getBufferedImage();
		int w = img.getWidth();
		int h = img.getHeight();

		double dx = dx(w,h);
		final Complex dz = new Complex( dx, dx );

		mChunker.setChunks(mChunks);

		synchronized(img) {
			Collection<Rectangle> chunks = mChunker.chunkify(new Rectangle(0,0,w,h));
			for( final Rectangle rect : chunks ) {
				if( r.isCancelled() ) { System.out.println("cancelled mandelbrot before chunk " + rect ); return;}

				//
				// visualize chunk as 'under computation'
				//
				Graphics2D g2d = img.createGraphics();
				g2d.setColor(new Color(128,128,128,128));
				g2d.fillRect(rect.x, rect.y, rect.width, rect.height);
				GlowingRoundRect.paint(g2d, rect, 4, Color.GRAY, 4, 1.0f);
				g2d.dispose();
				r.updated(rect, true);

				//
				// run each chunk in an own thread
				//
				Thread t = new Thread( new Runnable() {
					public void run() {
						Complex z0 = new Complex( mTopLeft.re() + dz.re()*rect.x, mTopLeft.im() + dz.im()*rect.y);
						compute( r, z0, dz, rect, mMaxDepth );
						r.progress(1f/mChunks);
					}
				});
				t.start();
			}
		}
	}


	/**
	 * compute a single rectangle
	 * @param r
	 * @param raster
	 * @param z0
	 * @param dz
	 * @param rect
	 * @param maxdepth
	 */
	void compute( AsynchronousContentRequest r, Complex z0, Complex dz, Rectangle rect, int maxdepth ) {
		int[][] res = mMandel.computeArea(z0, dz, rect.width, rect.height, maxdepth );

		BufferedImage img = r.getBufferedImage();
		if( ! r.isCancelled() ) synchronized(img) {
			WritableRaster raster = img.getRaster();

			int[] color = new int[4];
			for( int i=0; i<rect.width; i++ ) {
				for( int j=0; j<rect.height; j++ ){
					fillColor( res[i][j], maxdepth, color );
					raster.setPixel(rect.x + i,rect.y + j, color);
	//				raster.setPixel(i,j, new int[]{256 - res[i][j]});
				}
			}
			if( r.isCancelled() ) { System.out.println("cancelled mandelbrot when leaving chunk " + rect ); return;}
		}
		r.updated(rect, false);

	}


	/**
	 * get a nice color from the depth
	 */
	private void fillColor( int depth, int maxdepth, int[] color) {

		// fully opaque
		color[3] = 255;

		//
		// the core always needs so be BLACK!
		//
		if( depth == maxdepth ) {
			color[0] = 0;
			color[1] = 0;
			color[2] = 0;
			return;
		}

		depth *= 1000;
		color[0] = depth & 0xff;
		color[1] = depth>>8 & 0xff;
		color[2] = depth>>16 & 0xff;
	};


	private double dx( int width, int height  ) {
		double  w = width;
		double  h = height;

		double ratiotarget = w / h;
		double ratiosource = mSize.re() / mSize.im();
		double dx;
		if( ratiotarget > ratiosource ) {
			dx = mSize.im() / h;
		} else {
			dx =  mSize.re() / w;
		}
		return dx;
	}


	/**
	 * adapt 'topleft' and 'size' to a rectangle on a panel with the given size
	 * @param currentsize
	 * @param zoom
	 */
	public void zoomTo( Dimension currentsize, Rectangle zoom ) {

		double dx = dx( currentsize.width, currentsize.height );

		mTopLeft = new Complex( mTopLeft.re() + zoom.x * dx, mTopLeft.im() + zoom.y * dx );
		mSize = new Complex( zoom.width*dx, zoom.height*dx );
	}


	/**
	 * compute the complex area that is display for a given size on screen
	 * @param screensize
	 * @return
	 */
	public ComplexArea getDisplayedArea( Dimension screensize ) {

		double dx = dx( screensize.width, screensize.height );
		Complex size = new Complex( screensize.width*dx, screensize.height*dx );

		return new ComplexArea(mTopLeft, size);
	}
}
