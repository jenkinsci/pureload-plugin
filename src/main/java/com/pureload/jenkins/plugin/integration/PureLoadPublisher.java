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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import com.pureload.jenkins.plugin.parser.JUnitParser;
import com.pureload.jenkins.plugin.parser.ParseException;
import com.pureload.jenkins.plugin.result.JUnitReport;
import com.pureload.jenkins.plugin.result.PureLoadResult;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractProject;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.tasks.Recorder;
import jenkins.model.ArtifactManager;
import jenkins.tasks.SimpleBuildStep;
import jenkins.util.VirtualFile;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * PureLoad publisher.
 * This build step tries to find JUnit result file in artifacts, parse the JUnt results file
 * and publish results (execute PureLoadResultsAction).
 */
@SuppressWarnings("unused") // used by framework
public class PureLoadPublisher extends Recorder implements SimpleBuildStep {

   private static final String JUNIT_REPORT_DIR = "junit";
   private static final String JUNIT_REPORT_FILENAME = "junit-report.xml";
   private static final String EXECUTION_REPORT_DIR = "report";
   private static final String EXECUTION_REPORT_FILENAME = "report.html";
   private static final Logger LOGGER = Logger.getLogger(PureLoadPublisher.class.getName());

   /**
    * The annotation @DataBoundConstructor are required for jenkins 1.393 even
    * if no params are passed in.
    */
   @DataBoundConstructor
   public PureLoadPublisher() {
      debug("Creating PureLoadPublisher");
   }

   @Override
   public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath filePath, @Nonnull Launcher launcher,
                       @Nonnull TaskListener listener)
       throws IOException
   {
      List<PureLoadResult> pureloadResults = findAndParseResults(run, listener);

      if (!pureloadResults.isEmpty()) {
         listener.getLogger().println("Parsed JUnit report. Adding PureLoad Results action.");
         debug("Creating results action...");
         PureLoadResultsAction action = new PureLoadResultsAction(run);
         action.setPureLoadResults(pureloadResults);

         debug("Adding results action...");
         run.addAction(action);
         for (PureLoadResult pureloadResult : pureloadResults) {
            if (!pureloadResult.getJunitReport().isSuccess()) {
               listener.getLogger().println("JUnit report indicated failure");
               run.setResult(Result.FAILURE);
            }
         }
      }
   }

   private List<PureLoadResult> findAndParseResults(Run<?, ?> run, TaskListener listener)
       throws IOException
   {
      List<PureLoadResult> pureloadResults = new ArrayList<>();
      ArtifactManager artifactManager = run.getArtifactManager();

      doFindAndParseResults(pureloadResults, artifactManager.root(), run, listener);

      if (pureloadResults.isEmpty()) {
         listener.error("Can not locate JUnit report file");
         run.setResult(Result.FAILURE);
      }

      // Sort results based on execution date.
      Collections.sort(pureloadResults, PureLoadResult.BY_NAME_DATE);

      return pureloadResults;
   }

   private void doFindAndParseResults(List<PureLoadResult> pureloadResults, VirtualFile file,
                                      Run<?, ?> run, TaskListener listener)
       throws IOException
   {
      try {
         if (isResultDir(file)) {
            pureloadResults.add(parseResult(file, run, listener));
         }
         else if (file.isDirectory()) {
            for (VirtualFile f : file.list()) {
               // Recurse into children.
               doFindAndParseResults(pureloadResults, f, run, listener);
            }
         }
         // else do nothing
      }
      catch (ParseException e) {
         listener.error(e.getMessage());
         run.setResult(Result.FAILURE);
      }
   }

   private PureLoadResult parseResult(VirtualFile resultDir, Run<?, ?> run, TaskListener listener)
       throws IOException, ParseException
   {
      VirtualFile junitFile = resultDir.child(JUNIT_REPORT_DIR).child(JUNIT_REPORT_FILENAME);

      debug("Parsing JUnit report... ");
      JUnitReport junitReport = JUnitParser.parse(junitFile);
      debug("Parsed JUnit report: {0}", junitReport);

      PureLoadResult pureloadResult = new PureLoadResult(junitReport);
      addTotalSummary(pureloadResult, resultDir.child(EXECUTION_REPORT_DIR).child(EXECUTION_REPORT_FILENAME));

      return pureloadResult;
   }

   private static boolean isResultDir(VirtualFile dir) throws IOException {
      if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
         return false;
      }

      VirtualFile junitDir = dir.child(JUNIT_REPORT_DIR);
      if (!junitDir.exists() || !junitDir.isDirectory() || !junitDir.canRead()) {
         return false;
      }

      VirtualFile junitReportFile = junitDir.child(JUNIT_REPORT_FILENAME);
      return junitReportFile.exists() && junitReportFile.canRead();
   }

   private static void debug(String msg, Object... args) {
      LOGGER.fine(MessageFormat.format(msg, args));
   }

   @Override
   public BuildStepMonitor getRequiredMonitorService() {
      return BuildStepMonitor.NONE;
   }

   private void addTotalSummary(PureLoadResult pureloadResult, VirtualFile reportFile) throws IOException {
      if (reportFile.isFile() && reportFile.canRead()) {
         try (InputStream is = reportFile.open()) {
            doAddTotalSummary(pureloadResult, is);
         }
      }
   }

   private void doAddTotalSummary(PureLoadResult pureloadResult, InputStream is) throws IOException {
      BufferedReader rdr = new BufferedReader(new InputStreamReader(is, Charset.defaultCharset()));
      // Find start of total summary
      String line = findStartTag(rdr, "<h3>Total</h3>");
      if (line == null) {
         debug("Total summary not found in execution report");
         return;
      }
      // Skip "<p>"
      rdr.readLine();
      pureloadResult.setTotalSummaryHtml(readUpTo(rdr, "</table>", true));
      // Find start of summary table
      line = findStartTag(rdr, "<table");
      if (line == null) {
         debug("Total summary table not found in execution report");
         return;
      }
      pureloadResult.setTotalSummaryTableHtml(line + readUpTo(rdr, "</table>", true));
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

   @SuppressWarnings("SameParameterValue")
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

   @SuppressWarnings("unused") // used by framework
   @Symbol("publishPureLoad")
   @Extension
   public static class DescriptorImpl extends BuildStepDescriptor<Publisher> {

      @Override
      public boolean isApplicable(Class<? extends AbstractProject> aClass) {
         // Indicates that this builder can be used with all kinds of project types
         return true;
      }

      @Nonnull
      @Override
      public String getDisplayName() {
         return "Publish PureLoad Results";
      }
   }
}
