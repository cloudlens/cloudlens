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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import cloudlens.engine.CL;
import cloudlens.engine.CLException;
import cloudlens.parser.ASTBuilder;
import cloudlens.parser.ASTElement;
import cloudlens.parser.FileReader;

public class Whisk {

  /* Whisk action entry point */
  public static JsonObject main(JsonObject args) {
    final JsonObject res = new JsonObject();

    try {
      final CL cl = new CL(System.out, System.err, false, true);

      String script = "";
      String logURI = "";

      if (args.has("script")) {
        script = args.getAsJsonPrimitive("script").getAsString();
      }
      if (args.has("log")) {
        logURI = args.getAsJsonPrimitive("log").getAsString();
      }

      try {
        final InputStream inputStream = FileReader.fetchFile(logURI);
        cl.source(inputStream);
        final List<ASTElement> top = ASTBuilder.parse(script);
        cl.launch(top);

        final JsonElement jelement = new JsonParser()
            .parse(cl.cl.get("result").asString());
        return jelement.getAsJsonObject();

      } catch (final CLException e) {
        res.addProperty("error", e.getMessage());
        System.err.println(e.getMessage());
      } finally {
        cl.outWriter.flush();
        cl.errWriter.flush();
      }

    } catch (final Exception e) {
      res.addProperty("error", e.getMessage());
      System.err.println(e.getMessage());
    }

    return res;
  }

  public static void main(String[] args) {
    try {
      final FileInputStream inputStream = new FileInputStream(
          new File(args[0]));
      final BufferedReader rd = new BufferedReader(
          new InputStreamReader(inputStream));
      String line;
      final StringBuilder sb = new StringBuilder();
      while ((line = rd.readLine()) != null) {
        sb.append(line);
      }
      rd.close();

      final JsonElement jelement = new JsonParser().parse(sb.toString());

      final JsonObject res = main(jelement.getAsJsonObject());
      System.out.println(res);

    } catch (final FileNotFoundException e) {
      e.printStackTrace();
    } catch (final IOException e) {
      e.printStackTrace();
    }

  }

}
