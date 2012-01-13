package org.bundlebee.examples.fractal;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

/**
 *
 * @author innoq
 */
public class AsynchronousContentRequest {

	private final BufferedImagePanel mTarget;
	private final BufferedImage mImage;
	private final Rectangle[] mRects;
	private boolean mCancelled = false;

	public interface Observer {
		void started();
		void progressed( float p );
		void finished();
	}

	private final Observer mObserver;

	AsynchronousContentRequest( BufferedImagePanel target, BufferedImage i, Rectangle[] rects, Observer obs ) {
		mTarget		= target;
		mImage		= i;
		mRects		= rects;
		mObserver	= obs;
		if( null != mObserver ) mObserver.started();
	}

	public BufferedImage getBufferedImage() {
		return mImage;
	}

	public Rectangle[] getRectangles() {
		return mRects;
	}


	public void updated( Rectangle r, boolean intermediate ) {
		if( !isCancelled() )
			mTarget.update(r, intermediate);
	}

	public boolean isCancelled() {
		return mCancelled;
	}

	synchronized void cancel() {
//		System.out.println("AsynchronousContentRequest got cancelled: " + this );
		mCancelled = true;
	}

	float mProgress = 0;
	public synchronized void progress( float p ) {
		mProgress += p;
		if( null != mObserver && !isCancelled() ) {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					mObserver.progressed(mProgress);
					if( mProgress >= 1.0f )
						mObserver.finished();
				}
			});
		}
	}
}
