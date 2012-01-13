package org.bundlebee.examples.fractal.mandelbrot;

/**
 *
 * @author innoq
 */
public class Mandelbrot {

	public static int iterate(Complex z0, int maxdepth) {
		Complex z = z0;
		for (int t = 0; t <= maxdepth; t++) {
//			if (z.abs() >= 2.0) {			// z.absSquared is WAY faster!
			if (z.absSquared() >= 4.0) {
				return t;
			}
			z = z.times(z).plus(z0);
		}
		return maxdepth;
	}

	public static int[][] computeArea( Complex z0, Complex dz, int width, int height, int maxdepth ) {
		int[][] res = new int[width][height];
		
		for( int i=0; i<width; i++ ) {
			if( Thread.currentThread().isInterrupted() ) { System.out.println("calculation interrupted at column "+ i); return res; }
			for( int j=0; j<height; j++) {
				Complex z = new Complex( z0.re()+i*dz.re(), z0.im()+j*dz.im() );
				res[i][j] = iterate(z, maxdepth);
			}
		}
		return res;
	}
}
