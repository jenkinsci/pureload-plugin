/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.integration;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import com.pureload.jenkins.plugin.parser.JUnitParser;
import com.pureload.jenkins.plugin.parser.ParseException;
import com.pureload.jenkins.plugin.result.JUnitReport;
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
@SuppressWarnings("WeakerAccess")
public class PureLoadPublisher extends Recorder implements SimpleBuildStep {

   private static final String JUNIT_REPORT_FILENAME = "junit-report.xml";
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
      JUnitReport report = findAndParseJUnit(run, listener);
      debug("Parsed JUnit report: {0}", report);
      if (report != null) {
         listener.getLogger().println("Parsed JUnit report. Adding PureLoad Results action.");
         debug("Creating results action...");
         PureLoadResultsAction action = new PureLoadResultsAction(run);
         action.setReport(report);
         debug("Adding results action...");
         run.addAction(action);
         if (!report.isSuccess()) {
            listener.getLogger().println("JUnit report indicated failure");
            run.setResult(Result.FAILURE);
         }
      }
   }

   private JUnitReport findAndParseJUnit(Run<?, ?> run, TaskListener listener) throws IOException {
      VirtualFile file = findJUnitFile(run);
      if (file == null) {
         listener.error("Can not locate JUnit report file");
         run.setResult(Result.FAILURE);
         return null;
      }
      debug("Parsing JUnit report... ");
      try {
         return JUnitParser.parse(file);
      }
      catch (ParseException e) {
         listener.error(e.getMessage());
         run.setResult(Result.FAILURE);
      }
      return null;
   }

   private static VirtualFile findJUnitFile(Run<?, ?> run) throws IOException {
      debug("Locating JUnit report file... ");
      ArtifactManager artifactManager = run.getArtifactManager();
      return findJUnitFile(artifactManager.root().list());
   }

   private static VirtualFile findJUnitFile(VirtualFile[] list) throws IOException {
      for (VirtualFile file : list) {
         if (file.isDirectory()) {
            VirtualFile foundFile = findJUnitFile(file.list());
            if (foundFile != null) {
               return foundFile;
            }
         }
         if (file.getName().equalsIgnoreCase(JUNIT_REPORT_FILENAME)) {
            debug("Found junit file: {0}", file);
            return file;
         }
      }
      return null;
   }


   private static void debug(String msg, Object... args) {
      LOGGER.fine(MessageFormat.format(msg, args));
   }

   @Override
   public BuildStepMonitor getRequiredMonitorService() {
      return BuildStepMonitor.NONE;
   }

   @SuppressWarnings("WeakerAccess")
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
