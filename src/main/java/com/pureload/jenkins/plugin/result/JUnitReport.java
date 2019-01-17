/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.result;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Describes result from one JUnit XML file.
 * Tis is the result from parsing a JUnit report XML file and includes a list of test case results.
 */
public class JUnitReport {

   private final String fileName;
   private final List<TestCaseResult> results = new ArrayList<>();
   private String plcFileName;
   private Date date;
   private long execTime; // Execution time (sec)
   private TestCaseResult current;

   public JUnitReport(String fileName) {
      this.fileName = fileName;
   }

   public String getFileName() { return fileName; }

   public void setPlcFileName(String plcFileName) { this.plcFileName = plcFileName; }
   public String getPlcFileName() { return plcFileName; }

   public Date getDate() { return this.date != null ? new Date(this.date.getTime()) : null; }
   public void setDate(Date date) { this.date = new Date(date.getTime()); }

   public long getExecTime() { return execTime; }
   public void setExecTime(long time) { this.execTime = time; }

   public boolean isSuccess() {
      // Check KPIs
      for (TestCaseResult result : results) {
         if (result.getType() == TestCaseResult.Type.KPI) {
            if (!result.isOk()) {
               // KPI failed; test failed
               return false;
            }
         }
      }
      // If we have KPIs this means that all are ok, and we consider this as success.
      // If we don not have any defined KPIs we can not decide, so we also treat this
      // as success.
      return true;
   }

   public TestCaseResult[] getKpiResults() { return getResults(TestCaseResult.Type.KPI); }
   public TestCaseResult[] getScenarioResults() { return getResults(TestCaseResult.Type.Scenario); }

   private TestCaseResult[] getResults(TestCaseResult.Type type) {
      List<TestCaseResult> retResults = new ArrayList<>();
      for (TestCaseResult result : results) {
         if (result.getType() == type) {
            retResults.add(result);
         }
      }
      return retResults.toArray(new TestCaseResult[0]);
   }


   public void setCurrent(TestCaseResult result) { this.current = result; }
   public TestCaseResult getCurrent() { return this.current; }
   public void addCurrent() {
      results.add(current);
      this.current = null;
   }

   @Override
   public String toString() {
      return "JUnitReport{" +
             "fileName='" + fileName + '\'' +
             ", plcFileName='" + plcFileName + '\'' +
             ", results=" + results +
             '}';
   }
}
