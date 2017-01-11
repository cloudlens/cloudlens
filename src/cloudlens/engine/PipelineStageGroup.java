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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cloudlens.block.BlockEngine;
import cloudlens.block.BlockObject;
import cloudlens.parser.ASTGroup;

public class PipelineStageGroup extends PipelineStage {
  public final String output;

  public static List<PipelineStep> getPatterns(CLElement e) {
    final List<PipelineStep> patterns = new ArrayList<>();
    final BlockObject jsStrings = e.closure.call();
    for (int i = 0; i < jsStrings.size(); i++) {
      final String regex = jsStrings.get(i).asString();
      final ASTGroup g = e.group();
      final PipelineStepPattern step = new PipelineStepPattern(g.file,
          g.line + i, regex, g.var, g.upon);
      patterns.add(step);
    }
    return patterns;
  }

  public PipelineStageGroup(CLElement e) {
    super(e.group().stream, getPatterns(e));
    this.output = e.group().output;
  }

  @Override
  public CLIterator apply(BlockEngine engine, CLIterator clIt) {
    final CLIterator res = new CLIterator(engine, new Iterator<BlockObject>() {
      private boolean hasNext;
      private boolean beforeFirstAdvance = true;
      private BlockObject firstOuputEntry;
      private BlockObject nextOuputEntryAcc;

      private BlockObject aggregate(BlockEngine engine, BlockObject entry) {
        if (entry == null) { // end of stream
          final BlockObject current = nextOuputEntryAcc;
          nextOuputEntryAcc = null;
          return current;
        }
        for (final PipelineStep processor : processors) {
          entry = processor.step(entry);
          if (processor.executed) {
            // new entry
            final BlockObject current = nextOuputEntryAcc;
            nextOuputEntryAcc = engine.newObject();
            engine.merge(nextOuputEntryAcc, entry, output);
            return current;
          }
        }
        // not a new entry
        if (nextOuputEntryAcc != null) {
          // not before first entry that satisfies the predicate
          if (nextOuputEntryAcc.containsKey(output)) {
            engine.merge(nextOuputEntryAcc, entry, output);
          }
        }
        return null;
      }

      private BlockObject advance() {
        beforeFirstAdvance = false;
        BlockObject nextOuputEntry = null;
        while (nextOuputEntry == null && clIt.hasNext()) {
          nextOuputEntry = aggregate(engine, clIt.next());
        }
        if (nextOuputEntry == null) {
          nextOuputEntry = aggregate(engine, null);
          hasNext = false;
        }
        return nextOuputEntry;
      }

      @Override
      public boolean hasNext() {
        if (beforeFirstAdvance) {
          // We have to do advance to know if the iterator has at least one
          // element.
          firstOuputEntry = advance();
          hasNext = firstOuputEntry != null;
        }
        return hasNext;
      }

      @Override
      public BlockObject next() {
        final BlockObject current;
        if (firstOuputEntry != null) {
          current = firstOuputEntry;
          firstOuputEntry = null;
          hasNext = clIt.hasNext();
        } else {
          current = advance();
        }
        return current;
      }
    }, false);
    return res;
  }
}
