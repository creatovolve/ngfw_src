Ext.define('Ung.view.extra.Hosts', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.hosts',

    /* requires-start */
    requires: [
        'Ung.view.extra.HostsController'
    ],
    /* requires-end */
    controller: 'hosts',

    layout: 'border',

    dockedItems: [{
        xtype: 'toolbar',
        ui: 'navigation',
        dock: 'top',
        border: false,
        style: {
            background: '#333435',
            zIndex: 9997
        },
        defaults: {
            xtype: 'button',
            border: false,
            hrefTarget: '_self'
        },
        items: Ext.Array.insert(Ext.clone(Util.subNav), 0, [{
            xtype: 'component',
            margin: '0 0 0 10',
            style: {
                color: '#CCC'
            },
            html: 'Current Hosts'.t()
        }])
    }],

    defaults: {
        border: false
    },

    viewModel: {
        data: {
            autoRefresh: false
        },
        formulas: {
            hostDetails: function (get) {
                if (get('hostsgrid.selection')) {
                    var data = get('hostsgrid.selection').getData();
                    delete data._id;
                    delete data.javaClass;
                    return data;
                }
                return;
            }
        }
    },

    items: [{
        xtype: 'ungrid',

        region: 'center',
        itemId: 'hostsgrid',
        reference: 'hostsgrid',
        title: 'Current Hosts'.t(),
        store: 'hosts',
        stateful: true,

        enableColumnHide: true,
        forceFit: false,
        viewConfig: {
            stripeRows: true,
            enableTextSelection: true
        },

        plugins: ['gridfilters'],

        columns: [{
            header: 'Address'.t(),
            dataIndex: 'address',
            resizable: false,
            filter: { type: 'string' }
        },{
            header: 'MAC'.t(),
            columns:[{
                header: 'Address'.t(),
                dataIndex: 'macAddress',
                filter: { type: 'string' }
            },{
                header: 'Vendor'.t(),
                dataIndex: 'macVendor',
                filter: { type: 'string' }
            }]
        },{
            header: 'Interface'.t(),
            dataIndex: 'interfaceId',
            filter: { type: 'string' },
            rtype: 'interface'
        },{
            header: 'Creation Time'.t(),
            dataIndex: 'creationTimeDate',
            hidden: true,
            rtype: 'timestamp',
            filter: { type: 'date' },
        },{
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTime',
            hidden: true,
            rtype: 'timestamp',
            filter: { type: 'date' },
        },{
            header: 'Last Session Time'.t(),
            dataIndex: 'lastSessionTime',
            hidden: true,
            rtype: 'timestamp',
            filter: { type: 'date' },
        },{
            header: 'Last Completed TCP Session Time'.t(),
            dataIndex: 'lastCompletedTcpSessionTime',
            hidden: true,
            rtype: 'timestamp',
            filter: { type: 'date' },
        },{
            header: 'Entitled Status'.t(),
            dataIndex: 'entitled',
            hidden: true,
            rtype: 'boolean',
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
        },{
            header: 'Active'.t(),
            dataIndex: 'active',
            width: 80,
            rtype: 'boolean',
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
        },{
            header: 'HTTP User Agent'.t(),
            dataIndex: 'httpUserAgent',
            filter: { type: 'string' },
        },{
            header: 'Captive Portal Authenticated'.t(),
            dataIndex: 'captivePortalAuthenticated',
            rtype: 'boolean',
            filter: {
                type: 'boolean',
                yesText: 'true',
                noText: 'false'
            },
        },{
            header: 'Tags'.t(),
            dataIndex: 'tags',
            rtype: 'tags'
        },{
            header: 'Tags String'.t(),
            dataIndex: 'tagsString'
        },{
            header: 'Hostname'.t(),
            dataIndex: 'hostname',
            filter: { type: 'string' },
        },{
            header: 'Hostname Source'.t(),
            dataIndex: 'hostnameSource',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (DHCP)'.t(),
            dataIndex: 'hostnameDhcp',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (DNS)'.t(),
            dataIndex: 'hostnameDns',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (Device)'.t(),
            dataIndex: 'hostnameDevice',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (Device Last Known)'.t(),
            dataIndex: 'hostnameDeviceLastKnown',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (OpenVPN)'.t(),
            dataIndex: 'hostnameOpenVpn',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (Reports)'.t(),
            dataIndex: 'hostnameReports',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Hostname (Directory Connector)'.t(),
            dataIndex: 'hostnameDirectoryConnector',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Username'.t(),
            dataIndex: 'username',
            filter: { type: 'string' },
        },{
            header: 'Username Source'.t(),
            dataIndex: 'usernameSource',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Username (Directory Connector)'.t(),
            dataIndex: 'usernameDirectoryConnector',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Username (Captive Porrtal)'.t(),
            dataIndex: 'usernameCaptivePortal',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Username (Device)'.t(),
            dataIndex: 'usernameDevice',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Username (OpenVPN)'.t(),
            dataIndex: 'usernameOpenVpn',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Username (IPsec VPN)'.t(),
            dataIndex: 'usernameIpsecVpn',
            hidden: true,
            filter: { type: 'string' },
        },{
            header: 'Quota'.t(),
            columns: [{
                header: 'Size'.t(),
                dataIndex: 'quotaSize',
                filter: 'number',
            },{
                header: 'Remaining'.t(),
                dataIndex: 'quotaRemaining',
                filter: 'number',
            },{
                header: 'Issue Time'.t(),
                dataIndex: 'quotaIssueTime',
                hidden: true
            },{
                header: 'Expiration Time'.t(),
                dataIndex: 'quotaExpirationTime',
                hidden: true,
                rtype: 'timestamp',
                filter: { type: 'date' },
            }]
        }]
    }, {
        region: 'east',
        xtype: 'unpropertygrid',
        title: 'Host Details'.t(),
        itemId: 'details',

        bind: {
            source: '{hostDetails}'
        }
    }],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getHosts',
        bind: {
            disabled: '{autoRefresh}'
        }
    }, {
        xtype: 'button',
        text: 'Auto Refresh'.t(),
        bind: {
            iconCls: '{autoRefresh ? "fa fa-check-square-o" : "fa fa-square-o"}'
        },
        enableToggle: true,
        toggleHandler: 'setAutoRefresh'
    }, {
        xtype: 'button',
        text: 'Reset View'.t(),
        iconCls: 'fa fa-refresh',
        itemId: 'resetBtn',
        handler: 'resetView',
    }, '-', 'Filter:'.t(), {
        xtype: 'textfield',
        checkChangeBuffer: 200
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/hosts',
        hrefTarget: '_self'
    }]
});
