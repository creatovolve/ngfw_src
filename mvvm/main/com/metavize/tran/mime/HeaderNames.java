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
 
package com.metavize.tran.mime;


/**
 * Constants for popular header names. 
 */
public interface HeaderNames {

  public static final String CONTENT_TYPE = "Content-Type";  
  public static final String CONTENT_TRANSFER_ENCODING = "Content-Transfer-Encoding";
  public static final String SUBJECT = "Subject";
  public static final String TO = "To";
  public static final String CC = "cc";
  public static final String CONTENT_DISPOSITION = "Content-Disposition";
  
  
  public static final LCString CONTENT_TYPE_LC = new LCString(CONTENT_TYPE);
  public static final LCString CONTENT_TRANSFER_ENCODING_LC = new LCString(CONTENT_TRANSFER_ENCODING);
  public static final LCString SUBJECT_LC = new LCString(SUBJECT);
  public static final LCString TO_LC = new LCString(TO);
  public static final LCString CC_LC = new LCString(CC);
  public static final LCString CONTENT_DISPOSITION_LC = new LCString(CONTENT_DISPOSITION);  

    
}    