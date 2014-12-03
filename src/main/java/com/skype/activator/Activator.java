package com.skype.activator;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import com.skype.Skype;

public class Activator implements BundleActivator {

    @Override
    public void start(BundleContext bundleContext) throws Exception {
        System.out.println("Skype bundle started");

        Skype.chat("hacakton").send("Test message");
    }

    @Override
    public void stop(BundleContext bundleContext) throws Exception {
        System.out.println("Skype sender stopped");
    }

}
