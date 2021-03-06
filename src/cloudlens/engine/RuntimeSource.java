/*
 *  This file is part of the CloudLens project.
 *
 * Copyright 2015-2018 IBM Corporation
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

import cloudlens.parser.ASTSource;

public class RuntimeSource implements RuntimeElement {
  public final CLElement source;
  public final ASTSource ast;

  public RuntimeSource(CLElement e) {
    this.source = e;
    this.ast = source.source();
  }

  @Override
  public CLIterator run(CL cl, CLIterator clIt, boolean withHistory) {
    try {
      switch (ast.format) {
      case Json:
        clIt = CLIterator.json(cl.engine, ast.url, ast.path, withHistory);
        break;
      case Text:
        clIt = CLIterator.source(cl.engine, ast.url, withHistory);
        break;
      }
      return clIt;
    } catch (final CLException e) {
      throw new CLException("Error: " + ast.file + ", block starting line "
          + ast.line + ":\n" + e.getMessage());
    }
  }
}
