Ext.define('Ung.apps.adblocker.view.CookieFilters', {
    extend: 'Ext.panel.Panel',
    alias: 'widget.app-adblocker-cookiefilters',
    itemId: 'cookiefilters',
    title: 'Cookie Filters'.t(),

    layout: 'border',
    border: false,

    defaults: {
        xtype: 'ungrid',
    },

    items: [{
        region: 'center',
        title: 'Standard Cookie Filters'.t(),

        listProperty: 'settings.cookies.list',
        bind: '{cookies}',

        columns: [
            Column.enabled, {
                header: 'Rule'.t(),
                width: 200,
                dataIndex: 'string',
                flex: 1
            }]
    }, {
        region: 'south',
        height: '50%',
        split: true,

        title: 'User Defined Cookie Filters'.t(),

        tbar: ['@add', '->', '@import', '@export'],
        recordActions: ['edit', 'delete'],

        listProperty: 'settings.userCookies.list',
        bind: '{userCookies}',

        emptyRow: {
            string: '',
            enabled: true,
            javaClass: 'com.untangle.uvm.node.GenericRule'
        },

        columns: [
            Column.enabled, {
                header: 'Rule'.t(),
                width: 200,
                dataIndex: 'string',
                flex: 1,
                editor: {
                    xtype: 'textfield',
                    emptyText: '[enter rule]'.t(),
                    allowBlank: false
                }
            }],
        editorFields: [{
            xtype: 'checkbox',
            bind: '{record.enabled}',
            fieldLabel: 'Enable'.t()
        }, {
            xtype: 'textfield',
            bind: '{record.string}',
            fieldLabel: 'Rule'.t(),
            emptyText: '[enter rule]'.t(),
            allowBlank: false,
            width: 400
        }]
    }]

});
