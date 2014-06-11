package org.nibali.klister

import org.nibali.klister.Klister._

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.SparkConf

object Main extends App {
  // Create Spark configuration
  val conf = new SparkConf().setAppName("Equijoin")
  // Enable event log so we can use the history server
  conf.set("spark.eventLog.enabled", "true")
  conf.set("spark.eventLog.dir", "hdfs://localhost:8020/user/cloudera/spark_logs")
  // Create Spark context
  val sc = new SparkContext(conf)

  val rdds = args.take(2).map(file => {
    sc.textFile(file).map(x => x.split("\\W+").map(_.toInt)).map(a => (a(0), a(1)))
  })
  val nReducers = args(2).toInt
  val outFile = args(3)

  val joined = rdds(0).equijoin(rdds(1), nReducers)

  joined.saveAsTextFile(outFile)

  println("Output contains " + joined.count() + " records")

  // Stop Spark context
  sc.stop()
}
