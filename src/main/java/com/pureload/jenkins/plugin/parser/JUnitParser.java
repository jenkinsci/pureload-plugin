/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.pureload.jenkins.plugin.result.JUnitReport;
import com.pureload.jenkins.plugin.result.TestCaseResult;
import hudson.FilePath;
import hudson.model.Result;
import hudson.model.Run;
import hudson.model.TaskListener;
import jenkins.model.ArtifactManager;
import jenkins.util.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Created by janne on 2017-02-15.
 */
public class JUnitParser {

   private static final String JUNIT_REPORT_FILENAME = "junit-report.xml";

   private static final Logger LOGGER = Logger.getLogger(JUnitParser.class.getName());
   private static final DateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

   public static JUnitReport findAndParseJUnit(Run<?, ?> run, TaskListener listener) throws IOException {
      VirtualFile file = findJUnitFile(run);
      if (file == null) {
         listener.error("Can not locate JUnit report file");
         run.setResult(Result.FAILURE);
         return null;
      }
      debug("Parsing JUnit report... ");
      try {
         return JUnitParser.doParse(file);
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

   static public JUnitReport parse(File file) throws ParseException {
      return doParse(new FilePath(file));
   }

   static private JUnitReport doParse(VirtualFile file) throws ParseException {
      try {
         return doParse(file.getName(), file.open());
      }
      catch (IOException e) {
         throw new ParseException("Can not parse: " + file.getName(), e);
      }
   }

   static public JUnitReport doParse(FilePath file) throws ParseException {
      try {
         return doParse(file.getName(), file.read());
      }
      catch (IOException | InterruptedException e) {
         throw new ParseException("Can not parse: " + file.getName(), e);
      }
   }

   static private JUnitReport doParse(String fileName, InputStream is) throws ParseException {
      try {
         final SAXParserFactory factory = SAXParserFactory.newInstance();
         factory.setValidating(false);
         factory.setNamespaceAware(false);
         final SAXParser parser = factory.newSAXParser();
         final ParserHandler handler = new ParserHandler(fileName);
         parser.parse(is, handler);
         return handler.getReport();
      }
      catch (Exception e) {
         throw new ParseException("Can not doParse: " + fileName, e);
      }
      finally {
         try {
            is.close();
         }
         catch (IOException ignore) {}
      }
   }

   private static void debug(String msg, Object... args) {
      LOGGER.info(MessageFormat.format(msg, args));
   }

   private static class ParserHandler extends DefaultHandler {

      private JUnitReport report;
      private Locator locator;
      private String value;

      ParserHandler(String fileName) {
         this.report = new JUnitReport(fileName);
      }

      public JUnitReport getReport() { return report; }

      /** Called when XML-parser starts reading XML document */
      @Override
      public void setDocumentLocator(Locator locator) {
         this.locator = locator;
      }

      /** Called at start of an element */
      @Override
      public void startElement(String uri, String localName, String qName, Attributes attributes)
          throws SAXException
      {
         if ("testsuite".equalsIgnoreCase(qName)) {
            String name = attributes.getValue("name");
            report.setPlcFileName(name);
            String timeStr = attributes.getValue("timestamp");
            try {
               Date date = ISO_8601_DATE_FORMAT.parse(timeStr);
               report.setDate(date);
            }
            catch (java.text.ParseException e) {
               throw new SAXParseException("Can not doParse timestamp attribute", locator);
            }
            timeStr = attributes.getValue("time");
            try {
               report.setExecTime(parseTime(timeStr));
            }
            catch (java.text.ParseException e) {
               throw new SAXParseException("Can not doParse time attribute", locator);
            }
         }
         else if ("testcase".equalsIgnoreCase(qName)) {
            String classname = attributes.getValue("classname");
            TestCaseResult.Type type = classname.startsWith("KPI.") ?
                                       TestCaseResult.Type.KPI : TestCaseResult.Type.Scenario;
            String name = attributes.getValue("name");
            String time = attributes.getValue("time");
            String status = attributes.getValue("status");
            TestCaseResult result = report.getCurrent();
            if (result != null) {
               // We have a new testcase; add current
               report.addCurrent();
            }
            else {
               // Create new result
               result = new TestCaseResult(name, type);
               if (type == TestCaseResult.Type.Scenario) {
                  // Scenario result; doParse time
                  try {
                     result.setExecTime(parseTime(time) / 1000.0f);
                  }
                  catch (java.text.ParseException e) {
                     throw new SAXParseException("Can not doParse time attribute", locator);
                  }
               }
               result.setOk(status.equalsIgnoreCase("Ok"));
               report.setCurrent(result);
            }
         }
         else if ("failure".equalsIgnoreCase(qName)) {
            String message = attributes.getValue("message");
            currentScenarioFailed(message);
         }
         else if ("error".equalsIgnoreCase(qName)) {
            currentScenarioFailed("");
         }
      }

      /** Called at end of an element */
      @Override
      public void endElement(String uri, String localName, String qName) {
         if (("testsuite".equalsIgnoreCase(qName) || "testcase".equalsIgnoreCase(qName))) {
            TestCaseResult result = report.getCurrent();
            if (result != null) {
               // Done; add current
               report.addCurrent();
            }
         }
      }

      /** Called with the text contents in between the start and end tags of an element */
      @Override
      public void characters(char[] ch, int start, int length) {
         value = new String(ch, start, length);
      }

      private void currentScenarioFailed(String msg) {
         TestCaseResult result = report.getCurrent();
         if (result != null) {
            result.setOk(false);
            result.setKpiErrorMessage(msg);
         }
      }

      private static long parseTime(final String time) throws java.text.ParseException {
         if (StringUtils.isEmpty(time)) {
            return 0;
         }
         else {
            NumberFormat numberFormat = NumberFormat.getNumberInstance();
            Number number = numberFormat.parse(time);
            return (long) number.floatValue();
         }
      }
   }
}
