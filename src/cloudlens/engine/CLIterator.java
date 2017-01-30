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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;

import org.apache.commons.lang3.StringEscapeUtils;

import cloudlens.block.BlockEngine;
import cloudlens.block.BlockException;
import cloudlens.block.BlockObject;
import cloudlens.parser.FileReader;

public class CLIterator implements Iterator<BlockObject> {
  public boolean withHistory;
  public ArrayList<BlockObject> history;
  private Iterator<BlockObject> it;
  private ArrayList<BlockObject> mem;

  public CLIterator() {
    this.it = Collections.emptyIterator();
    this.mem = new ArrayList<>();
    this.history = new ArrayList<>();
    this.withHistory = false;
  }

  public CLIterator(BlockEngine engine, Iterator<BlockObject> it,
      boolean withHistory) {
    this.it = it;
    this.mem = new ArrayList<>();
    this.history = new ArrayList<>();
    this.withHistory = withHistory;
  }

  public CLIterator(BlockEngine engine, BlockObject array,
      boolean withHistory) {
    this.it = array.asList();
    this.history = new ArrayList<>();
    this.mem = new ArrayList<>();
    this.withHistory = withHistory;
  }

  public CLIterator(BlockEngine engine, ArrayList<BlockObject> array,
      boolean withHistory) {
    this.it = array.iterator();
    this.history = new ArrayList<>(array);
    this.mem = new ArrayList<>();
    this.withHistory = withHistory;
  }

  @Override
  public boolean hasNext() {
    return it.hasNext();
  }

  @Override
  public BlockObject next() {
    final BlockObject v = it.next();
    if (withHistory) {
      mem.add(v);
    }
    return v;
  }

  private void restart() {
    it = new ArrayList<>(mem).iterator();
    history = mem;
    mem = new ArrayList<>();
  }

  public void iterate() {
    while (this.hasNext()) {
      this.next();
    }
    restart();
  }

  public static CLIterator source(BlockEngine engine, InputStream inputStream,
      boolean withHistory) {
    final BlockObject wrap = engine
        .eval("function(message) { return {message:message}; }");
    final CLIterator res = new CLIterator(engine, new Iterator<BlockObject>() {
      final Scanner scan = new Scanner(inputStream);

      @Override
      protected void finalize() throws IOException {
        if (scan != null) {
          scan.close();
        }
      }

      @Override
      public boolean hasNext() {
        return scan.hasNext();
      }

      @Override
      public BlockObject next() {
        final BlockObject record = wrap.call(scan.nextLine());
        return record;
      }
    }, withHistory);
    if (withHistory) {
      res.iterate();
    }
    ;
    return res;
  }

  public static CLIterator source(BlockEngine engine, String urlString,
      boolean withHistory) {
    final InputStream inputStream = FileReader.fetchFile(urlString);
    return source(engine, inputStream, withHistory);
  }

  public static CLIterator json(BlockEngine engine, InputStream inputStream,
      String path, boolean withHistory) {
    try {
      final InputStreamReader isr = new InputStreamReader(inputStream);
      final BufferedReader rd = new BufferedReader(isr);
      String line;
      final StringBuilder sb = new StringBuilder();
      while ((line = rd.readLine()) != null) {
        sb.append(line);
      }
      final String s = StringEscapeUtils.escapeEcmaScript(sb.toString());
      try {
        final String jsonPath = (path == null) ? "" : "." + path;
        final BlockObject jsStream = engine
            .eval("JSON.parse(\"" + s + "\")" + jsonPath);
        if (!engine.isArray(jsStream)) {
          throw new BlockException(
              "Parse Error: the log stream " + path + " must be a json Array");
        }
        final CLIterator res = new CLIterator(engine, jsStream.asList(),
            withHistory);
        if (withHistory) {
          res.iterate();
        }
        return res;
      } catch (final BlockException e) {
        throw new CLException(e.getMessage());
      }
    } catch (final IOException e) {
      throw new CLException(e.getMessage());
    }
  }

  public static CLIterator json(BlockEngine engine, String urlString,
      String path, boolean withHistory) {
    final InputStream inputStream = FileReader.fetchFile(urlString);
    return json(engine, inputStream, path, withHistory);
  }

}
