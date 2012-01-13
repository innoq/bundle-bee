package org.bundlebee.examples.fractal.mandelbrot;

/**
 *
 * @author innoq
 */
public class ComplexArea {

	private final Complex mTopLeft;
	private final Complex mSize;

	public ComplexArea(Complex topleft, Complex size) {
		mTopLeft	= topleft;
		mSize		= size;
	}

	public Complex topleft() {
		return mTopLeft;
	}

	public Complex size() {
		return mSize;
	}

}
