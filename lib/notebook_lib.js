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

function printOutliers(arr, sdev, map) {
	print("outliers:");
	for (i = 0; i < arr.length; i++) {
		if (arr[i] > sdev) {
			print(map[arr[i]] + ":" + arr[i]);
		}
	}
}


function histogram(data, sdev, max, name) {
    map = {};
    binl = Math.floor(sdev/8);
    maxbin = Math.floor(max/binl);
    print("max=" + max + " maxbin=" + maxbin + " binl=" + binl);
    for(i = 0; i <= maxbin; i++){
        map[i] = 0;
    }
    for(i = 0; i < data.length; i++){
       map[Math.floor(data[i]/binl)]++;
    }
    var s = "%table x\t"+name+"\n";
    for(i = 0; i <= maxbin; i++){
        s += i*binl + "\t" + map[i]  + "\n";
    }
    print(s);
}
