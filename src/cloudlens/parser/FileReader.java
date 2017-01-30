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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;

import cloudlens.engine.CLException;

public class FileReader {

  public static InputStream fetchFile(String urlString) {
    try {
      InputStream inputStream;
      URL url;
      if (urlString.startsWith("local:")) {
        final String path = urlString.replaceFirst("local:", "");
        inputStream = Files.newInputStream(Paths.get(path));
      } else if (urlString.startsWith("file:")) {
        url = new URL(urlString);
        inputStream = Files.newInputStream(Paths.get(url.toURI()));
      } else if (urlString.startsWith("http:")
          || urlString.startsWith("https:")) {
        url = new URL(urlString);
        final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoInput(true);
        final Matcher matcher = Pattern.compile("//([^@]+)@")
            .matcher(urlString);
        if (matcher.find()) {
          final String encoding = Base64.getEncoder()
              .encodeToString(matcher.group(1).getBytes());
          conn.setRequestProperty("Authorization", "Basic " + encoding);
        }
        conn.setRequestMethod("GET");
        inputStream = conn.getInputStream();
      } else {
        throw new CLException(
            "supported protocols are: http, https, file, and local.");
      }
      return inputStream;
    } catch (IOException | URISyntaxException e) {
      throw new CLException(e.getMessage());
    }
  }

  public static ArrayList<String> fullPaths(String[] filePaths) {
    final ArrayList<String> res = new ArrayList<>();
    if (filePaths != null) {
      for (final String filePath : filePaths) {
        final File file = new File(filePath);
        if (!file.exists()) {
          throw new CLException(filePath + ": no such file.");
        }
        final String filename = file.getName();
        if (!FilenameUtils.getExtension(filename).equals("js")) {
          throw new CLException(filePath + " is not a JS file.");
        }
        final String abspath = file.getAbsolutePath();
        res.add(abspath);
      }
    }
    return res;

  }

  public static InputStream readFiles(String[] fileNames) throws IOException {
    try {
      final List<InputStream> streams = new ArrayList<>();
      for (final String name : fileNames) {
        streams.add(new FileInputStream(new File(name)));
      }
      final Enumeration<InputStream> files = Collections.enumeration(streams);
      return new SequenceInputStream(files);
    } catch (final IOException e) {
      throw new CLException(e.getMessage());
    }
  }
}
