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

package cloudlens.engine;

import java.util.ArrayList;
import java.util.List;

import cloudlens.block.BlockObject;
import cloudlens.parser.ASTMatch;

public class PipelineStageMatch extends PipelineStage {

  public static List<PipelineStep> getMatchers(CLElement e) {
    final List<PipelineStep> matchers = new ArrayList<>();
    final BlockObject jsStrings = e.closure.call();
    for (int i = 0; i < jsStrings.size(); i++) {
      final String regex = jsStrings.get(i).asString();
      final ASTMatch m = e.match();
      final PipelineStepPattern step = new PipelineStepPattern(m.file,
          m.line + i, regex, m.upon);
      matchers.add(step);
    }
    return matchers;
  }

  public PipelineStageMatch(CLElement e) {
    super(getMatchers(e));
  }

}
