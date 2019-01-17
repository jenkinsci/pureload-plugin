/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.integration;

import java.util.logging.Logger;

import com.pureload.jenkins.plugin.result.JUnitReport;
import hudson.model.Action;
import hudson.model.Run;

/**
 * Action used to set/get JUnit report.
 */
@SuppressWarnings("WeakerAccess")
public class PureLoadResultsAction implements Action {

   private static final Logger LOGGER = Logger.getLogger(PureLoadResultsAction.class.getName());

   private final Run<?, ?> build;
   private JUnitReport report;

   PureLoadResultsAction(final Run<?, ?> build) {
      this.build = build;
   }

   public Run<?, ?> getBuild() {
      return build;
   }

   public void setReport(JUnitReport report) {
      this.report = report;
   }

   public JUnitReport getReport() {
      if (report == null) {
         LOGGER.severe("Can not get parsed report");
      }
      return report;
   }


   @Override
   public String getIconFileName() {
      return "/plugin/pureload/images/check.png";
   }
   @Override
   public String getDisplayName() {
      return "PureLoad Results";
   }
   @Override
   public String getUrlName() {
      return "pureload-results";
   }


}
