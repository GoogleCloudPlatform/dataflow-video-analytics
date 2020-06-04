# Video Analytics Solution Using Dataflow & Video AI
This repo contains a series of reference implementations for video analytics solution by using Dataflow & Video AI.  Goal for this  code repo is to provide an easy to use end to end solution to process large scale unstructured video data by bringing mutiple data streams together  to drive insight using Video AI. 

1. Near realtime object detection  in video clips  by using Video Intellgence API.    
2. Streaming Live Video Analytics (coming soon!).  

## Table of Contents  
* [Object Detection in Video Clips](#object_detection_in_video_clips).  
	* [Reference Architecture](#reference_architecture_using_video_intelligence_api).      
	* [Build & Run Using Dataflow Flex Template](#build_run).  
	* [Test Using a Drone  Video Clip Dataset from Kaggle ](#test). 
 

## Object Detection in Video Clips 
Customers in the alarm monitoring, television, insurance and other industries are producing large volumes of unstructured  data and are looking for solutions for augmenting unstructured data streams with other streams of events (for example, motion sensor events) and building analytical solutions to drive their operations and reporting. For example, alarm monitoring companies want to augment motion sensor data with the analysis of video clips (and, eventually, live video feeds) to determine if a dispatch of a security team to a customer’s premises is justified and therefore reduce the false positive rate that drives the costs of their operations up.

### Reference Architecture Using Video Intelligence API
 ![ref_arch](diagram/video_blog_diagram.png)

### Build & Run
1. Create  two buckets , one to store input dataset and another one to store fle template config file

```export DRONE_VIDEO_CLIPS_BUCKET=${PROJECT_ID}_drone_dataset
export DF_TEMPLATE_BUCKET=${PROJECT_ID}_df_temp_config
gsutil mb -c standard -l <var>region</var> gs://${DRONE_VIDEO_CLIPS_BUCKET}
gsutil mb -c standard -l <var>region</var> gs://${DF_TEMPLATE_BUCKET}
```

2. Create requried topics and subcriptions as below

```export INPUT_TOPIC_ID=<var>topic-id</var>
export INPUT_SUBSCRIPTION_ID=<var>subscription-id</var>
export OUTPUT_TOPIC_ID=<var>topic-id</var>
export OUTPUT_SUBSCRIPTION_ID=<var>subscription-id</var>
gcloud pubsub topics create $INPUT_TOPIC_ID
gcloud pubsub subscriptions create $INPUT_SUBSCRIPTION_ID --topic=$OUTPUT_TOPIC_ID
gcloud pubsub topics create $OUTPUT_TOPIC_ID
gcloud pubsub subscriptions create $OUTPUT_SUBSCRIPTION_ID --topic=$OUTPUT_TOPIC_ID
```

3. Create a BigQuery Dataset and Table. 

```export DATASET_NAME=<var>dataset-name</var>
bq --location=US mk -d \
--description "vide-object-tracking-dataset" \
${DATASET_NAME}
bq mk -t --schema src/main/resources/table_schema.json \
--description "object_tracking_data" \
${PROJECT_ID}:${DATASET_NAME}.object_tracking_analysis
```

4. Gradle Build

```gradle spotlessApply -DmainClass=com.google.solutions.df.video.analytics.VideoAnalyticsPipeline 
gradle build -DmainClass=com.google.solutions.df.video.analytics.VideoAnalyticsPipeline 
```  

5.  Trigger using Gradle Run 
This configuration is defaulted to 1

- 1 second processing time
- filter for window and person entity with confidence greater than 90%

```
gradle run -DmainClass=com.google.solutions.df.video.analytics.VideoAnalyticsPipeline \
-Pargs=" --topicId=projects/next-demo-2020/topics/video-analysis --runner=DirectRunner \
--project=${PROJECT_ID} --autoscalingAlgorithm=THROUGHPUT_BASED --workerMachineType=n1-highmem-4 \
--numWorkers=3 --maxNumWorkers=5 --region=us-central1 \
--subscriberId=projects/${PROJECT_ID}/subscriptions/${INPUT_SUBSCRIPTION_ID} \
--features=OBJECT_TRACKING --entity=window,person --windowInterval=1 \
--keyRange=8 --tableSpec=${PROJECT_ID}:${DATASET_NAME}.object_tracking_analysis" \
--confidence=0.9 --topicId=projects/${PROJECT_ID}/topic/${OUTPUT_TOPIC_ID} 
```

6. Create a docker image for flex template. 
 
```gradle jib --image=gcr.io/${PROJECT_ID}/df-video-analytics:latest -DmainClass=com.google.solutions.df.video.analytics.VideoAnalyticsPipeline
```

7. Update the template config file src/main/resources/dynamic_template_video_analytics.json with image name and upload it to a Cloud Storage bucket.

```nano dynamic_template_video_analytics.json
{"image": "gcr.io/<var>project-id</var>/df-video-analytics",
 "sdk_info": {"language": "JAVA"}
}
gsutil cp src/main/resources/dynamic_template_video_analytics.json gs://${DF_TEMPLATE_BUCKET}/
```


8. Trigger using gcloud flex template

```gcloud beta dataflow flex-template run "video-object-tracking" \
--project=${PROJECT_ID} --region=us-central1 \ --template-file-gcs-location=gs://${DF_TEMPLATE_BUCKET}/dynamic_template_video_analytics.json \
--parameters=^~^autoscalingAlgorithm="NONE"~numWorkers=5~maxNumWorkers=5 \
~workerMachineType=n1-highmem-4 \
~subscriberId=projects/${PROJECT_ID}/subscriptions/${INPUT_SUBSCRIPTION_ID} \
~tableSpec=${PROJECT_ID}:${DATASET_NAME}.object_tracking_analysis \
~features=OBJECT_TRACKING~entity=window,person~windowInterval=1 \
~streaming=true~keyRange=8~confidence=0.9 \
~topicId=projects/${PROJECT_ID}/topics/${OUTPUT_TOPIC_ID} 
```


#### Dataflow Pipeline
 ![ref_arch](diagram/video_dag.png)
 
### Test
1. Enable GCS metadata notification for the PubSub and copy dataset to your bucket. 

```
gsutil notification create -t ${INPUT_TOPIC_ID} -f json gs://${DRONE_VIDEO_CLIPS_BUCKET}
```





