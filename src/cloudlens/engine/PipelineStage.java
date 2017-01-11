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

import java.util.Iterator;
import java.util.List;

import cloudlens.block.BlockEngine;
import cloudlens.block.BlockObject;

public abstract class PipelineStage extends Pipeline {
  public final List<PipelineStep> processors;

  public PipelineStage(String stream, List<PipelineStep> processors) {
    super(stream);
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

  @Override
  public CLIterator apply(BlockEngine engine, CLIterator clIt) {
    final CLIterator res = new CLIterator(engine, new Iterator<BlockObject>() {
      @Override
      public boolean hasNext() {
        return clIt.hasNext();
      }

      @Override
      public BlockObject next() {
        final BlockObject current = apply(clIt.next());
        return current;
      }
    }, false);
    return res;
  }
}
