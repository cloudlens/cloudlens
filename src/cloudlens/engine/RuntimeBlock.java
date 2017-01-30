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

import java.util.List;

import cloudlens.block.BlockException;
import cloudlens.block.BlockObject;
import cloudlens.parser.ASTBlock;

public class RuntimeBlock implements RuntimeElement {
  public final CLElement block;
  public final ASTBlock ast;

  public RuntimeBlock(CLElement e) {
    this.block = e;
    this.ast = block.block();
  }

  @Override
  public CLIterator run(CL cl, CLIterator clIt, boolean withHistory) {
    try {

      final BlockObject closures = block.closure.call();
      final List<RuntimeElement> runtimes = CLBuilder.build(cl, block,
          closures);

      for (final RuntimeElement e : runtimes) {
        clIt = e.run(cl, clIt, withHistory);
      }

      cl.executed = true;
      return clIt;

    } catch (final BlockException e) {
      throw new CLException("Error: " + ast.file + ", block starting line "
          + ast.line + ":\n" + e.getMessage());
    }
  }
}
