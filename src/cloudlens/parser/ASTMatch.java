/*
 *  This file is part of the CloudLens project.
 *
 * Copyright omitted for blind review
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cloudlens.engine.CLException;
import cloudlens.engine.PipelineStep;

public class ASTMatch extends ASTElement {
  public String upon;
  public List<String> rules;
  public List<PipelineStep> matchers;

  public ASTMatch(String file, int line, List<String> rules, String upon) {
    super(file, line, ASTType.Match);
    this.matchers = new ArrayList<>();
    this.rules = rules;

    // Check that the upon option start with the entry name
    final Pattern varuponpat = Pattern.compile("entry\\.\\w+");
    final Matcher varuponmatch = varuponpat.matcher(upon);
    if (!varuponmatch.find()) {
      throw new CLException(
          "Invalid path: " + upon + ". Maybe you forgot the entry name.");
    }
    this.upon = upon;
  }
}
