/*
 *  This file is part of the CloudLens project.
 *
 * Copyright 2015-2016 IBM Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cloudlens.engine;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import cloudlens.block.BlockObject;

// a wrapper around the Pattern class to keep track of property names and types
public class PipelineStepPattern implements PipelineStep {
  private final String file;
  private final int line;
  private final String upon;
  // a pattern to find property names in a regex
  private static Pattern property = Pattern
      .compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>");

  // a pattern to find type declarations
  private static Pattern type = Pattern.compile(
      "([a-zA-Z][a-zA-Z0-9]*) *: *([a-zA-Z][a-zA-Z0-9]*(?:\\[[^\\]]+\\])?)");

  private Pattern pattern = null; // the actual pattern
  private Set<Map.Entry<String, String>> env = null; // property:type
  private Matcher matcher;

  public PipelineStepPattern(String file, int line, String regex, String name,
      String upon) {
    this.file = file;
    this.line = line;
    this.upon = upon;

    try {
      final int pos = regex.indexOf("#");
      pattern = Pattern.compile(pos == -1 ? regex : regex.substring(0, pos));
      final Map<String, String> map = new Hashtable<>();
      final Matcher m = property.matcher(regex);
      // find declared properties
      while (m.find()) {
        // default to Object type
        map.put(m.group(1), "Object");
      }
      if (pos >= 0) {
        // add declared property type when found
        final Matcher t = type.matcher(regex.substring(pos + 1));
        while (t.find()) {
          map.put(t.group(1), t.group(2));
        }
      }
      env = map.entrySet();
      matcher = pattern.matcher("");
    } catch (final PatternSyntaxException e) {
      throw new CLException("Regular Expression Syntax Error: " + file
          + " line " + line + "\n" + e.getMessage());
    }
  }

  // match the input against the pattern and define properties found in pattern
  @Override
  public boolean step(BlockObject properties) {
    final String[] varpath = this.upon.split("\\.");
    final String text = properties.getpath(varpath, 1).asString();

    if (text == null) {
      return false;
    }
    try {
      final Matcher m = matcher.reset(text);
      if (!m.find()) {
        return false;
      }
      for (final Map.Entry<String, String> p : env) {
        final String value = m.group(p.getKey());
        if (value != null) {
          // match includes this property
          Object v = value;
          // interpret value according to declared type if any
          switch (p.getValue()) {
          case "int":
            try {
              v = Integer.valueOf(value);
            } catch (final NumberFormatException e) {
              // ignore type error for now
            }
            break;
          }
          if (p.getValue().startsWith("Date[")) {
            try {
              // using getTime() for now to get a long
              v = new SimpleDateFormat(
                  p.getValue().substring(5, p.getValue().length() - 1))
                      .parse(value).getTime();
            } catch (final ParseException | IllegalArgumentException e) {
              // ignore type error for now
            }
          }
          properties.put(p.getKey(), v);
        }
      }
      return true;
    } catch (final IllegalArgumentException e) {
      throw new CLException("Regular Expression Syntax Error: " + file
          + " line " + line + "\n" + e.getMessage());
    }
  }
}
