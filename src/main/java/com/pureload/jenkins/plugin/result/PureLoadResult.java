/* Copyright (c) 2019 PureLoad Software Group AB. All Rights Reserved. */

package com.pureload.jenkins.plugin.result;

import java.util.Comparator;
import java.util.logging.Logger;

import com.pureload.jenkins.plugin.integration.PureLoadResultsAction;

/**
 * PureLoad result data holder.
 */
public class PureLoadResult {
   /** Compare by JUnit report PLC file name, execution date. */
   public static final Comparator<PureLoadResult> BY_NAME_DATE = new Comparator<PureLoadResult>() {
      public int compare(PureLoadResult o1, PureLoadResult o2) {
         int c = 0;
         if ((o1.junitReport.getPlcFileName() != null) && (o2.junitReport.getPlcFileName() != null)) {
            c = o1.junitReport.getPlcFileName().compareTo(o2.junitReport.getPlcFileName()); // First by name.
         }
         if ((c == 0) && (o1.junitReport.getDate() != null) && (o2.junitReport.getDate() != null)) {
            c = o1.junitReport.getDate().compareTo(o2.junitReport.getDate());               // Second by date.
         }
         return c;
      }
   };

   private static final Logger LOGGER = Logger.getLogger(PureLoadResultsAction.class.getName());

   private JUnitReport junitReport;
   private String totalSummaryHtml;
   private String totalSummaryTableHtml;

   public PureLoadResult(JUnitReport junitReport) {
      this.junitReport = junitReport;
   }

   public JUnitReport getJunitReport() { return junitReport; }

   public String getTotalSummaryHtml() {
      if (totalSummaryHtml == null) {
         LOGGER.severe("Can not access total summary");
         return "";
      }
      return totalSummaryHtml;
   }
   public void setTotalSummaryHtml(String totalSummaryHtml) {
      this.totalSummaryHtml = totalSummaryHtml;
   }

   public String getTotalSummaryTableHtml() {
      if (totalSummaryTableHtml == null) {
         LOGGER.severe("Can not access total summary table");
         return "";
      }
      return totalSummaryTableHtml;
   }
   public void setTotalSummaryTableHtml(String totalSummaryTableHtml) {
      this.totalSummaryTableHtml = totalSummaryTableHtml;
   }
}
