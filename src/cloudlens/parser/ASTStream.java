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

package cloudlens.parser;

import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cloudlens.block.BlockException;
import cloudlens.block.JSEngine;
import cloudlens.engine.CLException;
import cloudlens.engine.PipelineStep;

public class ASTStream extends ASTStreamingSection {
  public Collection<Collection<String>> clauses;
  public PipelineStep step;
  public String script;

  public ASTStream(String file, int line, ASTArgs args,
      Collection<Collection<String>> conditions, String script) {
    super(file, line, ASTType.Stream, args);

    Pattern varpat;
    Matcher varmatch;
    String regex;
    if (!conditions.isEmpty()) {
      // Check if the conditions are in the form entry.foo
      regex = "(?<var>\\b" + this.var + "(?:(?:\\.\\w+)+|\\b))";
      varpat = Pattern.compile(regex);
      for (final Collection<String> clause : conditions) {
        for (final String sampler : clause) {
          varmatch = varpat.matcher(sampler);
          if (!varmatch.find()) {
            throw new CLException(
                "Empty <when> filter. Maybe you forgot the entry name.");
          }
        }
      }
    } else {
      // If there is no condition, find dependency in the script.
      regex = "(?<var>\\b" + this.var + "(?:\\.\\w+)+)|\"[^\"]*\"";
      varpat = Pattern.compile(regex);
      varmatch = varpat.matcher(script);
      while (varmatch.find()) {
        final String var = varmatch.group("var");
        if (var != null) {
          conditions.add(Collections.singletonList(var));
        }
      }
    }

    if (conditions.isEmpty()) {
      conditions.add(Collections.singletonList(this.var));
    }

    this.clauses = conditions;

    final String streamScript = " function (" + var + "){" + script
        + "; return " + var + "}";

    try {
      JSEngine.checkSyntax(file, line, streamScript);
    } catch (final BlockException e) {
      throw new CLException(e.getMessage());
    }

    this.script = script;
  }
}
