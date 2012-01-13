package org.bundlebee.examples.fractal;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.swing.Timer;

/**
 *
 * @author innoq
 */
public class EffectManager {

	private final List<GlowingBorderEffect> mEffects = Collections.synchronizedList(new ArrayList<GlowingBorderEffect>());

	public EffectManager( final BufferedImagePanel target ) {
		Timer t = new Timer(100, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				synchronized(mEffects) {
					Iterator<GlowingBorderEffect> it =  mEffects.iterator();
					while( it.hasNext() ) {
						GlowingBorderEffect eff = it.next();
						if( ! eff.isFinished() ) {
						}
						else
						{
							it.remove();
						}
						target.repaint(eff.getRectangle());
					}
				}
			}
		});
		t.setRepeats(true);
		t.start();
	}

	void display( Graphics2D g2 ) {
		synchronized(mEffects) {
			for( GlowingBorderEffect e : mEffects ) {
				e.display(g2);
			}
		}
	}

	void addEffect( GlowingBorderEffect e ) {
		synchronized(mEffects) {
			mEffects.add(e);
		}
	}

}
