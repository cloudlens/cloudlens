#! /bin/bash

#  This file is part of the CloudLens project.
#
# Copyright 2015-2016 IBM Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

git clone https://github.com/apache/incubator-zeppelin.git -b branch-0.5.6
cd incubator-zeppelin
patch -N -p1 < ../karma.patch # workaround for zeppelin build failure on macOS
mvn clean install -Drat.skip=true -DskipTests
cd ..
cp zeppelin-confs/zeppelin-site.xml incubator-zeppelin/conf/
mvn clean generate-sources compile assembly:single
mkdir incubator-zeppelin/interpreter/cloudlens
cp target/*.jar incubator-zeppelin/interpreter/cloudlens
