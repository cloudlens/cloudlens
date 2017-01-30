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

import cloudlens.engine.CLException;

public class ASTSource extends ASTElement {
  public enum Format {
    Text, Json
  }

  public String url;
  public Format format;
  public String path;

  public ASTSource(String file, int line, String url, String format,
      String path) {
    super(file, line, ASTType.Source);
    this.url = url;
    this.path = path;
    if (format == null) {
      this.format = Format.Text;
    } else if (format.equals("json")) {
      this.format = Format.Json;
    } else {
      throw new CLException("Unsupported format: " + format);
    }
  }
}
