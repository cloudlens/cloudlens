package cloudlens.engine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;

import cloudlens.block.BlockEngine;
import cloudlens.block.BlockObject;

public class PipelineStageAfter extends PipelineStage {
  private CLIterator localIt;
  public CLElement after;

  public PipelineStageAfter(CLElement e) {
    super(new ArrayList<>());
    this.after = e;
    this.effect = true;
  }

  @Override
  public CLIterator apply(BlockEngine engine, CLIterator clIt) {
    final CLIterator res = new CLIterator(engine, new Iterator<BlockObject>() {
      @Override
      public boolean hasNext() {
        if (clIt.hasNext()) {
          return true;
        }
        if (localIt == null) {
          final BlockObject current = after.closure.call();
          executed = true;
          if (engine.isArray(current)) {
            localIt = new CLIterator(engine, current, false);
          } else {
            localIt = new CLIterator(engine,
                Collections.singletonList(current).iterator(),
                clIt.withHistory);
          }
        }
        return localIt.hasNext();
      }

      @Override
      public BlockObject next() {
        if (clIt.hasNext()) {
          return clIt.next();
        } else {
          return localIt.next();
        }
      }
    }, false);
    return res;
  }
}
