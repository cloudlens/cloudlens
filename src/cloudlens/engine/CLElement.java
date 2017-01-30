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

import java.util.ArrayList;
import java.util.List;

import cloudlens.block.BlockObject;
import cloudlens.parser.ASTAfter;
import cloudlens.parser.ASTBlock;
import cloudlens.parser.ASTElement;
import cloudlens.parser.ASTLens;
import cloudlens.parser.ASTMatch;
import cloudlens.parser.ASTProcess;
import cloudlens.parser.ASTRun;
import cloudlens.parser.ASTSource;

public class CLElement {
  public ASTElement ast;
  public BlockObject closure;
  public List<CLElement> children;

  public CLElement() {
    this.children = new ArrayList<>();
  }

  public CLElement(ASTElement ast) {
    this.ast = ast;
    this.children = new ArrayList<>();
  }

  public ASTBlock block() {
    try {
      return (ASTBlock) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTBlock.");
    }
  }

  public ASTProcess process() {
    try {
      return (ASTProcess) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTProcess.");
    }
  }

  public ASTAfter after() {
    try {
      return (ASTAfter) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTAfter.");
    }
  }

  public ASTMatch match() {
    try {
      return (ASTMatch) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTMatch.");
    }
  }

  public ASTLens lens() {
    try {
      return (ASTLens) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTLens.");
    }
  }

  public ASTRun run() {
    try {
      return (ASTRun) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTRun.");
    }
  }

  public ASTSource source() {
    try {
      return (ASTSource) ast;
    } catch (final Exception e) {
      throw new CLException("Cannot cast to ASTSource.");
    }
  }

}
