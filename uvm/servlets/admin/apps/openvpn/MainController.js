Ext.define('Ung.apps.openvpn.MainController', {
    extend: 'Ext.app.ViewController',
    alias: 'controller.app-openvpn',

    control: {
        '#': {
            afterrender: 'getSettings'
        },
        '#status': {
            beforerender: 'onStatusBeforeRender'
        }
    },

    onStatusBeforeRender: function () {
        var me = this,
            vm = this.getViewModel();
        vm.bind('{instance.targetState}', function (state) {
            if (state === 'RUNNING') {
                me.getActiveClients();
                me.getRemoteServersStatus();
            }
        });
    },

    getActiveClients: function () {
        var grid = this.getView().down('#activeClients'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getActiveClients(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('activeClients', result.list);
        });
    },

    getRemoteServersStatus: function () {
        var grid = this.getView().down('#remoteServers'),
            vm = this.getViewModel();
        grid.setLoading(true);
        this.getView().appManager.getRemoteServersStatus(function (result, ex) {
            grid.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('remoteServers', result.list);
            console.log(result);
        });
    },

    getSettings: function () {
        var v = this.getView(), vm = this.getViewModel();
        v.setLoading(true);
        v.appManager.getSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            vm.set('settings', result);
        });
    },

    setSettings: function () {
        var me = this, v = this.getView(), vm = this.getViewModel();

        v.query('ungrid').forEach(function (grid) {
            var store = grid.getStore();
            if (store.getModifiedRecords().length > 0 ||
                store.getNewRecords().length > 0 ||
                store.getRemovedRecords().length > 0 ||
                store.isReordered) {
                store.each(function (record) {
                    if (record.get('markedForDelete')) {
                        record.drop();
                    }
                });
                store.isReordered = undefined;
                vm.set(grid.listProperty, Ext.Array.pluck(store.getRange(), 'data'));
            }
        });

        v.setLoading(true);
        v.appManager.setSettings(function (result, ex) {
            v.setLoading(false);
            if (ex) { Util.exceptionToast(ex); return; }
            Util.successToast('Settings saved');
            me.getSettings();
        }, vm.get('settings'));
    },

    uploadFile: function(cmp) {
        var form = Ext.ComponentQuery.query('form[name=upload_form]')[0];
        var file = Ext.ComponentQuery.query('textfield[name=uploadConfigFileName]')[0].value;
        if ( file == null || file.length === 0 ) {
            Ext.MessageBox.alert('Select File'.t(), 'Please choose a file to upload.'.t());
            return;
            }
        form.submit({
            url: "/openvpn/uploadConfig",
            success: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Success'.t(), 'The configuration has been imported.'.t());
                this.getSettings();
            }, this),
            failure: Ext.bind(function( form, action ) {
                Ext.MessageBox.alert('Failure'.t(), 'Import failure'.t() + ": " + action.result.code);
            }, this)
        });
    },

});

Ext.define('Ung.apps.openvpn.SpecialGridController', {
    extend: 'Ung.cmp.GridController',
    alias: 'controller.app-openvpn-special',

    downloadClient: function(view, row, colIndex, item, e, record) {
        var me = this, v = this.getView(), vm = this.getViewModel();
        me.getDistributeWindow().populate(record);
    },

getDistributeWindow: function() {
    this.distributeWindow = Ext.create('Ext.window.Window', {
        title: 'Download OpenVPN Client'.t(),
        items: [{
            xtype: 'panel',
                items: [{
                    xtype: 'fieldset',
                    title: 'Download'.t(),
                    margin: 10,
                    defaults: { margin: 10 },
                        items: [{
                            xtype: 'component',
                            html: 'These files can be used to configure your Remote Clients.'.t(),
                        }, {
                            xtype: 'component',
                            name: 'downloadWindowsInstaller',
                            html:  " "
                        }, {
                            xtype: 'component',
                            name: 'downloadGenericConfigurationFile',
                            html: " "
                        }, {
                            xtype: 'component',
                            name: 'downloadUntangleConfigurationFile',
                            html: " "
                        }, {
                            xtype: 'component',
                            html: '<BR>'
                        }, {
                            xtype: 'component',
                            html: 'This file can be used to configure Chromebook clients.  On the target device, browse to <b>chrome://net-internals#chromeos</b> and use Import ONC file.'.t(),
                        }, {
                            xtype: 'component',
                            name: 'downloadChromebookConfigurationFile',
                            html: " "
                        }]
                    }]
                }],
                bbar: ['->', {
                    name: 'close',
                    iconCls: 'cancel-icon',
                    text: 'Close'.t(),
                    handler: function() {
                        this.distributeWindow.close();
                    },
                    scope: this
                }],
                closeWindow: function() {
                    this.destroy();
                },
                populate: function( record ) {
                    this.record = record;
                    this.setTitle('Download OpenVPN Client'.t() + ' | ' + record.data.name);

                    var windowsLink = this.down('[name="downloadWindowsInstaller"]');
                    var genericLink = this.down('[name="downloadGenericConfigurationFile"]');
                    var untangleLink = this.down('[name="downloadUntangleConfigurationFile"]');
                    var chromebookLink = this.down('[name="downloadChromebookConfigurationFile"]');

                    windowsLink.update('Loading...'.t());
                    genericLink.update('Loading...'.t());
                    untangleLink.update('Loading...'.t());
                    chromebookLink.update('Loading...'.t());

                    Ext.MessageBox.wait("Building OpenVPN Client...".t(), "Please Wait".t());
                    var openvpnApp = rpc.appManager.app('openvpn');
                    var loadSemaphore = 3;

                    openvpnApp.getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                        if (exception) { Util.exceptionToast(exception); return; }
                        windowsLink.update('<a href="'+result+'" target="_blank">'+'Click here to download this client\'s Windows setup.exe file.'.t() + '</a>');
                        if(--loadSemaphore == 0) { Ext.MessageBox.hide();}
                    }, this), this.record.data.name, "exe" );

                    openvpnApp.getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                        if (exception) { Util.exceptionToast(exception); return; }
                        chromebookLink.update('<a href="'+result+'" target="_blank">'+'Click here to download this client\'s configuration onc file for Chromebook.'.t() + '</a>');
                        if(--loadSemaphore == 0) { Ext.MessageBox.hide();}
                    }, this), this.record.data.name, "onc" );

                    openvpnApp.getClientDistributionDownloadLink( Ext.bind(function(result, exception) {
                        if (exception) { Util.exceptionToast(exception); return; }
                        genericLink.update('<a href="'+result+'" target="_blank">'+'Click here to download this client\'s configuration zip file for other OSs (apple/linux/etc).'.t() + '</a>');
                        untangleLink.update('<a href="'+result+'" target="_blank">'+'Click here to download this client\'s configuration file for remote Untangle OpenVPN clients.'.t() + '</a>');
                        if(--loadSemaphore == 0) { Ext.MessageBox.hide();}
                    }, this), this.record.data.name, "zip" );
                }
            });

        this.distributeWindow.show();
        return this.distributeWindow;
    },

});
