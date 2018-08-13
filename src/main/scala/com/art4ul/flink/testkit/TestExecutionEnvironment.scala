package com.art4ul.flink.testkit

import com.art4ul.flink.testkit.sink.ResultBuffer
import com.typesafe.scalalogging.LazyLogging
import org.apache.flink.api.common.JobExecutionResult
import org.apache.flink.configuration.{Configuration, RestOptions, TaskManagerOptions}
import org.apache.flink.runtime.jobgraph.JobGraph
import org.apache.flink.runtime.minicluster.{MiniCluster, MiniClusterConfiguration}
import org.apache.flink.streaming.api.scala.{StreamExecutionEnvironment => ScalaStreamExecutionEnvironment}
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment
import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}

class ScalaTestExecutionEnvironment(javaEnv: TestExecutionEnvironment) extends ScalaStreamExecutionEnvironment(javaEnv) {

  def close: Unit = javaEnv.close

  def forceClose: Unit = javaEnv.forceClose

  def getMiniCluster: MiniCluster = javaEnv.miniCluster
}

object ScalaTestExecutionEnvironment {
  def apply(): ScalaTestExecutionEnvironment = new ScalaTestExecutionEnvironment(new TestExecutionEnvironment)
}

class TestExecutionEnvironment(configuration: Configuration) extends StreamExecutionEnvironment with LazyLogging {

  def this() = this(new Configuration())

  var miniCluster: MiniCluster = _
  var jobGraph: JobGraph = _
  @volatile private var running: Boolean = false
  @volatile private var resultException: Throwable = null


  def checkSinkCompleteness: Unit = {
    implicit val ec = ExecutionContext.global
    Future {
      var run: Boolean = true
      while (run) {
        val completeAll = ResultBuffer.sinkStates
          .values()
          .forall(f => f.isCompleted)

        if (completeAll) {
          val values = ResultBuffer.sinkStates.values().flatMap(_.value)
          val failed = values.filter(_.isFailure)
          if (failed.isEmpty) {
            forceClose
          } else {
            resultException = failed.head.failed.get
            forceClose
            run = false
          }
        }
        Thread.sleep(100)
      }
    }
  }

  protected override def execute(jobName: String): JobExecutionResult = {
    val streamGraph = getStreamGraph()
    streamGraph.setJobName(jobName)

    jobGraph = streamGraph.getJobGraph()
    jobGraph.setAllowQueuedScheduling(true)

    val conf = new Configuration()
    conf.addAll(jobGraph.getJobConfiguration())
    conf.setString(TaskManagerOptions.MANAGED_MEMORY_SIZE, "0")

    // add (and override) the settings with what the user defined
    conf.addAll(this.configuration)

    if (!conf.contains(RestOptions.PORT)) {
      conf.setInteger(RestOptions.PORT, 0)
    }

    val numSlotsPerTaskManager = conf.getInteger(TaskManagerOptions.NUM_TASK_SLOTS, jobGraph.getMaximumParallelism())

    val cfg = new MiniClusterConfiguration.Builder()
      .setConfiguration(conf)
      .setNumSlotsPerTaskManager(numSlotsPerTaskManager)
      .build()

    logger.info("Running job on local embedded Flink mini cluster")


    miniCluster = new MiniCluster(cfg)
    try {
      miniCluster.start();
      conf.setInteger(RestOptions.PORT, miniCluster.getRestAddress().getPort())
      running = true

      checkSinkCompleteness
      miniCluster.executeJobBlocking(jobGraph);
    } catch {
      case ex: Throwable =>
        if (running) {
          throw ex
        } else if (!running && resultException == null) { //Igonre if shutdown
          new JobExecutionResult(null, 0, null)
        } else {
          throw resultException
        }
    } finally {
      close
    }
  }

  def close: Unit = {
    transformations.clear()
    miniCluster.close()
  }

  def forceClose: Unit = {
    running = false
    transformations.clear()
    miniCluster.close()
  }
}
