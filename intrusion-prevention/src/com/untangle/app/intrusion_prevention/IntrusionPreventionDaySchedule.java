/**
 * $Id$
 */
package com.untangle.app.intrusion_prevention;

import org.json.JSONObject;
import org.json.JSONString;
import java.io.Serializable;

/**
    Class to hold the manual schedule for intrusion prevention
 */
@SuppressWarnings("serial")
public class IntrusionPreventionDaySchedule implements Serializable, JSONString
{
    private Integer hour;
    private Integer minute;

   /**
      Constructor
    */
   public IntrusionPreventionDaySchedule(Integer... schedule) {

      switch(schedule.length) {
         case 1:
            this.hour = schedule[0];
            this.minute = 0;
            break;
         case 2:
            this.hour = schedule[0];
            this.minute = schedule[1];
            break;
         default:
            this.hour = -1;
            this.minute = -1;
            break;
      }
   }

   public Integer getHour() { return this.hour; }
   public void setHour(Integer hour) { this.hour = hour; }

   public Integer getMinute() { return this.hour; }
   public void setMinute(Integer minute) { this.minute = minute; }

   /**
     * Returns daySchedule as a JSON string.
     *
     * @return
     *      Server daySchedule in JSON form.
     */
    public String toJSONString()
    {
        JSONObject jO = new JSONObject(this);
        return jO.toString();
    }

}