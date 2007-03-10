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

package com.untangle.tran.httpblocker;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.sleepycat.je.DatabaseException;
import com.untangle.mvvm.tran.IPMaddrRule;
import com.untangle.mvvm.tran.MimeType;
import com.untangle.mvvm.tran.MimeTypeRule;
import com.untangle.mvvm.tran.StringRule;
import com.untangle.tran.http.RequestLineToken;
import com.untangle.tran.token.Header;
import com.untangle.tran.util.CharSequenceUtil;
import com.untangle.tran.util.PrefixUrlList;
import com.untangle.tran.util.UrlDatabase;
import com.untangle.tran.util.UrlList;
import org.apache.log4j.Logger;

/**
 * Does blacklist lookups in the database.
 *
 * @author <a href="mailto:amread@untangle.com">Aaron Read</a>
 * @version 1.0
 */
class Blacklist
{
    private static final String BLACKLIST_HOME = "file:///var/lib/urlblacklist.com/";
    private static final File DB_HOME = new File(System.getProperty("bunnicula.db.dir"), "httpblocker");

    private final Logger logger = Logger.getLogger(Blacklist.class);

    private final HttpBlockerImpl transform;

    private final UrlDatabase<String> urlDatabase = new UrlDatabase<String>();

    private volatile HttpBlockerSettings settings;
    private volatile String[] blockedUrls = new String[0];
    private volatile String[] passedUrls = new String[0];

    // XXX support expressions

    // constructors -----------------------------------------------------------

    Blacklist(HttpBlockerImpl transform)
    {
        this.transform = transform;
    }

    // blacklist methods ------------------------------------------------------

    void configure(HttpBlockerSettings settings)
    {
        this.settings = settings;
    }

    synchronized void reconfigure()
    {
        urlDatabase.clear();

        for (BlacklistCategory cat : settings.getBlacklistCategories()) {
            String catName = cat.getName();
            if (cat.getBlockDomains()) {
                String dbName = catName + "-domains";
                try {
                    URL url = new URL(BLACKLIST_HOME + catName + "/domains");
                    UrlList ul = new PrefixUrlList(DB_HOME, dbName, url);
                    urlDatabase.addBlacklist(dbName, ul);
                } catch (IOException exn) {
                    logger.warn("could not open: " + dbName, exn);
                } catch (DatabaseException exn) {
                    logger.warn("could not open: " + dbName, exn);
                }
            }

            if (cat.getBlockUrls()) {
                String dbName = catName + "-urls";
                try {
                    URL url = new URL(BLACKLIST_HOME + catName + "/urls");
                    UrlList ul = new PrefixUrlList(DB_HOME, dbName, url);
                    urlDatabase.addBlacklist(dbName, ul);
                } catch (IOException exn) {
                    logger.warn("could not open: " + dbName, exn);
                } catch (DatabaseException exn) {
                    logger.warn("could not open: " + dbName, exn);
                }
            }
        }

        urlDatabase.initAll(true);

        blockedUrls = makeCustomList(settings.getBlockedUrls());
        passedUrls = makeCustomList(settings.getPassedUrls());
    }

    void destroy() { }

