
#!/bin/bash
# Copyright 2019 Google Inc.
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
# limitations under the License.#!/usr/bin/env bash
# Copyright 2019 Google Inc.
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
# limitations under the License.
timeout=6
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/'Stockflue_Flyaround.mp4'  gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/cat.mp4  gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/'Surenen_Pass_Trail_Running.mp4'  gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/VerticalFlyOver.mp4  gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/'Bluemlisalphutte_Flyover.mp4'  gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/'Berghouse_Leopard_Jog.mp4'   gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/'Creux_du_Van_Flight.mp4'   gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/gbikes_dinosaur.mp4   gs://${DATAFLOW_TEMPLATE_BUCKET}/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/DJI_0501.MP4   gs://drone-video-dataset/
sleep $timeout
gsutil -m cp gs://df-video-analytics-drone-dataset/'Isles_of_Glencoe.mp4'   gs://drone-video-dataset/

