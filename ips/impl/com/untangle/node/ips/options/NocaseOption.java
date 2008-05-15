/*
 * $HeadURL$
 * Copyright (c) 2003-2007 Untangle, Inc.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, version 2,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package com.untangle.node.ips.options;

import org.apache.log4j.Logger;

public class NocaseOption extends IPSOption
{
    private final Logger logger = Logger.getLogger(getClass());

    public NocaseOption(OptionArg arg)
    {
        super(arg);
        String[] parents = new String [] { "ContentOption", "UricontentOption" };
        IPSOption option = signature.getOption(parents, this);
        if(option == null) {
            logger.warn("Unable to find content option to set nocase for sig: " + arg.getRule().getText());
            return;
        }

        if (option instanceof ContentOption) {
            ContentOption content = (ContentOption) option;
            content.setNoCase();
        } else if (option instanceof UricontentOption) {
            UricontentOption uricontent = (UricontentOption) option;
            uricontent.setNoCase();
        }
    }
}
