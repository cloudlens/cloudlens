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

package cloudlens.block;

import javax.script.CompiledScript;
import javax.script.ScriptException;

public class JSCompiledBlock implements CompiledBlock {
  private final CompiledScript compiledScript;

  protected JSCompiledBlock(CompiledScript compiledScript) {
    this.compiledScript = compiledScript;
  }

  @Override
  public Object eval() {
    try {
      return compiledScript.eval();
    } catch (final ScriptException e) {
      throw new BlockException(e);
    }
  }

}
