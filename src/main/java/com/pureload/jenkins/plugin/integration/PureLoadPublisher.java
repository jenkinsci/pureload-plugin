/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.integration;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.logging.Logger;
import javax.annotation.Nonnull;

import com.pureload.jenkins.plugin.parser.JUnitParser;
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
import jenkins.tasks.SimpleBuildStep;
import org.jenkinsci.Symbol;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * PureLoad publisher.
 * This build step tries to find JUnit result file in artifacts, parse the JUnt results file
 * and publish results (execute PureLoadResultsAction).
 */
public class PureLoadPublisher extends Recorder implements SimpleBuildStep {

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
      JUnitReport report = JUnitParser.findAndParseJUnit(run, listener);
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

   private void debug(String msg, Object... args) {
      LOGGER.fine(MessageFormat.format(msg, args));
   }

   @Override
   public BuildStepMonitor getRequiredMonitorService() {
      return BuildStepMonitor.NONE;
   }

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
