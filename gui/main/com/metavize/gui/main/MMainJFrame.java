/*
 * Copyright (c) 2004, 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.metavize.gui.main;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.swing.*;
import javax.swing.border.*;

import com.metavize.gui.configuration.*;
import com.metavize.gui.pipeline.*;
import com.metavize.gui.store.*;
import com.metavize.gui.transform.*;
import com.metavize.gui.upgrade.*;
import com.metavize.gui.util.*;
import com.metavize.mvvm.*;
import com.metavize.mvvm.security.*;
import com.metavize.mvvm.tran.*;
import com.metavize.mvvm.policy.*;

public class MMainJFrame extends javax.swing.JFrame {


    // CONSTANTS
    private static final Dimension MIN_SIZE = new Dimension(640, Util.determineMinHeight(480));
    private static final Dimension MAX_SIZE = new Dimension(2560, 1600); // the 30-inch cinema display max


    public MMainJFrame() {
        Util.setMMainJFrame(this);

        // INIT GUI
        initComponents();
        storeJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        toolboxJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
        configurationJScrollPane.getVerticalScrollBar().setUnitIncrement(5);
	java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-1024)/2, (screenSize.height-768)/2, 1024, 768);

	PolicyStateMachine policyStateMachine = new PolicyStateMachine(mTabbedPane,rackJComboBox,Util.getMRackJPanel(),
								       toolboxJScrollPane,policyToolboxJPanel,serviceToolboxJPanel,
								       storeScrollJPanel,metavizeJButton,
								       Util.getMPipelineJPanel().getJScrollPane());
	rackJComboBox.addActionListener(policyStateMachine);
	metavizeJButton.addActionListener(policyStateMachine);
		
        // UPDATE/UPGRADE
        new UpdateCheckThread();
    }
    
    
    public void updateJButton(final int count){
	Runnable updateButtonInSwing = new Runnable(){
		public void run() {
		    if( count == 0 ){
			upgradeJButton.setText("<html><center>Upgrade<br>(no upgrades)</center></html>");
			upgradeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/upgrade/IconUnavailable32x32.png")));
                        upgradeJButton.setEnabled(true);
		    }
		    else if( count == 1 ){
			upgradeJButton.setText("<html><center><b>Upgrade<br>(1 upgrade)</b></center></html>");
			upgradeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/upgrade/IconAvailable32x32.png")));
                        upgradeJButton.setEnabled(true);
		    }
		    else if( count > 1 ){
			upgradeJButton.setText("<html><center><b>Upgrade<br>(" + count + " upgrades)</b></center></html>");
			upgradeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/upgrade/IconAvailable32x32.png")));
                        upgradeJButton.setEnabled(true);
		    }
		    else if( count == Util.UPGRADE_UNAVAILABLE ){
			upgradeJButton.setText("<html><center>Upgrade<br>(unavailable)</center></html>");
			upgradeJButton.setEnabled(true);
		    }
		    else if( count == Util.UPGRADE_CHECKING ){
			upgradeJButton.setText("<html><center>Upgrade<br>(checking...)</center></html>");
			upgradeJButton.setEnabled(true);
		    }
		}
	    };
	SwingUtilities.invokeLater( updateButtonInSwing );
    }
    
    
    public Dimension getMinimumSize(){ return MIN_SIZE; } // used for form resizing
    

    private void initComponents() {//GEN-BEGIN:initComponents
        java.awt.GridBagConstraints gridBagConstraints;

        controlsJPanel = new javax.swing.JPanel();
        metavizeJButton = new javax.swing.JButton();
        mTabbedPane = new javax.swing.JTabbedPane();
        storeJPanel = new javax.swing.JPanel();
        storeJScrollPane = new javax.swing.JScrollPane();
        storeScrollJPanel = new javax.swing.JPanel();
        storeSpacerJPanel = new javax.swing.JPanel();
        toolboxJPanel = new javax.swing.JPanel();
        toolboxJScrollPane = new javax.swing.JScrollPane();
        toolboxScrollJPanel = new javax.swing.JPanel();
        policyToolboxJPanel = new javax.swing.JPanel();
        serviceToolboxJPanel = new javax.swing.JPanel();
        toolboxSpacerJPanel = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        configurationJPanel = new javax.swing.JPanel();
        configurationJScrollPane = new javax.swing.JScrollPane();
        jPanel8 = new javax.swing.JPanel();
        adminJButton = new javax.swing.JButton();
        networkJButton = new javax.swing.JButton();
        remoteJButton = new javax.swing.JButton();
        backupJButton = new javax.swing.JButton();
        protocolJButton = new javax.swing.JButton();
        maintenanceJButton = new javax.swing.JButton();
        aboutJButton = new javax.swing.JButton();
        configurationSpacerJPanel1 = new javax.swing.JPanel();
        rackJComboBox = new javax.swing.JComboBox();
        upgradeJButton = new javax.swing.JButton();
        mPipelineJPanel = new com.metavize.gui.pipeline.MPipelineJPanel();
        backgroundJLabel = new com.metavize.gui.widgets.MTiledIconLabel();

        getContentPane().setLayout(new java.awt.GridBagLayout());

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Metavize EdgeGuard Client");
        setFocusCycleRoot(false);
        setIconImage((new javax.swing.ImageIcon( this.getClass().getResource("/com/metavize/gui/icons/LogoNoText16x16.gif"))).getImage());
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                exitForm(evt);
            }
        });

        controlsJPanel.setLayout(new java.awt.GridBagLayout());

        controlsJPanel.setFocusable(false);
        controlsJPanel.setMinimumSize(new java.awt.Dimension(200, 427));
        controlsJPanel.setOpaque(false);
        controlsJPanel.setPreferredSize(new java.awt.Dimension(200, 410));
        metavizeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/main/LogoNoText96x96.png")));
        metavizeJButton.setBorderPainted(false);
        metavizeJButton.setContentAreaFilled(false);
        metavizeJButton.setDoubleBuffered(true);
        metavizeJButton.setFocusPainted(false);
        metavizeJButton.setFocusable(false);
        metavizeJButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        metavizeJButton.setMargin(new java.awt.Insets(1, 3, 3, 3));
        metavizeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                metavizeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        gridBagConstraints.insets = new java.awt.Insets(45, 45, 45, 45);
        controlsJPanel.add(metavizeJButton, gridBagConstraints);

        mTabbedPane.setDoubleBuffered(true);
        mTabbedPane.setFocusable(false);
        mTabbedPane.setFont(new java.awt.Font("Arial", 0, 11));
        mTabbedPane.setMinimumSize(new java.awt.Dimension(177, 177));
        mTabbedPane.setPreferredSize(new java.awt.Dimension(200, 160));
        storeJPanel.setLayout(new java.awt.GridBagLayout());

        storeJPanel.setBorder(new javax.swing.border.TitledBorder(null, " Click to Purchase ", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        storeJPanel.setFocusable(false);
        storeJPanel.setFont(new java.awt.Font("Arial", 0, 11));
        storeJPanel.setMaximumSize(new java.awt.Dimension(189, 32767));
        storeJPanel.setMinimumSize(new java.awt.Dimension(189, 134));
        storeJPanel.setOpaque(false);
        storeJScrollPane.setBorder(null);
        storeJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        storeJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        storeJScrollPane.setDoubleBuffered(true);
        storeJScrollPane.setFocusable(false);
        storeJScrollPane.setFont(new java.awt.Font("Arial", 0, 12));
        storeJScrollPane.setOpaque(false);
        storeJScrollPane.getViewport().setOpaque(false);
        storeScrollJPanel.setLayout(new java.awt.GridBagLayout());

        storeScrollJPanel.setFocusable(false);
        storeScrollJPanel.setOpaque(false);
        storeSpacerJPanel.setFocusable(false);
        storeSpacerJPanel.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        storeScrollJPanel.add(storeSpacerJPanel, gridBagConstraints);

        storeJScrollPane.setViewportView(storeScrollJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        storeJPanel.add(storeJScrollPane, gridBagConstraints);

        mTabbedPane.addTab("Store", storeJPanel);

        toolboxJPanel.setLayout(new java.awt.GridBagLayout());

        toolboxJPanel.setBorder(new javax.swing.border.TitledBorder(null, " Click to Install into Rack", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        toolboxJPanel.setFocusable(false);
        toolboxJPanel.setMaximumSize(new java.awt.Dimension(189, 32767));
        toolboxJPanel.setMinimumSize(new java.awt.Dimension(189, 134));
        toolboxJPanel.setOpaque(false);
        toolboxJScrollPane.setBorder(null);
        toolboxJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        toolboxJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        toolboxJScrollPane.setDoubleBuffered(true);
        toolboxJScrollPane.setFocusable(false);
        toolboxJScrollPane.setOpaque(false);
        toolboxJScrollPane.getViewport().setOpaque(false);
        toolboxScrollJPanel.setLayout(new java.awt.GridBagLayout());

        toolboxScrollJPanel.setFocusable(false);
        toolboxScrollJPanel.setOpaque(false);
        policyToolboxJPanel.setLayout(new javax.swing.BoxLayout(policyToolboxJPanel, javax.swing.BoxLayout.Y_AXIS));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        toolboxScrollJPanel.add(policyToolboxJPanel, gridBagConstraints);

        serviceToolboxJPanel.setLayout(new java.awt.GridLayout(1, 1));

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        toolboxScrollJPanel.add(serviceToolboxJPanel, gridBagConstraints);

        toolboxSpacerJPanel.setFocusable(false);
        toolboxSpacerJPanel.setMinimumSize(new java.awt.Dimension(0, 0));
        toolboxSpacerJPanel.setOpaque(false);
        toolboxSpacerJPanel.setPreferredSize(new java.awt.Dimension(0, 0));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        toolboxScrollJPanel.add(toolboxSpacerJPanel, gridBagConstraints);

        toolboxJScrollPane.setViewportView(toolboxScrollJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        toolboxJPanel.add(toolboxJScrollPane, gridBagConstraints);

        jLabel1.setFont(new java.awt.Font("Dialog", 0, 10));
        jLabel1.setText("shift-click to remove...");
        jLabel1.setFocusable(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(2, 0, 0, 0);
        toolboxJPanel.add(jLabel1, gridBagConstraints);

        mTabbedPane.addTab("Toolbox", toolboxJPanel);

        configurationJPanel.setLayout(new java.awt.GridBagLayout());

        configurationJPanel.setBorder(new javax.swing.border.TitledBorder(null, " Click to Configure", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Arial", 0, 11)));
        configurationJPanel.setFocusable(false);
        configurationJPanel.setMaximumSize(new java.awt.Dimension(189, 134));
        configurationJPanel.setMinimumSize(new java.awt.Dimension(189, 134));
        configurationJPanel.setOpaque(false);
        configurationJPanel.setPreferredSize(new java.awt.Dimension(189, 134));
        configurationJScrollPane.setBorder(null);
        configurationJScrollPane.setHorizontalScrollBarPolicy(javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        configurationJScrollPane.setVerticalScrollBarPolicy(javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        configurationJScrollPane.setDoubleBuffered(true);
        configurationJScrollPane.setFocusable(false);
        jPanel8.setLayout(new java.awt.GridBagLayout());

        jPanel8.setFocusable(false);
        adminJButton.setFont(new java.awt.Font("Arial", 0, 12));
        adminJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        adminJButton.setText("<html>Accounts</html>");
        adminJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        adminJButton.setDoubleBuffered(true);
        adminJButton.setFocusPainted(false);
        adminJButton.setFocusable(false);
        adminJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        adminJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        adminJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        adminJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                adminJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(adminJButton, gridBagConstraints);

        networkJButton.setFont(new java.awt.Font("Arial", 0, 12));
        networkJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        networkJButton.setText("<html>Network<br>Settings</html>");
        networkJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        networkJButton.setDoubleBuffered(true);
        networkJButton.setFocusPainted(false);
        networkJButton.setFocusable(false);
        networkJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        networkJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        networkJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        networkJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                networkJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(networkJButton, gridBagConstraints);

        remoteJButton.setFont(new java.awt.Font("Arial", 0, 12));
        remoteJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        remoteJButton.setText("<html>Remote<br>Administration</html>");
        remoteJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        remoteJButton.setDoubleBuffered(true);
        remoteJButton.setFocusPainted(false);
        remoteJButton.setFocusable(false);
        remoteJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        remoteJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        remoteJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        remoteJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                remoteJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(remoteJButton, gridBagConstraints);

        backupJButton.setFont(new java.awt.Font("Arial", 0, 12));
        backupJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        backupJButton.setText("<html>Backup &amp;<br>Restore</html>");
        backupJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        backupJButton.setDoubleBuffered(true);
        backupJButton.setFocusPainted(false);
        backupJButton.setFocusable(false);
        backupJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        backupJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        backupJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        backupJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                backupJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(backupJButton, gridBagConstraints);

        protocolJButton.setFont(new java.awt.Font("Arial", 0, 12));
        protocolJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        protocolJButton.setText("<html>Protocol<br>Settings</html>");
        protocolJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        protocolJButton.setDoubleBuffered(true);
        protocolJButton.setFocusPainted(false);
        protocolJButton.setFocusable(false);
        protocolJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        protocolJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        protocolJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        protocolJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                protocolJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(protocolJButton, gridBagConstraints);

        maintenanceJButton.setFont(new java.awt.Font("Arial", 0, 12));
        maintenanceJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        maintenanceJButton.setText("<html>Support</html>");
        maintenanceJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        maintenanceJButton.setDoubleBuffered(true);
        maintenanceJButton.setFocusPainted(false);
        maintenanceJButton.setFocusable(false);
        maintenanceJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        maintenanceJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        maintenanceJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        maintenanceJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maintenanceJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 1, 3, 3);
        jPanel8.add(maintenanceJButton, gridBagConstraints);

        aboutJButton.setFont(new java.awt.Font("Arial", 0, 12));
        aboutJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/icons/LogoNoText32x32.png")));
        aboutJButton.setText("<html>About<br>EdgeGuard</html>");
        aboutJButton.setBorder(new javax.swing.border.CompoundBorder(new javax.swing.border.EtchedBorder(), new javax.swing.border.EmptyBorder(new java.awt.Insets(2, 2, 2, 0))));
        aboutJButton.setDoubleBuffered(true);
        aboutJButton.setFocusPainted(false);
        aboutJButton.setFocusable(false);
        aboutJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        aboutJButton.setMargin(new java.awt.Insets(1, 3, 4, 2));
        aboutJButton.setMaximumSize(new java.awt.Dimension(810, 370));
        aboutJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(10, 1, 3, 3);
        jPanel8.add(aboutJButton, gridBagConstraints);

        configurationSpacerJPanel1.setFocusable(false);
        configurationSpacerJPanel1.setOpaque(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1.0;
        jPanel8.add(configurationSpacerJPanel1, gridBagConstraints);

        configurationJScrollPane.setViewportView(jPanel8);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        configurationJPanel.add(configurationJScrollPane, gridBagConstraints);

        mTabbedPane.addTab("Config", configurationJPanel);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        gridBagConstraints.insets = new java.awt.Insets(0, 10, 0, 10);
        controlsJPanel.add(mTabbedPane, gridBagConstraints);

        rackJComboBox.setFont(new java.awt.Font("Dialog", 0, 12));
        rackJComboBox.setFocusable(false);
        rackJComboBox.setMinimumSize(new java.awt.Dimension(160, 24));
        rackJComboBox.setPreferredSize(new java.awt.Dimension(160, 24));
        rackJComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rackJComboBoxActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.insets = new java.awt.Insets(15, 0, 0, 0);
        controlsJPanel.add(rackJComboBox, gridBagConstraints);

        upgradeJButton.setFont(new java.awt.Font("Default", 0, 12));
        upgradeJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/upgrade/IconAvailable32x32.png")));
        upgradeJButton.setText("<html>Upgrade<br></html>");
        upgradeJButton.setDoubleBuffered(true);
        upgradeJButton.setFocusPainted(false);
        upgradeJButton.setFocusable(false);
        upgradeJButton.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        upgradeJButton.setMargin(new java.awt.Insets(2, 2, 2, 4));
        upgradeJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                upgradeJButtonActionPerformed(evt);
            }
        });

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.insets = new java.awt.Insets(15, 20, 10, 20);
        controlsJPanel.add(upgradeJButton, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        getContentPane().add(controlsJPanel, gridBagConstraints);

        mPipelineJPanel.setOpaque(false);
        //((com.metavize.gui.pipeline.MPipelineJPanel)mPipelineJPanel).setMFilterJPanel((com.metavize.gui.filter.MFilterJPanel)mFilterJPanel);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(mPipelineJPanel, gridBagConstraints);

        backgroundJLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/metavize/gui/main/MainBackground1600x100.png")));
        backgroundJLabel.setDoubleBuffered(true);
        backgroundJLabel.setFocusable(false);
        backgroundJLabel.setOpaque(true);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weightx = 1.0;
        gridBagConstraints.weighty = 1.0;
        getContentPane().add(backgroundJLabel, gridBagConstraints);

        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        setBounds((screenSize.width-1024)/2, (screenSize.height-768)/2, 1024, 768);
    }//GEN-END:initComponents

    private void rackJComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rackJComboBoxActionPerformed
        //System.err.println("action performed");
    }//GEN-LAST:event_rackJComboBoxActionPerformed

    private void metavizeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_metavizeJButtonActionPerformed
    }//GEN-LAST:event_metavizeJButtonActionPerformed
    


    private void protocolJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_protocolJButtonActionPerformed
        try{
	    protocolJButton.setEnabled(false);
	    ProtocolJDialog protocolJDialog = new ProtocolJDialog();
	    protocolJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing protocol settings", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing protocol settings", f); }
	}
	finally{
	    protocolJButton.setEnabled(true);
	}
    }//GEN-LAST:event_protocolJButtonActionPerformed
    
    private void maintenanceJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maintenanceJButtonActionPerformed
        try{
	    maintenanceJButton.setEnabled(false);
	    MaintenanceJDialog maintenanceJDialog = new MaintenanceJDialog();
	    maintenanceJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing remote maintenance settings", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing remote maintenance settings", f); }
	}
	finally{
	    maintenanceJButton.setEnabled(true);
	}
    }//GEN-LAST:event_maintenanceJButtonActionPerformed
    
    private void remoteJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_remoteJButtonActionPerformed
        try{
	    remoteJButton.setEnabled(false);
	    RemoteJDialog remoteJDialog = new RemoteJDialog();
	    remoteJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing remote administration settings", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing remote administration settings", f); }
	}
	finally{
	    remoteJButton.setEnabled(true);
	}
    }//GEN-LAST:event_remoteJButtonActionPerformed
    
    private void backupJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_backupJButtonActionPerformed
	try{
	    backupJButton.setEnabled(false);
	    BackupRestoreJDialog backupRestoreJDialog = new BackupRestoreJDialog();
	    backupRestoreJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing backup and restore panel", e);}
	    catch(Exception f){Util.handleExceptionNoRestart("Error showing backup and restore panel", f);}
	}
	finally{
	    backupJButton.setEnabled(true);
	}
    }//GEN-LAST:event_backupJButtonActionPerformed
    
    private void networkJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_networkJButtonActionPerformed
	try{
	    networkJButton.setEnabled(false);
	    NetworkJDialog networkJDialog = new NetworkJDialog();
	    networkJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing network settings", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing network settings", f); }
	}
	finally{
	    networkJButton.setEnabled(true);
	}
    }//GEN-LAST:event_networkJButtonActionPerformed
    
    private void aboutJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutJButtonActionPerformed
	try{
	    aboutJButton.setEnabled(false);
	    AboutJDialog aboutJDialog = new AboutJDialog();
	    aboutJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error showing about", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error showing about", f); }
	}
	finally{
	    aboutJButton.setEnabled(true);
	}
    }//GEN-LAST:event_aboutJButtonActionPerformed
    
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized
        Util.resizeCheck(this, MIN_SIZE, MAX_SIZE);
    }//GEN-LAST:event_formComponentResized
    
    private void upgradeJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_upgradeJButtonActionPerformed
	try{
	    upgradeJButton.setEnabled(false);
	    UpgradeJDialog upgradeJDialog =  new UpgradeJDialog();
            upgradeJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error checking for upgrades on server", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error checking for upgrades on server", f); }
	}
	finally{
	    upgradeJButton.setEnabled(true);
	}
    }//GEN-LAST:event_upgradeJButtonActionPerformed
    
    private void adminJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_adminJButtonActionPerformed
	try{
	    adminJButton.setEnabled(false);
	    AdminConfigJDialog adminConfigJDialog = new AdminConfigJDialog();
	    adminConfigJDialog.setVisible(true);
	}
	catch(Exception e){
	    try{ Util.handleExceptionWithRestart("Error changing admins", e); }
	    catch(Exception f){ Util.handleExceptionNoRestart("Error changing admins", f); }
	}
	finally{
	    adminJButton.setEnabled(true);
	}
    }//GEN-LAST:event_adminJButtonActionPerformed
    
    private void exitForm(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_exitForm
        Util.exit(0);
    }//GEN-LAST:event_exitForm
    
        
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutJButton;
    private javax.swing.JButton adminJButton;
    private javax.swing.JLabel backgroundJLabel;
    private javax.swing.JButton backupJButton;
    private javax.swing.JPanel configurationJPanel;
    private javax.swing.JScrollPane configurationJScrollPane;
    private javax.swing.JPanel configurationSpacerJPanel1;
    private javax.swing.JPanel controlsJPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel mPipelineJPanel;
    private javax.swing.JTabbedPane mTabbedPane;
    private javax.swing.JButton maintenanceJButton;
    private javax.swing.JButton metavizeJButton;
    private javax.swing.JButton networkJButton;
    private javax.swing.JPanel policyToolboxJPanel;
    private javax.swing.JButton protocolJButton;
    private javax.swing.JComboBox rackJComboBox;
    private javax.swing.JButton remoteJButton;
    private javax.swing.JPanel serviceToolboxJPanel;
    private javax.swing.JPanel storeJPanel;
    private javax.swing.JScrollPane storeJScrollPane;
    private javax.swing.JPanel storeScrollJPanel;
    private javax.swing.JPanel storeSpacerJPanel;
    private javax.swing.JPanel toolboxJPanel;
    private javax.swing.JScrollPane toolboxJScrollPane;
    private javax.swing.JPanel toolboxScrollJPanel;
    private javax.swing.JPanel toolboxSpacerJPanel;
    private javax.swing.JButton upgradeJButton;
    // End of variables declaration//GEN-END:variables



    private class UpdateCheckThread extends Thread implements Killable {
    // KILLABLE //////
    private volatile boolean killed;
    public void setKilled(boolean killed){ this.killed = killed; }
    //////////////////

        public UpdateCheckThread(){
	    super("MVCLIENT-UpdateCheckThread");
            this.setDaemon(true);
	    Util.addKillableThread(this);
	    this.setContextClassLoader(Util.getClassLoader());
            this.start();
        }
        public void run() {
	    MackageDesc[] mackageDescs;
	    
            // FORCE THE SERVER TO UPDATE ONCE
            updateJButton(Util.UPGRADE_CHECKING);
            try{
                Util.getToolboxManager().update();
            }
            catch(Exception e){
                Util.handleExceptionNoRestart("Error updating upgrades on server", e);
            }
	    
	    while(true){
		try{
                    if(killed)
                        return;
		    
                    // CHECK FOR UPGRADES
                    updateJButton(Util.UPGRADE_CHECKING);
		    mackageDescs = Util.getToolboxManager().upgradable();
                    if( Util.isArrayEmpty(mackageDescs) ){
                        updateJButton(0);
                        Util.setUpgradeCount(0);
                    }
                    else{
                        updateJButton(mackageDescs.length);
                        Util.setUpgradeCount(mackageDescs.length);
                    }
                    Thread.sleep(Util.UPGRADE_THREAD_SLEEP_MILLIS);
		}
		catch(InterruptedException ie){
		    return;  // closed by interruption
		}
		catch(Exception e){
		    Util.handleExceptionNoRestart("Error checking for upgrades on server", e);
		    updateJButton(Util.UPGRADE_UNAVAILABLE);
		    try{ Thread.currentThread().sleep(10000); }  catch(Exception f){}
		}
	    }
        }
    }


}



