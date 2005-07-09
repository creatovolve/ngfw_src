/*
 * Copyright (c) 2005 Metavize Inc.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information of
 * Metavize Inc. ("Confidential Information").  You shall
 * not disclose such Confidential Information.
 *
 * $Id:$
 */

package com.metavize.tran.util;


/**
 * Implementation of TemplateValues which acts as 
 * a map (really, a java.util.Properties).
 *
 * @see Template
 */
public class MapTemplateValues 
  extends java.util.Properties
  implements TemplateValues {


  public String getTemplateValue(String key) {
    return getProperty(key);
  }

}