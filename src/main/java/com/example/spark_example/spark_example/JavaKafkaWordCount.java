package com.example.spark_example.spark_example;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaPairReceiverInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka.KafkaUtils;
import org.spark_project.guava.collect.Lists;

import scala.Tuple2;

/**
 * word count streaming kafka
 * 
 * @author yaokai
 *
 */
public class JavaKafkaWordCount {
	private static final Pattern SPACE = Pattern.compile(" ");
	private JavaKafkaWordCount() {
	}
    
	public static void main(String[] args) throws Exception {

		// StreamingExamples.setStreamingLogLevels();
		SparkConf sparkConf = new SparkConf().setAppName("JavaKafkaWordCount");
		// Create the context with 30 seconds batch size
		JavaStreamingContext jssc = new JavaStreamingContext(sparkConf, new Duration(30 * 1000));
		String zkQuorum = "192.168.157.131:2181,192.168.157.129:2181,192.168.157.130:2181";
		String group = "test";
		String topics = "test-topic,test-topic3";
		int numThreads = 2;
		Map<String,Integer> tipicMap = new HashMap<String,Integer>();
		for(String topic:topics.split(",")){
			tipicMap.put(topic, numThreads);
		}
		JavaPairReceiverInputDStream<String,String> lines = KafkaUtils.createStream(jssc, zkQuorum, group, tipicMap);
		
		JavaDStream<String> words = lines.flatMap(new FlatMapFunction<Tuple2<String,String>,String>(){

			/**
			 * 
			 */
			private static final long serialVersionUID = -9185339503913930927L;

			@Override
			public Iterator<String> call(Tuple2<String, String> t) throws Exception {
				return Lists.newArrayList(SPACE.split(t._2)).iterator();
			}
			
		});
		JavaPairDStream<String, Integer> pairs = words.mapToPair(new PairFunction<String, String, Integer>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Tuple2<String, Integer> call(String word) throws Exception {
				return new Tuple2<String, Integer>(word, 1);
			}

		});
		JavaPairDStream<String, Integer> wordcount = pairs.reduceByKey(new Function2<Integer, Integer, Integer>() {

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public Integer call(Integer v1, Integer v2) throws Exception {
				return v1 + v2;
			}

		});

		wordcount.print();
		jssc.start();
		jssc.awaitTermination();
		jssc.close();
	}
}