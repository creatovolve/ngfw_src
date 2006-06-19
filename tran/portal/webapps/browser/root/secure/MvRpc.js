// Copyright (c) 2006 Metavize Inc.
// All rights reserved.

MvRpc = { };

MvRpc.invoke = function(requestStr, serverUrl, requestHeaders, useGet,
                        actionCallback, timeoutCallback, authCallback)
{
   DBG.println("MvRpc.invoke(" + requestStr + ", " + serverUrl + ")");

   var obj = {
      actionCallback: actionCallback,
      timeoutCallback: timeoutCallback,
      authCallback: authCallback
   };

   var cb = new AjxCallback(this, MvRpc._callbackFn, obj);

   AjxRpc.invoke(requestStr, serverUrl, requestHeaders, cb, useGet);
}

// public methods -------------------------------------------------------------

MvRpc._reloadPage = function()
{
   window.location.reload();
}

MvRpc.reloadPageCallback = new AjxCallback(null, MvRpc._reloadPage, { });

// private methods ------------------------------------------------------------

MvRpc.MAGIC_RE = /<!-- MagicComment: MVTimeout -->/;

MvRpc._callbackFn = function(obj, results)
{
   if (results.xml) {
      if (obj.authCallback && "auth-error" == results.xml.firstChild.tagName) {
         return obj.authCallback.run(results);
      } else if (obj.actionCallback) {
         return obj.actionCallback.run(results);
      }
   } else if (obj.timeoutCallback && results.text && 0 <= results.text.search(MvRpc.MAGIC_RE)) {
      return obj.timeoutCallback.run(results);
   } else if (obj.actionCallback) {
      return obj.actionCallback.run(results);
   } else {
   }
}