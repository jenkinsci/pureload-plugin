/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.result;

/**
 * Simple bean representing result from a test case.
 * This is either scenario result or a KPI Result.
 */
public class TestCaseResult {

   /* Result type */
   public enum Type {
      Scenario, // Result from executing a scenario
      KPI       // KPI Result
   }

   private final String name;            // Name/type etc.
   private final Type type;              // result type
   private boolean ok = true;            // Ok?
   private float execTime = -1;          // Execution time (sec)
   private String kpiMessage = "";       // KPI Status/error message

   public TestCaseResult(String name, Type type) {
      this.name = name;
      this.type = type;
   }

   public String getName() { return name; }
   public Type getType() { return type; }
   public boolean isOk() { return ok; }
   public float getExecTime() { return execTime; }
   public String getKpiMessage() { return kpiMessage; }

   public void setExecTime(float execTime) { this.execTime = execTime; }
   public void setOk(boolean ok) { this.ok = ok; }
   public void setKpiMessage(String kpiMessage) { this.kpiMessage = kpiMessage; }

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
