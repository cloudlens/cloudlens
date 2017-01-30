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

import java.io.ByteArrayOutputStream;
import java.util.List;

public class RuntimePipeline implements RuntimeElement {

  public final List<PipelineStage> pipeline;
  private boolean effect = false;

  public RuntimePipeline(List<PipelineStage> pipeline) {
    this.pipeline = pipeline;
  }

  @Override
  public CLIterator run(CL cl, CLIterator clIt, boolean withHistory) {
    for (final PipelineStage child : pipeline) {
      clIt = child.apply(cl.engine, clIt);
    }
    clIt.withHistory = withHistory;

    clIt.iterate();

    for (final PipelineStage child : pipeline) {
      cl.executed |= child.executed;
      effect |= child.effect;
    }

    cl.outWriter.flush();

    if (effect && cl.out instanceof ByteArrayOutputStream
        && ((ByteArrayOutputStream) cl.out).size() == 0) {
      cl.errWriter.println("Pipeline has no output.");
    }

    return clIt;
  }
}