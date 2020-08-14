Ext.define('Ung.apps.intrusionprevention.view.Updates', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-intrusion-prevention-updates',
    itemId: 'update-signatures',

    title: 'Updates'.t(),

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
            store: Ung.apps.intrusionprevention.Main.updateSignatureFrequency,
            bind: '{settings.updateSignatureFrequency}',
            queryMode: 'local',
            editable: false,
            displayField: 'name',
            valueField: 'value'
        },
        Ung.apps['intrusionprevention'].Main.updateSignatureIsMilitaryTime,
        Ung.apps['intrusionprevention'].Main.updateSignatureDaily(),
        Ung.apps['intrusionprevention'].Main.updateSignatureWeekly(),
        Ung.apps['intrusionprevention'].Main.updateSignatureButton]
    }]
}); 