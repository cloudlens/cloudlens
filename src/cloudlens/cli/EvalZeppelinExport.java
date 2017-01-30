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

package cloudlens.cli;

import java.io.IOException;
import java.util.List;

import cloudlens.engine.CL;
import cloudlens.engine.CLException;
import cloudlens.parser.ASTBuilder;
import cloudlens.parser.ASTException;
import cloudlens.parser.ASTParagraph;

public class EvalZeppelinExport {

  public static void main(String[] args) {
    try {
      final List<ASTParagraph> paragraphs = ASTBuilder
          .parseZeppelinExport(args[0]);
      final CL cl = new CL(System.out, System.err, false, true);
      try {
        for (final ASTParagraph paragraph : paragraphs) {
          switch (paragraph.type) {
          case CL:
            cl.launch(paragraph.asCL());
            break;
          case JS:
            cl.engine.eval(paragraph.asJS());
            break;
          }
        }
      } catch (final CLException exn) {
        System.err.println("Unable to execute paragraph: " + exn);

      }
    } catch (final IOException exn) {
      System.err.println("Unable to parse " + args[0] + ": " + exn);
    } catch (final ASTException e) {
      System.err.println(e.getMessage());
    }

  }

}
