Ext.define('Ung.apps.intrusionprevention.view.UpdateSignatures', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-intrusion-prevention-update-signatures',
    itemId: 'update-signatures',

    title: 'Update Signatures'.t(),

    scrollable: true,
    padding: 10,

    items: [{
        title: 'Update Signatures and Signatures Schedule'.t(),
        items: [{
            xtype: 'component',
            html: 'You can manually update signatures or set a schedule.'
        }, {
            xtype: 'combo',
            fieldLabel: 'Frequency'.t(),
            labelAlign: 'right',
            width: 400,
            bind: {
                store: '{updateSignatureFrequencyStore}',
                value: '{settings.updateSignatureFrequency}'
            },
            queryMode: 'local',
            editable: false,
            displayField: 'name',
            valueField: 'value'
        }]
    }]
});