package org.bundlebee.examples.fractal;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

/**
 *
 * @author innoq
 */
public class BufferedImagePanel extends JPanel {

	private final BufferedImageProvider		mImageProvider;
	private final ContentProvider			mContentProvider;

	private BufferedImage					mImage = null;
	private ZoomRectangle					mZoom = new ZoomRectangle();

	public interface ZoomListener {
		/**
		 * zoom to a rectangle
		 * @param r
		 */
		void zoomTo( Rectangle r );
	}

	private final List<ZoomListener> mZoomListeners = new ArrayList<ZoomListener>();
	

	private AsynchronousContentRequest.Observer mRequestObserver = null;


	public BufferedImagePanel( BufferedImageProvider imageprovider, ContentProvider contentprovider ) {
		mImageProvider		= imageprovider;
		mContentProvider	= contentprovider;

		//
		// when size changes, ask for a new BufferedImage
		//
		addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				mImage = mImageProvider.get( BufferedImagePanel.this, mImage );
				Rectangle r = new Rectangle( BufferedImagePanel.this.getSize() );
				repaint(r);
				requestContentProvider( new AsynchronousContentRequest(BufferedImagePanel.this, mImage, new Rectangle[]{r}, mRequestObserver ) );
			}
			public void componentMoved(ComponentEvent e) {}
			public void componentShown(ComponentEvent e) {}
			public void componentHidden(ComponentEvent e) {}
		});

		MouseAdapter ma = new MouseAdapter() {
			boolean mIsDragging = false;
			@Override
			public void mousePressed(MouseEvent e) {
				super.mousePressed(e);
				mZoom.setLocation(e.getPoint());
				mZoom.setSize(0,0);
				mZoom.setVisible(true);
				repaint(mZoom);
				mIsDragging = true;
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				super.mouseReleased(e);
				mIsDragging = false;
				mZoom.setVisible(false);
				repaint(mZoom);
				for( ZoomListener l : mZoomListeners ) {
					l.zoomTo(mZoom);
				}
			}

		};

		//
		// as with JDK5, MouseAdapter doesn't implement MouseMotionListener
		// so we need a separate object
		//
		MouseMotionAdapter mma = new MouseMotionAdapter() {

			@Override
			public void mouseDragged(MouseEvent e)
			{
				repaint(mZoom);
				mZoom.setSize( e.getX()-mZoom.x, e.getY()-mZoom.y );
				repaint(mZoom);
			}
		};

		addMouseListener(ma);
		addMouseMotionListener(mma);
	}

	public void addZoomListener( ZoomListener l ) {
		mZoomListeners.add( l );
	}

	public void removeZoomListener( ZoomListener l ) {
		mZoomListeners.remove( l );
	}

	public BufferedImage getImage() {
		return mImage;
	}

	public void setRequestObserver( AsynchronousContentRequest.Observer obs ) {
		mRequestObserver = obs;
	}

	private final ThreadGroup mThreadGroup = new ThreadGroup("AsynchronousContentRequest");
	private AsynchronousContentRequest mLastContentRequest = null;
	void requestContentProvider( final AsynchronousContentRequest r ) {
		if( null!=mLastContentRequest ) mLastContentRequest.cancel();
		mThreadGroup.interrupt();
		mLastContentRequest = r;
		Thread contentthread = new Thread( mThreadGroup, new Runnable() {
			public void run() {
				mContentProvider.request(r);			}
		});
//		contentthread.setPriority( Thread.currentThread().getPriority() - 1 );
		contentthread.start();
	}


	/**
	 * ask for be content for the complete panel
	 */
	public void updateContent() {
		if( null!=mImage) {
			requestContentProvider( new AsynchronousContentRequest(BufferedImagePanel.this, mImage, new Rectangle[]{getBounds()}, mRequestObserver ) );
		}
	}

	
	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
//		Rectangle r = g.getClipBounds();
//		g.setColor(Color.CYAN.darker());
//		g.fillRect(r.x, r.y, r.width, r.height);
		g.drawImage(mImage, 0, 0, null);

		Graphics2D g2 = (Graphics2D) g.create();
		mEffects.display(g2);
		g2.dispose();

		g2 = (Graphics2D) g.create();
		mZoom.display(g2);
		g2.dispose();
	}

	private final EffectManager mEffects = new EffectManager(this);

	void update( Rectangle r, boolean intermediate ) {
		if( ! intermediate )
			mEffects.addEffect(new GlowingBorderEffect(r));
		this.repaint(r);
	}
}
