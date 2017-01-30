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

package cloudlens.parser;

import java.util.List;

public class ASTParagraph {

  public static enum ParagraphType {
    CL, JS
  }

  public String script;
  public List<ASTElement> elements;
  public ParagraphType type;

  public ASTParagraph(List<ASTElement> elements) {
    this.elements = elements;
    this.type = ParagraphType.CL;
  }

  public ASTParagraph(String script) {
    this.script = script;
    this.type = ParagraphType.JS;
  }

  public List<ASTElement> asCL() {
    return elements;
  }

  public String asJS() {
    return script;
  }
}
