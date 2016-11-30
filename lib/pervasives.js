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

function match(entry, input, regex){
    var str = entry[input];
    var m = regex.exec(str);
    if (m != null){
	for (i=3; i<arguments.length; i++){
            entry[arguments[i]] = m[i-2];
	}
    } 
}

function min(array){
    return Math.min.apply(Math,array);
}

function max(array){
    return Math.max.apply(Math,array);
}

function unique(array){
    return array.filter(
	function(value, index, self){
	    return self.indexOf(value) === index;
	});
}

function sort(array){
    return array.sort();
}

function filter(array, f){
    return array.filter(
	function(v){
	    return f(v);
	});
}

function standardDeviation(values){
  var avg = average(values);
  
  var squareDiffs = values.map(function(value){
    var diff = value - avg;
    var sqrDiff = diff * diff;
    return sqrDiff;
  });
  
  var avgSquareDiff = average(squareDiffs);

  var stdDev = Math.sqrt(avgSquareDiff);
  return stdDev;
}

function average(data){
  var sum = data.reduce(function(sum, value){
    return sum + value;
  }, 0);

  var avg = sum / data.length;
  return avg;
}
