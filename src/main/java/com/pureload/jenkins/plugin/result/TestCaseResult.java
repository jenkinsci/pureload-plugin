/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.result;

import java.util.Date;

/**
 * Simple bean representing result from a test case.
 * This is either scenario result or a KPI Result.
 */
public class TestCaseResult {
   /** Result type */
   public enum Type {
      Scenario, // Result from executing a scenario
      KPI       // KPI Result
   }

   private final String name;            // Name/type etc.
   private final Type type;              // result type
   private boolean ok = true;            // Ok?
   private float execTime = -1;          // Execution time (sec)

   // KPI specific attributes:
   private String kpiMessage = "";
   private String kpiStatus = "";        // KPI status (Low/OK/High/Failed)
   private String kpiValue = "";
   private String kpiThreshold = "";
   private long kpiTimestamp = 0;

   public TestCaseResult(String name, Type type) {
      this.name = name;
      this.type = type;
   }

   public String getName() { return name; }
   public Type getType() { return type; }

   public boolean isOk() { return ok; }
   public void setOk(boolean ok) { this.ok = ok; }
   public float getExecTime() { return execTime; }
   public void setExecTime(float execTime) { this.execTime = execTime; }
   public String getKpiMessage() { return kpiMessage; }
   public void setKpiMessage(String kpiMessage) { this.kpiMessage = kpiMessage; }
   public String getKpiStatus() { return kpiStatus; }
   public void setKpiStatus(String kpiStatus) { this.kpiStatus = kpiStatus; }
   public String getKpiValue() { return kpiValue; }
   public void setKpiValue(String kpiValue) { this.kpiValue = kpiValue; }
   public String getKpiThreshold() { return kpiThreshold; }
   public void setKpiThreshold(String kpiThreshold) { this.kpiThreshold = kpiThreshold; }
   public long getKpiTimestamp() { return kpiTimestamp; }
   public void setKpiTimestamp(long kpiTimestamp) { this.kpiTimestamp = kpiTimestamp; }
   public Date getKpiTimestampDate() { return new Date(this.kpiTimestamp); }


   @Override
   public String toString() {
      String ret = "TestCaseResult {" +
                   "name='" + name + '\'' +
                   ", type=" + type +
                   ", ok=" + ok;
      if (execTime > -1) {
         ret += ", execTime=" + execTime;
      }
      if (kpiMessage != null) {
         ret += ", KPI msg=" + kpiMessage;
      }
      ret += " }";
      return ret;
   }
}
