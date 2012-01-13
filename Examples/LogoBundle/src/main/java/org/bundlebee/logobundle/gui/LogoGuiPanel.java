package org.bundlebee.logobundle.gui;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Toolkit;
import java.net.URL;

import javax.swing.JPanel;

public class LogoGuiPanel extends JPanel {

    private Image logo = null;

    public LogoGuiPanel() {
        URL urlImage = getClass().getClassLoader().getResource("bundle_bee_green_anim.gif");

        this.logo = Toolkit.getDefaultToolkit().getImage(urlImage);

    }

    protected void paintComponent(Graphics g) {
        if (this.logo != null) {
            g.drawImage(this.logo, 0, 0, this);
        }
    }
}
