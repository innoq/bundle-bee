package org.bundlebee.logobundle.impl;

import java.awt.Toolkit;
import java.io.IOException;
import java.lang.ref.ReferenceQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;


import org.bundlebee.logobundle.LogoBundle;
import org.bundlebee.logobundle.gui.LogoGuiPanel;
import org.bundlebee.logobundle.gui.MainGui;

import org.osgi.framework.*;
import org.osgi.util.tracker.ServiceTracker;

/**
 *
 * @author <a href="mailto:philipp.haussleiter@innoq.com">Philipp Hau�leiter</a>
 */
public class LogoBundleImpl implements LogoBundle {

    private BundleContext bundleContext;
    private ServiceTracker carrierTracker;
    private ServiceTracker httpServiceTracker;
    private ReferenceQueue<ServiceTracker> serviceTrackerReferenceQueue = new ReferenceQueue<ServiceTracker>();
    private Integer port = null;
    private BundleListener bundleListener;
    private JFrame mainGui = null;

    public LogoBundleImpl() {
    }

    public LogoBundleImpl(final BundleContext bundleContext) throws InvalidSyntaxException, IOException {
        this.bundleContext = bundleContext;
    }

    public BundleContext getBundleContext() {
        return bundleContext;
    }

    public void start() {
        System.setProperty("apple.awt.UIElement", "true");
        this.mainGui = new MainGui();
        this.mainGui.setSize(400, 325);
        this.mainGui.setVisible(true);
    }

    public void stop() {
        this.mainGui.setVisible(false);
        this.mainGui.dispose();
    }

    public static void main(String[] args) {
        LogoBundle logobundle = new LogoBundleImpl();
        logobundle.start();
    }
}
