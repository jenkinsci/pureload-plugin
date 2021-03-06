/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.parser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import com.pureload.jenkins.plugin.result.JUnitReport;
import com.pureload.jenkins.plugin.result.TestCaseResult;
import hudson.FilePath;
import jenkins.util.VirtualFile;
import org.apache.commons.lang.StringUtils;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Static utilities to parse JUnit XML file.
 */
public class JUnitParser {

   private static final DateFormat ISO_8601_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");

   static public JUnitReport parse(VirtualFile file) throws ParseException {
      return doParse(file);
   }

   static public JUnitReport parse(File file) throws ParseException {
      return doParse(new FilePath(file));
   }

   static private JUnitReport doParse(VirtualFile file) throws ParseException {
      try {
         return doParse(file.getName(), file.open());
      }
      catch (IOException e) {
         throw new ParseException("Can not parse: '" + file.getName() + "': " + e, e);
      }
   }

   private static JUnitReport doParse(FilePath file) throws ParseException {
      try {
         return doParse(file.getName(), file.read());
      }
      catch (IOException | InterruptedException e) {
         throw new ParseException("Can not parse: '" + file.getName() + "': " + e, e);
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
         throw new ParseException("Can not parse: '" + fileName + "': " + e, e);
      }
      finally {
         try {
            is.close();
         }
         catch (IOException ignore) {}
      }
   }

   private static class ParserHandler extends DefaultHandler {
      // Regex pattern to find KPI details from system-out element.
      static final Pattern KPI_STDOUT_PATTERN = Pattern.compile("(?s)threshold=([^}]+).*timestamp=(\\d+).*value=([^,]+).*status=([^,]+)");

      private final JUnitReport report;
      private Locator locator;
      private StringBuilder eltCharacters;

      ParserHandler(String fileName) {
         this.report = new JUnitReport(fileName);
         eltCharacters = new StringBuilder();
      }

      JUnitReport getReport() { return report; }

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
         eltCharacters.setLength(0);

         if ("testsuite".equalsIgnoreCase(qName)) {
            String name = attributes.getValue("name");
            report.setPlcFileName(name);
            String timeStr = attributes.getValue("timestamp");
            try {
               Date date = ISO_8601_DATE_FORMAT.parse(timeStr);
               report.setDate(date);
            }
            catch (java.text.ParseException e) {
               throw new SAXParseException("Can not parse timestamp attribute", locator);
            }
            timeStr = attributes.getValue("time");
            try {
               report.setExecTime(parseTime(timeStr));
            }
            catch (java.text.ParseException e) {
               throw new SAXParseException("Can not parse time attribute", locator);
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
                  // Scenario result; parse time
                  try {
                     result.setExecTime(parseTime(time) / 1000.0f);
                  }
                  catch (java.text.ParseException e) {
                     throw new SAXParseException("Can not parse time attribute", locator);
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
         else if ("system-out".equalsIgnoreCase(qName)) {
            TestCaseResult result = report.getCurrent();
            if ((result != null) && (result.getType() == TestCaseResult.Type.KPI) && (eltCharacters.length() > 0))
            {
               // For a KPI result that is OK, we parse system-out to show additional details as a message.
               parseKpiSystemOut(eltCharacters, result);
            }
         }

         eltCharacters.setLength(0);
      }

      @Override
      public void characters(char[] ch, int start, int length) {
         // Store element characters for possible parsing.
         eltCharacters.append(new String(ch, start, length));
      }

      private void currentScenarioFailed(String msg) {
         TestCaseResult result = report.getCurrent();
         if (result != null) {
            result.setOk(false);
            result.setKpiMessage(msg);
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

      /** Parse system-out element of KPI to extract and update result with KPI specific information. */
      private static void parseKpiSystemOut(CharSequence systemOut, TestCaseResult result) {
         Matcher m = KPI_STDOUT_PATTERN.matcher(systemOut);
         if (!m.find()) {
            return; // Ignore unknown systemOut data.
         }

         result.setKpiValue(m.group(3));
         result.setKpiThreshold(m.group(1));
         result.setKpiStatus(m.group(4));
         result.setKpiTimestamp(Long.parseLong(m.group(2)));
      }
   }
}
