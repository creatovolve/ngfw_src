Ext.define('Ung.apps.intrusionprevention.view.UpdateSignatures', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-intrusion-prevention-update-signatures',
    itemId: 'update-signatures',

    title: 'Update Signatures'.t(),

    scrollable: true,
    padding: 10,

    defaults: {
        xtype: 'fieldset',
        padding: 10
    },

    items: [{
        title: 'Update Signatures and Signatures Schedule'.t(),
        items: [{
            xtype: 'component',
            html: 'You can manually update signatures or set a schedule.'
        }, {
            xtype: 'combo',
            fieldLabel: 'Frequency'.t(),
            bind: {
                store: '{updateSignatureFrequencyStore}',
                value: '{settings.updateSignatureFrequency}'
            },
            queryMode: 'local',
            editable: false,
            displayField: 'name',
            valueField: 'value'
        }, {
            xtype: 'fieldset',
            border: 'false',
            layout: {
                type: 'vbox'
            },
            items: [{
                xtype: 'fieldset',
                border: 'false',
                layout: {
                    type: 'hbox'
                },
                items: [{
                    xtype: 'combo',
                    queryMode: 'local',
                    fieldLabel: 'Sunday',
                    editable: false,
                    bind: {
                        store: '{updateSignatureHourStore}',
                        value: '{settings.updateSignatureSchedule[0].hour}'
                    },
                    displayField: 'name',
                    valueField: 'value'
                }, {
                    xtype: 'combo',
                    queryMode: 'local',
                    editable: false,
                    bind: {
                        store: '{updateSignatureMinuteStore}',
                        value: '{settings.updateSignatureSchedule[0].minute}'
                    },
                    displayField: 'name',
                    valueField: 'value'
                }]

            }]
        }]
    }]
});