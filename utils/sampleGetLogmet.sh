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

java -cp target/cloudlens-0.0.1-SNAPSHOT-jar-with-dependencies.jar readers.LogmetReader logmet.ng.bluemix.net <spaceid> <auth-token> 2015-12-13T15:10:18.163Z 2015-12-14T15:10:18.163Z
