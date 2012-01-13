/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bundlebee.examples.fractal;

/**
 *
 * @author innoq
 */
import javax.swing.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;

public class fraktal extends JFrame implements MouseListener {

	BufferedImage fractal;
	WritableRaster raster;
	Calculator calc;
	int mouse_x,mouse_y;
	double x;
	double y;
	double width;
	double height;

	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		mouse_x = e.getX();
		mouse_y = e.getY();
	}

	public void mouseReleased(MouseEvent e) {
		double tmpx = translateX(mouse_x);
		double tmpy = translateY(mouse_y);
		double tmp  = translateX(e.getX());
		x = tmpx;
		y = tmpy;

		width = Math.abs(tmp - tmpx);
		height = 0.75 * width;
		while (!calc.isDone()) {
			try { Thread.sleep(1000); } catch (InterruptedException ex) {}
		}

		calc = new Calculator(632,453,raster, this);

		calc.setInsets(x,y,width,height);

		calc.start();
	}

	public double translateX(int ix) {
		return x + (((double) ix) / 632) * width;
	}

	public double translateY(int iy) {
		return y + (((double) iy) / 453) * height;
	}

	public fraktal (String args[]) {
		super("Mandelbrot");
		setSize(640,480);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setResizable(false);


		Component graphics = new Component() {
			public void paint(Graphics g) {
				g.drawImage(fractal, 0,0,null);
			}
		};


		graphics.addMouseListener(this);
		getContentPane().add(graphics);

		fractal = new BufferedImage(632,453, BufferedImage.TYPE_INT_RGB);

		raster = fractal.getRaster();


		System.out.println("Topleft corner of raster: " + raster.getMinX() + "," + raster.getMinY());
		System.out.println("Size of raster: " + raster.getWidth() + "," + raster.getHeight());

		show();

		calc = new Calculator(632,453,raster, this);

		x = -1.5;
		y = -1;
		width = 2;
		height = 0.75 * width;

		if (args.length == 0) {
			calc.setInsets(-1.5, -1, 2, 0.75 * 2);
		}

		if (args.length == 3) {

			x = Double.parseDouble(args[0]);
			y = Double.parseDouble(args[1]);
			width = Double.parseDouble(args[2]);
			height = 0.75 * width;
			calc.setInsets(x,y,width,height);
		}

		System.out.println("Calculating rectangle: ("+x+","+y+") to (" + (x + width) + "," + (y + height)+")");

		calc.start();

		/*
		Object[] colors = new Object[200];
		int c = 255;
		for (int i = 0 ; i < 50 ; i++) {
			int[] color = new int[3];
			color[0] = 0; color[1] = 255 - c; color[2] = c;
			colors[i] = color;
			c -= 5;
		}


		c = 255;
		for (int i = 50 ; i < 100 ; i++) {
			int[] color = new int[3];
			color[0] = 255 - c; color[1] = 255; color[2] = 0;
			colors[i] = color;
			c -= 5;
		}

		c = 255;
		for (int i = 100 ; i < 150 ; i++) {
			int[] color = new int[3];
			color[0] = c; color[1] = c; color[2] = 255 - c;
			colors[i] = color;
			c -= 5;
		}

		c = 255;
		for (int i = 150 ; i < 200 ; i++) {
			int[] color = new int[3];
			color[0] = 255 - c; color[1] = 0; color[2] = 255;
			colors[i] = color;
			c -= 5;
		}


		for (int i = 0 ; i < 200 ; i++) {
			for (int j = 0 ; j < 30 ; j++) {
				raster.setPixel(i + 20,j + 30, (int[]) colors[i]);
			}
		}
		repaint();
		*/
	}

	public static void main(String args[]) {
		new fraktal(args);
	}

}


class Calculator extends Thread {
	WritableRaster raster;
	int sizex,sizey;

	double startx, starty, width, height;
	double z, zi;
	int[] black;
	int[] red;
	Object[] colors;
	JFrame parent;
	boolean done;
	;
	public Calculator(int sx, int sy, WritableRaster r, JFrame p) {
		parent = p;
		raster = r;
		sizex = sx;
		sizey = sy;
		z = 0;
		zi = 0;
		black = new int[3];
		black[0] = 0;black[1] = 0;black[2] = 0;
		red = new int[3];
		red[0] = 255;red[1] = 0;red[2] = 0;

		done = true;

		colors = new Object[200];
		int c = 255;
		for (int i = 0 ; i < 50 ; i++) {
			int[] color = new int[3];
			color[0] = 0; color[1] = 255 - c; color[2] = c;
			colors[i] = color;
			c -= 5;
		}


		c = 255;
		for (int i = 50 ; i < 100 ; i++) {
			int[] color = new int[3];
			color[0] = 255 - c; color[1] = 255; color[2] = 0;
			colors[i] = color;
			c -= 5;
		}

		c = 255;
		for (int i = 100 ; i < 150 ; i++) {
			int[] color = new int[3];
			color[0] = c; color[1] = c; color[2] = 255 - c;
			colors[i] = color;
			c -= 5;
		}

		c = 255;
		for (int i = 150 ; i < 200 ; i++) {
			int[] color = new int[3];
			color[0] = 255 - c; color[1] = 0; color[2] = 255;
			colors[i] = color;
			c -= 5;
		}
	}

	public void setInsets(double sx, double sy, double w, double h) {
		startx = sx; starty = sy;
		width = w; height = h;
	}

	public static double comp_mult_real(double a, double b, double c, double d) {
		return (a * c) - (b * d);
	}

	public static double comp_mult_imag(double a, double b, double c, double d) {
		return (a * d) + (b * c);
	}

	public int mandelbrotTest(double a, double bi) {
		// System.out.println("Testing ("+ a + "," + bi + ")");

		double atmp, btmp;
		int number = 0;
		double z = 0,zi = 0;

		while ( (number != 200) && (comp_magnitude(z,zi) < 2)) {
			number++;
			atmp = comp_mult_real(z,zi,z,zi);
			btmp = comp_mult_imag(z,zi,z,zi);

			z = atmp;
			zi = btmp;

			z += a;
			zi += bi;

			// if (number < 10)
				//System.out.println(number + "("+ z + "," + zi + ")");
		}

		if (number == 200) {
			// System.out.println("Part of the Mandelbrot set!");
			return -1;
		} else {
			// System.out.print(" " + number);
			return number;
		}
	}

	public boolean isDone() {
		return done;
	}

	public static double comp_magnitude(double a, double b) {
		return Math.sqrt( a * a + b * b);
	}

	public void run() {
		double dx = width / sizex;
		double dy = height / sizey;

		double z = startx , zi = starty;

		done = false;

		System.out.println("Calculating...");
		for (int x = 0 ; x < sizex ; x++) {
			zi = starty;
			int it;
			for (int y = 0 ; y < sizey ; y++) {
				if ((it =mandelbrotTest(z, zi)) != -1) {
					// In the mandelbrot set.
					raster.setPixel(x,y,(int[]) colors[it]);
				} else {
					// Not in the mandelbrot set
					raster.setPixel(x,y,black);
				}
				zi += dy;
			}
			if ( (x%5) == 0) {
				parent.repaint();

			}
			z += dx;
		}

		done = true;

		System.out.println("Done!");
	}

}