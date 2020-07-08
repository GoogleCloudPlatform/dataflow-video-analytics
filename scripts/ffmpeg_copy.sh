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
sudo apt update
sudo apt install ffmpeg
 // [START loadSnippet_1]
for file in *.mp4
do
ffmpeg -i "$file" -codec:a aac  -ac 2  -ar 48k -c copy -movflags faststart -f segment -segment_format mpegts  -segment_time 5 "${file%.*}~"%1d.mp4
done
gsutil -m cp *~*.mp4 gs://${VIDEO_CLIPS_BUCKET}/
 // [END loadSnippet_1]
