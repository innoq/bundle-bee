package org.bundlebee.examples.fractal;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.LinkedList;

/**
 *
 * @author innoq
 */
public class HalfsizingChunkifier implements Chunkifier {

	private int mChunks = 1;

	public HalfsizingChunkifier() {
	}

	public HalfsizingChunkifier( int chunks ) {
		mChunks = chunks;
	}


	public synchronized void setChunks( int chunks ) {
		mChunks = chunks;
	}

	public synchronized Collection<Rectangle> chunkify(Rectangle r) {
		LinkedList<Rectangle> list = new LinkedList<Rectangle>();
		list.add(r);
		while( list.size() != mChunks ) {
			Rectangle tip = list.getFirst();
			Rectangle half1 = new Rectangle();
			Rectangle half2 = new Rectangle();
			partition(tip, half1, half2);

			list.removeFirst();
			list.addLast(half1);
			list.addLast(half2);
		}
		return list;
	}

	private static void partition(Rectangle r, Rectangle res1, Rectangle res2 ) {
		float ratio = r.width/r.height;

		// common
		res1.x = r.x;
		res1.y = r.y;

		if( ratio < 1.0 ) {
			// half height
			res1.width = res2.width = r.width;
			res2.x = r.x;
			
			int h1 = r.height / 2;
			int h2 = r.height - h1;
			res1.height = h1;
			res2.height = h2;
			res2.y = r.y+h1;
		} else {
			// half width
			res1.height = res2.height = r.height;
			res2.y = r.y;

			int w1 = r.width / 2;
			int w2 = r.width - w1;

			res1.width = w1;
			res2.width = w2;
			res2.x = r.x+w1;
		}

	}
			

}
