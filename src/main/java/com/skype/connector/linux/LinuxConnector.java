/*******************************************************************************
 * Copyright (c) 2006-2007 Koji Hisano <hisano@gmail.com> - UBION Inc. Developer
 * Copyright (c) 2006-2007 UBION Inc. <http://www.ubion.co.jp/>
 * 
 * Copyright (c) 2006-2007 Skype Technologies S.A. <http://www.skype.com/>
 * 
 * Skype4Java is licensed under either the Apache License, Version 2.0 or
 * the Eclipse Public License v1.0.
 * You may use it freely in commercial and non-commercial products.
 * You may obtain a copy of the licenses at
 *
 *   the Apache License - http://www.apache.org/licenses/LICENSE-2.0
 *   the Eclipse Public License - http://www.eclipse.org/legal/epl-v10.html
 *
 * If it is possible to cooperate with the publicity of Skype4Java, please add
 * links to the Skype4Java web site <https://developer.skype.com/wiki/Java_API> 
 * in your web site or documents.
 * 
 * Contributors:
 * Koji Hisano - initial API and implementation
 ******************************************************************************/
package com.skype.connector.linux;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.skype.connector.Connector;
import com.skype.connector.ConnectorException;

/**
 * Implementation of the connector for Linux
 */
public final class LinuxConnector extends Connector {

    private static LinuxConnector _instance = null;
    
    /**
     * Get singleton instance.
     * @return instance.
     */
    public static synchronized Connector getInstance() {
        if(_instance == null) {
            _instance = new LinuxConnector();
        }
        return _instance;
    }
    
    private SkypeFrameworkListener listener = new SkypeFrameworkListener() {
        public void notificationReceived(String notificationString) {
            fireMessageReceived(notificationString);
        }
    };

    /**
     * Constructor.
     */
    private LinuxConnector() {
    }
    
    public boolean isRunning() throws ConnectorException {
        SkypeFramework.init();
        return SkypeFramework.isRunning();
    }

    /**
     * Gets the absolute path of Skype.
     * @return the absolute path of Skype.
     */
    public String getInstalledPath() {
        //TODO: get path to Skype
        File application = new File("/usr/bin/skype");
        if (application.exists()) {
            return application.getAbsolutePath();
        } else {
            return null;
        }
    }

    /**
     * Initializes this connector.
     */
    protected void initializeImpl() throws ConnectorException {
        SkypeFramework.init();
        SkypeFramework.addSkypeFrameworkListener(listener);
    }

    /**
     * Connects to Skype client.
     * @param timeout the maximum time in milliseconds to connect.
     * @return Status the status after connecting.
     * @throws ConnectorException when connection can not be established.
     */
    protected Status connect(int timeout) throws ConnectorException {

        if (!SkypeFramework.isRunning()) {
            setStatus(Status.NOT_RUNNING);
            return getStatus();
        }
        try {
            final BlockingQueue<String> queue = new LinkedBlockingQueue<String>();
            SkypeFrameworkListener initListener = new SkypeFrameworkListener() {
                public void notificationReceived(String notification) {
                    if ("OK".equals(notification) || "CONNSTATUS OFFLINE".equals(notification) || "ERROR 68".equals(notification)) {
                        try {
                            queue.put(notification);
                        } catch(InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
            };
            setStatus(Status.PENDING_AUTHORIZATION);
            SkypeFramework.addSkypeFrameworkListener(initListener);
            SkypeFramework.sendCommand("NAME " + getApplicationName());
            String result = queue.take();
            SkypeFramework.removeSkypeFrameworkListener(initListener);

            if ("OK".equals(result)) {
                setStatus(Status.ATTACHED);

            } else if ("CONNSTATUS OFFLINE".equals(result)) {
                setStatus(Status.NOT_AVAILABLE);

            } else if ("ERROR 68".equals(result)) {
                setStatus(Status.REFUSED);
            }
            return getStatus();
        } catch(InterruptedException e) {
            throw new ConnectorException("Trying to connect was interrupted.", e);
        }
    }

    /**
     * Sends a command to the Skype client.
     * @param command The command to send.
     */
    protected void sendCommand(final String command) {
        SkypeFramework.sendCommand(command);
    }

    /**
     * Cleans up the connector and the native library.
     */
    protected void disposeImpl() {
        SkypeFramework.removeSkypeFrameworkListener(listener);
        SkypeFramework.dispose();
    }
}
