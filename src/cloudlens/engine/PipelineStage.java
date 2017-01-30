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

import java.util.Iterator;
import java.util.List;

import cloudlens.block.BlockEngine;
import cloudlens.block.BlockObject;

public abstract class PipelineStage {
  public final List<PipelineStep> processors;
  private CLIterator localIt = new CLIterator();
  public boolean executed = false;
  public boolean effect = false;

  public PipelineStage(List<PipelineStep> processors) {
    this.processors = processors;
  }

  private BlockObject apply(BlockObject entry) {
    BlockObject current = entry;
    for (final PipelineStep processor : processors) {
      if (current.isUndefined()) {
        break;
      } else {
        executed |= processor.executed;
        current = processor.step(current);
      }
    }
    return current;
  }

  public CLIterator apply(BlockEngine engine, CLIterator clIt) {
    final CLIterator res = new CLIterator(engine, new Iterator<BlockObject>() {
      @Override
      public boolean hasNext() {
        return localIt.hasNext() || clIt.hasNext();
      }

      @Override
      public BlockObject next() {
        if (localIt.hasNext()) {
          return localIt.next();
        } else {
          final BlockObject current = apply(clIt.next());
          if (!engine.isArray(current)) {
            return current;
          } else {
            localIt = new CLIterator(engine, current, clIt.withHistory);
            return localIt.next();
          }
        }
      }
    }, false);
    return res;
  }
}
