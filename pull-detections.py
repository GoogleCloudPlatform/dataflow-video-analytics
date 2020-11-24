# Copyright 2020 Google LLC
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

import json
import argparse
from google.cloud import pubsub_v1

def pull(project, subscription):
    subscriber = pubsub_v1.SubscriberClient()
    subscription_path = subscriber.subscription_path(project, subscription)
    streaming_pull_future = subscriber.subscribe(subscription_path, callback=callback)
    print("\nListening for messages on the Pub/Sub subscription `{}`...\n".format(subscription_path))
    with subscriber:
        streaming_pull_future.result()

def callback(message):
    try:
        data = json.loads(message.data)
        print ("""------[ Entity detected ]-------
Entity: {}
File: {}
""".format(data['entity'], data['file_name']))
    except KeyError:
        print ("------[ Error ]-------\n{}\n".format(message.data))
    else:
        message.ack()

if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('--project', required=True, help='Your Google Cloud project')
    parser.add_argument('--subscription', required=True, help='The Pub/Sub subscription to pull from')
    args = parser.parse_args()
    pull(args.project, args.subscription)