/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins;

import java.io.File;

import com.pureload.jenkins.plugin.parser.JUnitParser;
import com.pureload.jenkins.plugin.result.JUnitReport;
import com.pureload.jenkins.plugin.result.TestCaseResult;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by janne on 2017-02-15.
 */
public class JUnitParserTest {

   /** A simple test with 2 scenarios and no errors */
   @Test
   public void testJUnit1() throws Exception {
      File junitReportFile = new File(getClass().getResource("/junit-test1.xml").toURI());
      JUnitReport report = JUnitParser.parse(junitReportFile);
      TestCaseResult[] kpiResults = report.getKpiResults();
      TestCaseResult[] scenarioResults = report.getScenarioResults();
      System.out.println(report);
      Assert.assertEquals(0, kpiResults.length);
      Assert.assertEquals(2, scenarioResults.length);
      Assert.assertEquals(1.004F, scenarioResults[0].getExecTime(), 0.001F);
      Assert.assertTrue(scenarioResults[0].isOk());
   }

   /** A simple test with 2 scenarios and 2 KPIs errors */
   @Test
   public void testJUnit12() throws Exception {
      File junitReportFile = new File(getClass().getResource("/junit-test12.xml").toURI());
      JUnitReport report = JUnitParser.parse(junitReportFile);
      TestCaseResult[] kpiResults = report.getKpiResults();
      TestCaseResult[] scenarioResults = report.getScenarioResults();
      System.out.println(report);
      Assert.assertEquals(1, kpiResults.length);
      Assert.assertEquals(2, scenarioResults.length);
      Assert.assertTrue(scenarioResults[0].isOk());
   }

   @Test
   public void testJUnit13() throws Exception {
      File junitReportFile = new File(getClass().getResource("/junit-test13.xml").toURI());
      JUnitReport report = JUnitParser.parse(junitReportFile);
      TestCaseResult[] kpiResults = report.getKpiResults();
      TestCaseResult[] scenarioResults = report.getScenarioResults();
      System.out.println(report);
      Assert.assertEquals(2, kpiResults.length);
      Assert.assertEquals(1, scenarioResults.length);
      Assert.assertFalse(scenarioResults[0].isOk());
   }

   /** A simple test with 1 scenario and no errors or KPI*/
   @Test
   public void testJUnit3() throws Exception {
      File junitReportFile = new File(getClass().getResource("/junit-test3.xml").toURI());
      JUnitReport report = JUnitParser.parse(junitReportFile);
      TestCaseResult[] kpiResults = report.getKpiResults();
      TestCaseResult[] scenarioResults = report.getScenarioResults();
      System.out.println(report);
      Assert.assertEquals(0, kpiResults.length);
      Assert.assertEquals(1, scenarioResults.length);
      Assert.assertTrue(scenarioResults[0].isOk());
   }

}
