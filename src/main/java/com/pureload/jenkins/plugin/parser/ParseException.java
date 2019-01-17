/*
 * Copyright (c) 2019 PureLoad Software Group AB. All rights reserved.
 */

package com.pureload.jenkins.plugin.parser;

/** Signal error parsing JUnit file */
class ParseException extends Exception {
   public ParseException(String message, Throwable cause) {
      super(message, cause);
   }
}
