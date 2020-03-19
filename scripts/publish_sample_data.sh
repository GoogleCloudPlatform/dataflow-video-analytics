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
 
set -x 
PROJECT_ID=$(gcloud config get-value project)

gcloud pubsub topics publish ${TOPIC_ID} --message "{\"inputUri\": \"gs://cloud-samples-data/video/cat.mp4\",\"videoContext\":{\"objectTrackingConfig\":  {\"model\":\"builtin/stable\"}},\"features\":[\"OBJECT_TRACKING\"],\"locationId\":\"us-east1\"}"

gcloud pubsub topics publish ${TOPIC_ID} --message "{\"inputUri\": \"gs://cloud-samples-data/video/cat.mp4\",\"videoContext\":{\"labelDetectionConfig\":  {\"model\":\"builtin/stable\"},\"labelDetectionMode\":\"FRAME_MODE\"},\"features\":[\"LABEL_DETECTION\"],\"locationId\":\"us-east1\"}"

gcloud pubsub topics publish ${TOPIC_ID} --message "{\"inputUri\": \"gs://cloud-samples-data/video/cat.mp4\",\"videoContext\":{\"shotChangeDetectionConfig\":  {\"model\":\"builtin/stable\"}},\"features\":[\"SHOT_CHANGE_DETECTION\"],\"locationId\":\"us-east1\"}"

gcloud pubsub topics publish ${TOPIC_ID} --message "{\"inputUri\": \"gs://cloud-samples-data/video/cat.mp4\",\"videoContext\":{\"explicitContentDetectionConfig\":  {\"model\":\"builtin/stable\"}},\"features\":[\"EXPLICIT_CONTENT_DETECTION\"],\"locationId\":\"us-east1\"}"

gcloud pubsub topics publish ${TOPIC_ID} --message "{\"inputUri\": \"gs://cloud-samples-data/video/cat.mp4\",\"videoContext\":{\"speechTranscriptionConfig\":  {\"model\":\"builtin/stable\"}},\"features\":[\"SPEECH_TRANSCRIPTION\"],\"locationId\":\"us-east1\"}"

gcloud pubsub topics publish ${TOPIC_ID} --message "{\"inputUri\": \"gs://cloud-samples-data/video/cat.mp4\",\"videoContext\":{\"textDetectionConfig\":  {\"model\":\"builtin/stable\"}},\"features\":[\"TEXT_DETECTION\"],\"locationId\":\"us-east1\"}"





