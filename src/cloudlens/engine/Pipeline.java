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

public class Pipeline {
  public List<Pipeline> children = null;
  public boolean executed = false;
  public boolean effect = false;
  public final String stream;

  public Pipeline(String stream) {
    this.stream = stream;
    this.children = new ArrayList<>();
  }

  public boolean isEmpty() {
    return children.isEmpty();
  }

  private boolean isLeaf() {
    return (this instanceof PipelineStage);
  }

  private void add(String[] streams, int pos, PipelineStage stage) {
    if (streams[pos].equals(stream)) {
      if (pos == streams.length - 1) {
        children.add(stage);
      } else {
        Pipeline child = lastChild(children, streams[pos + 1]);
        if (child != null) {
          child.add(streams, pos + 1, stage);
        } else {
          child = new Pipeline(streams[pos + 1]);
          children.add(child);
          child.add(streams, pos + 1, stage);
        }
      }
    } else {
      throw new CLException("Invalid stream" + stream);
    }
  }

  public void add(PipelineStage stage) {
    final String[] streamPath = stage.stream.split("\\.");
    add(streamPath, 0, stage);
  }

  private void addNode(String[] streams, int pos) {
    if (pos < streams.length) {
      if (streams[pos].equals(stream) && !children.isEmpty()) {
        final Pipeline child = children.get(children.size() - 1);
        child.addNode(streams, pos + 1);
      } else {
        final Pipeline child = new Pipeline(streams[pos]);
        children.add(child);
        child.addNode(streams, pos + 1);
      }
    }
  }

  public void addNode(String stream) {
    final String[] streamPath = stream.split("\\.");
    addNode(streamPath, 1);
  }

  private Pipeline lastChild(List<Pipeline> children, String stream) {
    if (!children.isEmpty()) {
      final Pipeline node = children.get(children.size() - 1);
      if (node.stream.equals(stream) && !node.isLeaf()) {
        return node;
      }
    }
    return null;
  }

  public BlockObject apply(BlockEngine engine, BlockObject entry) {
    if (!engine.isArray(entry.get(stream))) {
      throw new CLException(stream + " is not an array.");
    }

    CLIterator logStream = new CLIterator(engine, entry.get(stream), false);

    for (final Pipeline child : children) {
      logStream = child.apply(engine, logStream);
    }
    logStream.withHistory = true;

    logStream.iterate();
    entry.put(stream, engine.newArray(logStream.history));

    for (final Pipeline child : children) {
      executed |= child.executed;
      effect |= child.effect;
    }
    return entry;
  }

  public CLIterator apply(BlockEngine engine, CLIterator clIt) {
    final CLIterator res = new CLIterator(engine, new Iterator<BlockObject>() {
      @Override
      public boolean hasNext() {
        return clIt.hasNext();
      }

      @Override
      public BlockObject next() {
        final BlockObject current = apply(engine, clIt.next());
        return current;
      }
    }, false);
    return res;
  }
}
