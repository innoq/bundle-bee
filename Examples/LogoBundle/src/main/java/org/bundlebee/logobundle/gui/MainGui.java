package org.bundlebee.logobundle.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class MainGui extends JFrame {
	private JPanel jPanel = null;
	private JFrame selfFrame;

	public MainGui() {
		this.jPanel = new LogoGuiPanel();
		this.selfFrame = this;
		this.add(jPanel);
		this.setVisible(true);
		this.setSize(300, 400);
		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				selfFrame.dispose();
				selfFrame.setVisible(false);
			}
		});
	}

	public static void main(String args[]) {
		new MainGui();
	}
}
