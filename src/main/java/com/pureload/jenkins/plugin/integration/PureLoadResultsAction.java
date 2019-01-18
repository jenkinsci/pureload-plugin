/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.integration;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.logging.Logger;

import com.pureload.jenkins.plugin.result.JUnitReport;
import hudson.model.Action;
import hudson.model.Run;
import jenkins.util.VirtualFile;

/**
 * Action used to set/get JUnit report.
 */
@SuppressWarnings("WeakerAccess")
public class PureLoadResultsAction implements Action {

   private static final Logger LOGGER = Logger.getLogger(PureLoadResultsAction.class.getName());

   private final Run<?, ?> build;
   private JUnitReport report;
   private String totalSummaryHtml;
   private String totalSummaryTableHtml;

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

   public void setTotalSummary(VirtualFile file) {
      try {
         doReadTotalSummary(file);
      }
      catch (IOException e) {
         LOGGER.severe("Failed extracting HTML from execution report: " + e);
      }
   }

   public String getTotalSummaryHtml() {
      if (totalSummaryHtml == null) {
         LOGGER.severe("Can not access total summary");
         return "";
      }
      return totalSummaryHtml;
   }

   public String getTotalSummaryTableHtml() {
      if (totalSummaryTableHtml == null) {
         LOGGER.severe("Can not access total summary table");
         return "";
      }
      return totalSummaryTableHtml;
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

   private static void debug(String msg, Object... args) {
      LOGGER.fine(MessageFormat.format(msg, args));
   }

   private void doReadTotalSummary(VirtualFile file) throws IOException {
      if (file.isFile() && file.canRead()) {
         InputStream is = file.open();
         try {
            doReadTotalSummary(file.open());
         }
         finally {
            is.close();
         }
      }
   }

   private void doReadTotalSummary(InputStream is) throws IOException {
      BufferedReader rdr = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
      // Find start of total summary
      String line = findStartTag(rdr, "<h3>Total</h3>");
      if (line == null) {
         debug("Total summary not found in execution report");
         return;
      }
      // Skip "<p>"
      rdr.readLine();
      this.totalSummaryHtml = readUpTo(rdr, "</table>", true);
      // Find start of summary table
      line = findStartTag(rdr, "<table");
      if (line == null) {
         debug("Total summary table not found in execution report");
         return;
      }
      this.totalSummaryTableHtml = line + readUpTo(rdr, "</table>", true);
   }

   private String findStartTag(BufferedReader rdr, String tag) throws IOException {
      String line = rdr.readLine();
      while (line != null) {
         if (line.contains(tag)) {
            return line;
         }
         line = rdr.readLine();
      }
      return null;
   }

   private String readUpTo(BufferedReader rdr, String endTag, boolean includeEndLine) throws IOException {
      StringBuilder sb = new StringBuilder();
      String line = rdr.readLine();
      while (line != null) {
         if (line.contains(endTag)) {
            // We have found the end; done
            if (includeEndLine) {
               sb.append(line).append('\n');
            }
            return sb.toString();
         }
         sb.append(line).append('\n');
         line = rdr.readLine();
      }
      return null;
   }


}
