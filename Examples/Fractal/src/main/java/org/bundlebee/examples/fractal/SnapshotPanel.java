package org.bundlebee.examples.fractal;

import java.awt.AlphaComposite;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import javax.swing.JPanel;
import org.bundlebee.examples.fractal.mandelbrot.ComplexArea;
import org.bundlebee.examples.fractal.mandelbrot.MandelbrotContentProvider;

/**
 *
 * @author innoq
 */
public class SnapshotPanel extends JPanel {

	public final static int SIZE = 48;
	BufferedImage mImage = new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);

	private final MandelbrotContentProvider mMandel;
	private final ComplexArea mArea;

	public SnapshotPanel( final BufferedImagePanel srcpanel, MandelbrotContentProvider mandel, ComplexArea area ) {

		mMandel = mandel;
		mArea = area;

		setSize(SIZE,SIZE);
		setPreferredSize(new Dimension(SIZE, SIZE));

		BufferedImage srcimage = srcpanel.getImage();

		//
		// create a small image as a temporary source
		//
		BufferedImage tmpimage = new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = tmpimage.createGraphics();
		//g.scale((double)SIZE/srcimage.getWidth(), (double)SIZE/srcimage.getHeight());
		g.drawImage(srcimage, 0, 0, SIZE, SIZE, null);
		g.dispose();


		//
		// draw our image
		//
		g = mImage.createGraphics();
		int x = 0;
		int y = 0;
		int w = getWidth();
		int h = getHeight();
		int b = 1;

//		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		Polygon p = new Polygon(
				new int[]{ x, x+w, x+w,   x,   x, x+b,   x+b, x+w-b, x+w-b,   x },
                new int[]{ y,   y, y+h, y+h, y+b, y+b, y+h-b, y+h-b,   y+b, y+b },
                10
		);

		g.setClip(p);
		g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC, 0.2f ) );
		g.drawImage(tmpimage, 0, 0, null);


		x = x+b;
		y = y+b;
		w = w-2*b;
		h = h-2*b;
		p = new Polygon(
				new int[]{ x, x+w, x+w,   x,   x, x+b,   x+b, x+w-b, x+w-b,   x },
                new int[]{ y,   y, y+h, y+h, y+b, y+b, y+h-b, y+h-b,   y+b, y+b },
                10
		);
		g.setClip(p);
		g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC, 0.4f ) );
		g.drawImage(tmpimage, 0, 0, null);


		x = x+b;
		y = y+b;
		w = w-2*b;
		h = h-2*b;
		p = new Polygon(
				new int[]{ x, x+w, x+w,   x,   x, x+b,   x+b, x+w-b, x+w-b,   x },
                new int[]{ y,   y, y+h, y+h, y+b, y+b, y+h-b, y+h-b,   y+b, y+b },
                10
		);
		g.setClip(p);
		g.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC, 0.7f ) );
		g.drawImage(tmpimage, 0, 0, null);


		g.setClip( new Rectangle(x+b,x+b,w-2*b,h-2*b));
		g.setComposite( AlphaComposite.Src );
		g.drawImage( tmpimage, 0, 0, null );

//		g.drawImage( tmpimage,0,0,null );
		g.dispose();

		addMouseListener(new MouseAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				super.mouseClicked(e);
				mMandel.setArea(mArea);
				srcpanel.updateContent();
			}
		});
		
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;

//		int x = 0;
//		int y = 0;
//		int w = getWidth();
//		int h = getHeight();
//		int b = 1;
//
//
//		Shape origclip = g2.getClip();
////		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//
//		Polygon p = new Polygon(
//				new int[]{ x, x+w, x+w,   x,   x, x+b,   x+b, x+w-b, x+w-b,   x },
//                new int[]{ y,   y, y+h, y+h, y+b, y+b, y+h-b, y+h-b,   y+b, y+b },
//                10
//		);
//
//		g2.clip(p);
//		g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_ATOP, 0.2f ) );
//		g2.drawImage(mImage, 0, 0, null);
//
//
//		x = b;
//		y = b;
//		w = w-2*b;
//		h = h-2*b;
//		p = new Polygon(
//				new int[]{ x, x+w, x+w,   x,   x, x+b,   x+b, x+w-b, x+w-b,   x },
//                new int[]{ y,   y, y+h, y+h, y+b, y+b, y+h-b, y+h-b,   y+b, y+b },
//                10
//		);
//		g2.setClip(origclip);
//		g2.clip(p);
//		g2.setComposite( AlphaComposite.getInstance( AlphaComposite.SRC_ATOP, 0.5f ) );
//		g2.drawImage(mImage, 0, 0, null);
//
//
//		g2.setClip(origclip);
//		g2.clip( new Rectangle(x+b,x+b,w-2*b,h-2*b));
//		g2.setComposite( AlphaComposite.Src );
		g2.drawImage( mImage, 0, 0, null );
		

//		g.setColor(Color.RED);
//		g.fillRect(0, 0, SIZE, SIZE);
	}

}
