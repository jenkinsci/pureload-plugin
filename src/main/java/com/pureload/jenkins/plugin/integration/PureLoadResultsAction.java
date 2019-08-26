/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.integration;

import java.util.List;
import java.util.logging.Logger;

import com.pureload.jenkins.plugin.result.PureLoadResult;
import hudson.model.Action;
import hudson.model.Run;

/**
 * Action used to set/get JUnit report.
 */
@SuppressWarnings("WeakerAccess")
public class PureLoadResultsAction implements Action {
   private static final Logger LOGGER = Logger.getLogger(PureLoadResultsAction.class.getName());

   private final Run<?, ?> build;
   private List<PureLoadResult> pureloadResults;

   PureLoadResultsAction(final Run<?, ?> build) {
      this.build = build;
   }

   public Run<?, ?> getBuild() {
      return build;
   }

   public void setPureLoadResults(List<PureLoadResult> pureloadResults) {
      this.pureloadResults = pureloadResults;
   }

   @SuppressWarnings("unused") // used by index.jelly
   public List<PureLoadResult> getPureLoadResults() {
      if ((pureloadResults == null) || (pureloadResults.isEmpty())) {
         LOGGER.severe("Can not get parsed results(s)");
      }
      return pureloadResults;
   }

   @Override
   public String getIconFileName() { return "/plugin/pureload/images/check.png"; }
   @Override
   public String getDisplayName() { return "PureLoad Results"; }
   @Override
   public String getUrlName() { return "pureload-results"; }
}
