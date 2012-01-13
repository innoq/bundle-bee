package org.bundlebee.examples.fractal.mandelbrot;

/**
 * Same as Mandelbrot, just as an Object with non-static layout for usage as an OSGi service.
 * @author innoq
 */
public class MandelbrotAlgorithm {
	public int[][] computeArea( Complex z0, Complex dz, int width, int height, int maxdepth ) {
		System.out.println("computing mandelbrot " + z0 );
		return Mandelbrot.computeArea(z0, dz, width, height, maxdepth);
	}
}
