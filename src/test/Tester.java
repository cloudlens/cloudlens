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

package test;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import cloudlens.cli.EvalZeppelinExport;
import cloudlens.cli.Main;
import cloudlens.engine.CLBuilder;

public class Tester {
  public ByteArrayOutputStream outContent = new ByteArrayOutputStream();
  public ByteArrayOutputStream errContent = new ByteArrayOutputStream();

  public void setUpStreams() {
    System.setOut(new PrintStream(outContent));
    System.setErr(new PrintStream(errContent));
  }

  public void cleanUpStreams() {
    System.setOut(null);
    System.setErr(null);
  }

  public void test(String options, String log, String script, String output)
      throws Exception {
    final String cl = options + " -log " + log + " -run " + script;
    CLBuilder.reset();
    Main.main(StringUtils.split(cl));
    final String out = readFile(output);
    assertEquals(normalizeEOL(out), normalizeEOL(outContent.toString()));
  }

  public void testErr(String options, String log, String script, String output)
      throws Exception {
    final String cl = options + " -log " + log + " -run " + script;
    CLBuilder.reset();
    Main.main(StringUtils.split(cl));
    final String out = readFile(output);
    assertEquals(normalizeEOL(out), normalizeEOL(errContent.toString()));
  }

  public void testZeppelin(String script, String output) throws Exception {
    CLBuilder.reset();
    EvalZeppelinExport.main(new String[] { script });
    final String out = readFile(output);
    assertEquals(normalizeEOL(out), normalizeEOL(outContent.toString()));
  }

  private String readFile(String path) throws IOException {
    final byte[] encoded = Files.readAllBytes(Paths.get(path));
    return new String(encoded, Charset.defaultCharset());
  }

  private String normalizeEOL(String str) {
    return str.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");
  }

  static public Collection<Object[]> getTests(String dirName,
      String scriptExt) {
    final Collection<Object[]> testArgs = new ArrayList<Object[]>();
    final File dir = new File(dirName);
    for (final File file : dir.listFiles()) {
      if (file.isFile()) {
        final String filename = file.getName();
        if (FilenameUtils.getExtension(filename).equals("out")) {
          final String name = FilenameUtils.removeExtension(filename);
          final String longName = dirName + "/" + name;
          final String script = longName + scriptExt;
          final String log = longName + ".log";
          final String output = longName + ".out";

          String options;
          try {
            options = new String(
                Files.readAllBytes(Paths.get(longName + ".opt")));
          } catch (final IOException e) {
            options = "";
          }

          testArgs.add(new Object[] { name, options, log, script, output });
        }
      }
    }
    return testArgs;
  }
}
