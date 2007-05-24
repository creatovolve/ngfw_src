/*
 * Copyright (c) 2003-2007 Untangle, Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Untangle, Inc. ("Confidential Information"). You shall
 * not disclose such Confidential Information.
 *
 * $Id$
 */

package com.untangle.gui.configuration;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import com.untangle.gui.transform.*;
import com.untangle.gui.util.*;
import com.untangle.gui.widgets.editTable.*;
import com.untangle.mvvm.networking.InterfaceAlias;
import com.untangle.mvvm.networking.BasicNetworkSettings;
import com.untangle.mvvm.tran.IPaddr;

public class NetworkAliasJPanel extends MEditTableJPanel{

    public NetworkAliasJPanel() {
        super(true, true);
        super.setFillJButtonEnabled( true );
        super.setInsets(new Insets(4, 4, 2, 2));
        super.setTableTitle("");
        super.setDetailsTitle("");
        super.setAddRemoveEnabled(true);
        super.setAuxJPanelEnabled(true);

        // add a basic description
        JLabel descriptionJLabel = new JLabel("<html>Aliases give more IP addresses to your Untangle Server. This is useful"
                                              + " if you wish to bridge more than one subnet as a <b>Transparent Bridge</b>."
                                              + " This is also useful if you wish to assign more external IP addresses to"
                                              + " redirect to machines on the internal network (<b>NAT</b>).</html>");
        descriptionJLabel.setFont(new Font("Default", 0, 12));
        auxJPanel.setLayout(new BorderLayout());
        auxJPanel.add(descriptionJLabel);

        // create actual table model
        InterfaceAliasModel interfaceAliasModel = new InterfaceAliasModel();
        this.setTableModel( interfaceAliasModel );

    }




    class InterfaceAliasModel extends MSortedTableModel<NetworkCompoundSettings>{

        private static final int  T_TW  = Util.TABLE_TOTAL_WIDTH;
        private static final int  C0_MW = Util.STATUS_MIN_WIDTH; /* status */
        private static final int  C1_MW = Util.LINENO_MIN_WIDTH; /* # */
        private static final int  C2_MW = 120;  /* address */
        private static final int  C3_MW = 120;  /* netmask */

        protected boolean getSortable(){ return false; }

        public TableColumnModel getTableColumnModel(){

            DefaultTableColumnModel tableColumnModel = new DefaultTableColumnModel();
            //                                 #   min    rsz    edit   remv   desc   typ            def
            addTableColumn( tableColumnModel,  0,  C0_MW, false, false, true,  false, String.class,  null, sc.TITLE_STATUS );
            addTableColumn( tableColumnModel,  1,  C1_MW, false, false, true,  false, Integer.class, null, sc.TITLE_INDEX );
            addTableColumn( tableColumnModel,  2,  C2_MW, false, true,  false, false, String.class, "1.2.3.4", "address" );
            addTableColumn( tableColumnModel,  3,  C3_MW, false, true,  false, false, String.class, "255.255.255.0", "netmask" );
            addTableColumn( tableColumnModel,  4, 10,     false, false, true,  false, InterfaceAlias.class, null, "");
            return tableColumnModel;
        }


        public void generateSettings(NetworkCompoundSettings networkCompoundSettings,
                                     Vector<Vector> tableVector, boolean validateOnly) throws Exception {
            List<InterfaceAlias> elemList = new ArrayList(tableVector.size());
            InterfaceAlias newElem = null;
            int rowIndex = 0;

            for( Vector rowVector : tableVector ){
                rowIndex++;
                newElem = (InterfaceAlias) rowVector.elementAt(4);
                try{ newElem.setAddress( IPaddr.parse((String)rowVector.elementAt(2)) ); }
                catch(Exception e){ throw new Exception("Invalid \"address\" in row: " + rowIndex); }
                try{ newElem.setNetmask( IPaddr.parse((String) rowVector.elementAt(3)) ); }
                catch(Exception e){ throw new Exception("Invalid \"netmask\" in row: " + rowIndex); }
                elemList.add(newElem);
            }

            // SAVE SETTINGS //////////
            if( !validateOnly ){
                BasicNetworkSettings basicSettings = networkCompoundSettings.getBasicSettings();
                basicSettings.setAliasList( elemList );
            }
        }

        public Vector<Vector> generateRows(NetworkCompoundSettings networkCompoundSettings) {
            BasicNetworkSettings basicSettings = networkCompoundSettings.getBasicSettings();
            List<InterfaceAlias> interfaceAliasList =
                (List<InterfaceAlias>) basicSettings.getAliasList();
            Vector<Vector> allRows = new Vector<Vector>(interfaceAliasList.size());
            Vector tempRow = null;
            int rowIndex = 0;

            for( InterfaceAlias alias : interfaceAliasList ){
                rowIndex++;
                tempRow = new Vector(5);
                tempRow.add( super.ROW_SAVED );
                tempRow.add( rowIndex );
                tempRow.add( alias.getAddress().toString());
                tempRow.add( alias.getNetmask().toString());
                tempRow.add( alias );
                allRows.add( tempRow );
            }
            return allRows;
        }


    }

}
