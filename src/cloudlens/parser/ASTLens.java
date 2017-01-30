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

public class ASTLens extends ASTElement {
  public String name;
  public List<String> args;
  public List<ASTElement> astElements;

  public ASTLens(String file, int line, String name, List<String> args,
      List<ASTElement> elements) {
    super(file, line, ASTType.Lens);
    this.name = name;
    this.astElements = elements;
    this.args = args;
  }

}