    /**
     * Checks if the request should be blocked, giving an appropriate
     * response if it should.
     *
     * @param host the requested host.
     * @param path the requested path.
     * @return an HTML response.
     */
    String checkRequest(InetAddress clientIp,
                        RequestLineToken requestLine, Header header)
    {
        URI uri = requestLine.getRequestUri().normalize();

        String path = uri.getPath();
        path = null == path ? "" : uri.getPath().toLowerCase();

        String host = uri.getHost();
        if (null == host) {
            host = header.getValue("host");
            if (null == host) {
                host = clientIp.getHostAddress();
            }
        }

        host = host.toLowerCase();
        while (0 < host.length() && '.' == host.charAt(host.length() - 1)) {
            host = host.substring(0, host.length() - 1);
        }

        String passCategory = passClient(clientIp);

        if (null != passCategory) {
            HttpBlockerEvent hbe = new HttpBlockerEvent
                (requestLine.getRequestLine(), Action.PASS, Reason.PASS_CLIENT,
                 passCategory);
            logger.info(hbe);
            return null;
        } else {
            String dom = host;
            while (null != dom) {
                StringRule sr = findCategory(passedUrls, dom + path,
                                             settings.getPassedUrls());
                String category = null == sr ? null : sr.getCategory();

                if (null != category) {
                    HttpBlockerEvent hbe = new HttpBlockerEvent
                        (requestLine.getRequestLine(), Action.PASS,
                         Reason.PASS_URL, category);
                    transform.log(hbe);

                    return null;
                }
                dom = nextHost(dom);
            }
        }

        // check in HttpBlockerSettings
        String nonce = checkBlacklist(host, requestLine);

        if (null != nonce) {
            return nonce;
        }

        // Check Extensions
        for (StringRule rule : settings.getBlockedExtensions()) {
            String exn = rule.getString().toLowerCase();
            if (rule.isLive() && path.endsWith(exn)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("blocking extension " + exn);
                }
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_EXTENSION, exn);
                transform.log(hbe);

                HttpBlockerBlockDetails bd = new HttpBlockerBlockDetails
                    (settings, host, uri.toString(),
                     "extension (" + exn + ")");
                return transform.generateNonce(bd);
            }
        }

        return null;
    }

    String checkResponse(InetAddress clientIp, RequestLineToken requestLine,
                         Header header)
    {
        if (null == requestLine) {
            return null;
        } else if (null != passClient(clientIp)) {
            return null;
        }

        String contentType = header.getValue("content-type");

        for (MimeTypeRule rule : settings.getBlockedMimeTypes()) {
            MimeType mt = rule.getMimeType();
            if (rule.isLive() && mt.matches(contentType)) {
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK,
                     Reason.BLOCK_MIME, contentType);
                transform.log(hbe);
                String host = header.getValue("host");
                URI uri = requestLine.getRequestUri().normalize();

                HttpBlockerBlockDetails bd = new HttpBlockerBlockDetails
                    (settings, host, uri.toString(),
                     "Mime-Type (" + contentType + ")");
                return transform.generateNonce(bd);
            }
        }

        HttpBlockerEvent e = new HttpBlockerEvent(requestLine.getRequestLine(),
                                                  null, null, null, true);
        transform.log(e);

        return null;
    }

    // private methods --------------------------------------------------------

    /**
     * Check if client is whitelisted.
     *
     * @param clientIp address of the client machine.
     * @return true if the client is whitelisted.
     */
    private String passClient(InetAddress clientIp)
    {
        for (IPMaddrRule rule : settings.getPassedClients()) {
            if (rule.getIpMaddr().contains(clientIp) && rule.isLive()) {
                return rule.getCategory();
            }
        }

        return null;
    }

    private String checkBlacklist(String host,
                                  RequestLineToken requestLine)
    {
        String uri = requestLine.getRequestUri().normalize().toString();
        String category = null;
        StringRule stringRule = null;
        Reason reason = null;

        if (settings.getFascistMode()) {
            String c = "All Web Content";
            Reason r = Reason.BLOCK_ALL;
            HttpBlockerEvent hbe = new HttpBlockerEvent
                (requestLine.getRequestLine(), Action.BLOCK, r, c);
            transform.log(hbe);

            HttpBlockerBlockDetails bd = new HttpBlockerBlockDetails
                (settings, host, uri, "not allowed");
            return transform.generateNonce(bd);
        }

        String dom = host;
        while (null == category && null != dom) {
            String url = dom + uri;

            stringRule = findCategory(blockedUrls, url,
                                      settings.getBlockedUrls());
            category = null == stringRule ? null : stringRule.getCategory();
            if (null != category) {
                reason = Reason.BLOCK_URL;
            } else {
                List<String> all = urlDatabase.findAllBlacklisted("http", dom, uri);
                category = mostSpecificCategory(all);

                if (null != category) {
                    reason = Reason.BLOCK_CATEGORY;
                }
            }

            if (null == category) {
                dom = nextHost(dom);
            }
        }

        if (null != category) {
            if (null == stringRule || stringRule.getLog()) {
                HttpBlockerEvent hbe = new HttpBlockerEvent
                    (requestLine.getRequestLine(), Action.BLOCK, reason, category);
                transform.log(hbe);
            }

            BlacklistCategory bc = settings.getBlacklistCategory(category);
            if (null == bc && null != stringRule && !stringRule.isLive()) {
                return null;
            } else if (null != bc && bc.getLogOnly()) {
                return null;
            } else {
                HttpBlockerBlockDetails bd = new HttpBlockerBlockDetails
                    (settings, host, uri, category);
                return transform.generateNonce(bd);
            }
        }

        return null;
    }

    private String mostSpecificCategory(List<String> dbNames) {
        String category = null;
        if (dbNames != null)
            for (String dbName : dbNames) {
                int i = dbName.indexOf('-');
                String cat = 0 > i ? dbName : dbName.substring(0, i);

                if (category == null) {
                    category = cat;
                } else {
                    BlacklistCategory bc = settings.getBlacklistCategory(cat);
                    if (null == bc || bc.getLogOnly()) {
                        continue;
                    }
                    category = cat;
                }
            }
        return category;
    }

    private StringRule findCategory(CharSequence[] strs, String val,
                                    List<StringRule> rules)
    {
        int i = findMatch(strs, val);
        return 0 > i ? null : lookupCategory(strs[i], rules);
    }

    private String[] findCategories(Map<String, CharSequence[]> cats, String val)
    {
        List<String> result = new ArrayList<String>();
        for (String cat : cats.keySet()) {
            CharSequence[] strs = cats.get(cat);
            int i = findMatch(strs, val);
            if (0 <= i)
                result.add(cat);
        }
        if (result.size() == 0) {
            return null;
        } else {
            return (String[]) result.toArray(new String[result.size()]);
        }
    }

    private int findMatch(CharSequence[] strs, String val)
    {
        if (null == val || null == strs) {
            return -1;
        }

        int i = Arrays.binarySearch(strs, val, CharSequenceUtil.COMPARATOR);
        if (0 <= i) {
            return i;
        } else {
            int j = -i - 2;
            if (0 <= j && j < strs.length
                && CharSequenceUtil.startsWith(val, strs[j])) {
                return j;
            }
        }

        return -1;
    }

    private StringRule lookupCategory(CharSequence match,
                                      List<StringRule> rules)

    {
        for (StringRule rule : rules) {
            String uri = normalizeDomain(rule.getString());

            if ((rule.isLive() || rule.getLog()) && match.equals(uri)) {
                return rule;
            }
        }

        return null;
    }

    /**
     * Gets the next domain stripping off the lowest level domain from
     * host. Does not return the top level domain. Returns null when
     * no more domains are left.
     *
     * <b>This method assumes trailing dots are stripped from host.</b>
     *
     * @param host a <code>String</code> value
     * @return a <code>String</code> value
     */
    private String nextHost(String host)
    {
        int i = host.indexOf('.');
        if (-1 == i) {
            return null;
        } else {
            int j = host.indexOf('.', i + 1);
            if (-1 == j) { // skip tld
                return null;
            }

            return host.substring(i + 1);
        }
    }

    private String[] makeCustomList(List<StringRule> rules)
    {
        List<String> strings = new ArrayList<String>(rules.size());
        for (StringRule rule : rules) {
            if (rule.isLive()) {
                String uri = normalizeDomain(rule.getString());
                strings.add(uri);
            }
        }
        Collections.sort(strings);

        return strings.toArray(new String[strings.size()]);
    }

    private String normalizeDomain(String dom)
    {
        String url = dom.toLowerCase();
        String uri = url.startsWith("http://")
            ? url.substring("http://".length()) : url;

        while (0 < uri.length()
               && ('*' == uri.charAt(0) || '.' == uri.charAt(0))) {
            uri = uri.substring(1);
        }

        if (uri.startsWith("www.")) {
            uri = uri.substring("www.".length());
        }

        return uri;
    }
}
