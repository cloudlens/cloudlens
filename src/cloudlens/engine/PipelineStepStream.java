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

import java.util.Collection;

import cloudlens.block.BlockException;
import cloudlens.block.BlockObject;
import cloudlens.parser.ASTStream;

// a wrapper around the Script class to keep track of variable dependencies
public class PipelineStepStream implements PipelineStep {
  public CLElement stream;
  public ASTStream ast;

  public PipelineStepStream(CLElement e) {
    this.stream = e;
    this.ast = e.stream();
  }

  @Override
  public boolean step(BlockObject entry) {
    // do not execute if no update to the variables occurring in this script
    boolean go = false;
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
      return false;
    }

    try {
      stream.closure.call(entry);
    } catch (final BlockException e) {
      throw new CLException("Error: " + ast.file + ", block starting line "
          + ast.line + ":\n" + e.getMessage());
    }
    return true;
  }
}
