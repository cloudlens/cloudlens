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

import java.io.InputStream;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import cloudlens.engine.CL;
import cloudlens.engine.CLException;
import cloudlens.parser.ASTBuilder;
import cloudlens.parser.ASTElement;
import cloudlens.parser.ASTException;
import cloudlens.parser.FileReader;

public class Main {

  public static void main(String[] args) throws Exception {
    final CommandLineParser optionParser = new DefaultParser();
    final HelpFormatter formatter = new HelpFormatter();

    final Option lens = Option.builder("r").longOpt("run").hasArg()
        .argName("lens file").desc("Lens file.").required(true).build();
    final Option log = Option.builder("l").longOpt("log").hasArg()
        .argName("log file").desc("Log file.").build();
    final Option jsonpath = Option.builder().longOpt("jsonpath").hasArg()
        .argName("path").desc("Path to logs in a json object.").build();
    final Option js = Option.builder().longOpt("js").hasArg().argName("js file")
        .desc("Load JS file.").build();
    final Option format = Option.builder("f").longOpt("format").hasArg()
        .desc("Choose log format (text or json).").build();
    final Option streaming = Option.builder().longOpt("stream")
        .desc("Streaming mode.").build();
    final Option history = Option.builder().longOpt("history")
        .desc("Store history.").build();

    final Options options = new Options();
    options.addOption(log);
    options.addOption(lens);
    options.addOption(format);
    options.addOption(jsonpath);
    options.addOption(js);
    options.addOption(streaming);
    options.addOption(history);

    try {
      final CommandLine cmd = optionParser.parse(options, args);

      final String jsonPath = cmd.getOptionValue("jsonpath");
      final String[] jsFiles = cmd.getOptionValues("js");
      final String[] lensFiles = cmd.getOptionValues("run");
      final String[] logFiles = cmd.getOptionValues("log");
      final String source = cmd.getOptionValue("format");

      final boolean stream = cmd.hasOption("stream") || !cmd.hasOption("log");
      final boolean withHistory = cmd.hasOption("history") || !stream;

      final CL cl = new CL(System.out, System.err, stream, withHistory);

      try {
        final InputStream input = (cmd.hasOption("log"))
            ? FileReader.readFiles(logFiles) : System.in;

        if (source == null) {
          cl.source(input);
        } else {
          switch (source) {
          case "text":
            cl.source(input);
            break;
          case "json":
            cl.json(input, jsonPath);
            break;
          default:
            input.close();
            throw new CLException("Unsupported format: " + source);
          }
        }

        for (final String jsFile : FileReader.fullPaths(jsFiles)) {
          cl.engine.eval("CL.loadjs('file://" + jsFile + "')");
        }

        final List<ASTElement> top = ASTBuilder.parseFiles(lensFiles);
        cl.launch(top);

      } catch (final CLException | ASTException e) {
        cl.errWriter.println(e.getMessage());
      } finally {
        cl.outWriter.flush();
        cl.errWriter.flush();
      }
    } catch (final ParseException e) {
      System.err.println(e.getMessage());
      formatter.printHelp("cloudlens", options);
    }
  }
}