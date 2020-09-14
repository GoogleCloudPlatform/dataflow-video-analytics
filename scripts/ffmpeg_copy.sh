#!/usr/bin/env bash
# Copyright 2020 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License
rm -r *.mp4
gsutil -m cp $1 . 
for file in *.mp4
do
ffmpeg -i "$file" -c:v libx264 -crf 22 -map 0 -segment_time 3 -reset_timestamps 0 -g 30 -sc_threshold 0 -force_key_frames "expr:gte(t,n_forced*1)>
 for split_file in *~*.mp4
 do
   ffmpeg -i "$split_file" -c copy -map 0 -movflags faststart "modified_$split_file"
 done
done
gsutil -m cp *modified_*.mp4 $2
