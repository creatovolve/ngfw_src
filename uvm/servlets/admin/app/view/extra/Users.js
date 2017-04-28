Ext.define('Ung.view.extra.Users', {
    extend: 'Ext.panel.Panel',
    xtype: 'ung.users',

    /* requires-start */
    requires: [
        'Ung.view.extra.UsersController'
    ],
    /* requires-end */
    controller: 'users',

    viewModel: {
        data: {
            autoRefresh: false,
            usersData: []
        },
        stores: {
            users: {
                data: '{usersData}'
            }
        },
        formulas: {
            userDetails: function (get) {
                this.userDetailsGet = get;
                this.getView().down('#usersgrid').getSelectionModel().select(0);
                this.userDetailsSelectTask = new Ext.util.DelayedTask( Ext.bind(function(){
                    this.getView().down('#usersgrid').getSelectionModel().select(0);
                    if( !this.userDetailsGet('usersgrid.selection') ){
                        this.userDetailsSelectTask.delay(100);
                    }
                }, this) );
                this.userDetailsSelectTask.delay(100);
                if (get('usersgrid.selection')) {
                    var data = get('usersgrid.selection').getData();
                    delete data._id;
                    delete data.javaClass;
                    for( var k in data ){
                        /*
                         * Encode objects and arrays for details
                         */
                        if( ( typeof( data[k] ) == 'object' ) ||
                            ( typeof( data[k] ) == 'array' ) ){
                            data[k] = Ext.encode(data[k]);
                        }
                    }
                    return data;
                }
                return;
            }
        }
    },

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
            html: 'Current Users'.t()
        }])
    }],

    defaults: {
        border: false
    },

    items: [{
        region: 'center',
        xtype: 'ungrid',
        itemId: 'usersgrid',
        reference: 'usersgrid',
        title: 'Current Users'.t(),
        stateful: true,

        sortField: 'username',
        sortOrder: 'ASC',

        plugins: ['gridfilters'],
        columnLines: true,

        enableColumnHide: true,
        forceFit: false,
        viewConfig: {
            stripeRows: true,
            enableTextSelection: true,
            emptyText: '<p style="text-align: center; margin: 0; line-height: 2;"><i class="fa fa-info-circle fa-2x"></i> <br/>No Data!</p>',
        },

        bind: '{users}',

        columns: [{
            header: 'Username'.t(),
            dataIndex: 'username',
            filter: {
                type: 'string'
            }
        }, {
            header: 'Creation Time'.t(),
            dataIndex: 'creationTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Last Access Time'.t(),
            dataIndex: 'lastAccessTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Last Session Time'.t(),
            dataIndex: 'lastSessionTime',
            rtype: 'timestamp',
            filter: {
                type: 'date'
            }
        }, {
            header: 'Quota'.t(),
            columns: [{
                header: 'Quota Size'.t(),
                dataIndex: 'quotaSize',
                renderer: function (value) {
                    return value === 0 || value === '' ? '' : value;
                },
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Quota Remaining'.t(),
                dataIndex: 'quotaRemaining',
                filter: {
                    type: 'numeric'
                }
            }, {
                header: 'Quota Issue Time'.t(),
                dataIndex: 'quotaIssueTime',
                rtype: 'timestamp',
                filter: {
                    type: 'date'
                }
            }, {
                header: 'Quota Expiration Time'.t(),
                dataIndex: 'quotaExpirationTime',
                rtype: 'timestamp',
                filter: {
                    type: 'date'
                }
            }]
        }, {
            header: 'Tags'.t(),
            dataIndex: 'tags',
            filter: {
                type: 'string'
            },
            rtype: 'tags'
        }, {
            header: 'Tags String'.t(),
            dataIndex: 'tagsString',
            hidden: true,
            filter: {
                type: 'string'
            }
        }]
    }, {
        region: 'east',
        xtype: 'unpropertygrid',
        title: 'User Details'.t(),
        itemId: 'details',

        bind: {
            source: '{userDetails}'
        }
    }],
    tbar: [{
        xtype: 'button',
        text: 'Refresh'.t(),
        iconCls: 'fa fa-repeat',
        handler: 'getUsers',
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
    }, '->', {
        xtype: 'button',
        text: 'View Reports'.t(),
        iconCls: 'fa fa-line-chart',
        href: '#reports/users',
        hrefTarget: '_self'
    }]
});
