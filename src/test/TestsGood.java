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

import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class TestsGood {
  Tester tester;
  String name;
  String options;
  String script;
  String log;
  String output;

  public TestsGood(String name, String options, String log, String script,
      String output) {
    this.tester = new Tester();
    this.name = name;
    this.options = options;
    this.log = log;
    this.script = script;
    this.output = output;
  }

  @Before
  public void setUp() {
    tester.setUpStreams();
  }

  @After
  public void cleanUp() {
    tester.cleanUpStreams();
  }

  @Test
  public void test() throws Exception {
    tester.test(this.options, this.log, this.script, this.output);
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Tester.getTests("tests/good", ".lens");
  }
}