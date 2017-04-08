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

import java.util.Collection;

import cloudlens.block.BlockException;
import cloudlens.block.BlockObject;
import cloudlens.parser.ASTProcess;

// a wrapper around the Script class to keep track of variable dependencies
public class PipelineStepProcess extends PipelineStep {
  public CLElement process;
  public ASTProcess ast;

  public PipelineStepProcess(CLElement e) {
    this.process = e;
    this.ast = e.process();
  }

  @Override
  public BlockObject step(BlockObject entry) {
    // do not execute if no update to the variables occurring in this script
    boolean go = false;
    BlockObject current = entry;
    for (final Collection<String> clause : ast.clauses) {
      boolean goClause = !clause.isEmpty();
      for (final String variable : clause) {
        final String[] varpath = variable.split("\\.");
        // if varpath only contains entry name, always execute
        if (!(varpath.length <= 1)) {
          goClause &= entry.checkpath(varpath, 1);
        }
      }
      if (go |= goClause) {
        break;
      }
    }
    if (!go) {
      executed = false;
      return current;
    }

    try {
      current = process.closure.call(entry);
    } catch (final BlockException e) {
      throw new CLException("Error: " + ast.file + ", block starting line "
          + ast.line + ":\n" + e.getMessage());
    }
    executed = true;
    return current;
  }
}
