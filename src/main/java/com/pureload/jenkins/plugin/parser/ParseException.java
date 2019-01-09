/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.parser;

/** Signal error parsing JUnit file */
public class ParseException extends Exception {
   public ParseException(String message) {
      super(message);
   }
   public ParseException(String message, Throwable cause) {
      super(message, cause);
   }
}
