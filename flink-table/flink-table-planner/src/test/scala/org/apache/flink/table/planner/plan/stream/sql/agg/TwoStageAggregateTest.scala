/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.flink.table.planner.plan.stream.sql.agg

import org.apache.flink.api.scala._
import org.apache.flink.table.api._
import org.apache.flink.table.api.config.{AggregatePhaseStrategy, OptimizerConfigOptions}
import org.apache.flink.table.planner.utils.TableTestBase

import org.junit.jupiter.api.{BeforeEach, Test}

import java.time.Duration

class TwoStageAggregateTest extends TableTestBase {

  private val util = streamTestUtil()
  util.addTableSource[(Int, Long, String)]("MyTable", 'a, 'b, 'c)

  @BeforeEach
  def before(): Unit = {
    util.enableMiniBatch()
    util.tableEnv.getConfig.setIdleStateRetention(Duration.ofHours(1))
    util.tableEnv.getConfig.set(
      OptimizerConfigOptions.TABLE_OPTIMIZER_AGG_PHASE_STRATEGY,
      AggregatePhaseStrategy.TWO_PHASE)
  }

  @Test
  def testCountWithGroupBy(): Unit = {
    util.verifyExecPlan("SELECT COUNT(a) FROM MyTable GROUP BY b")
  }

  @Test
  def testCountWithoutGroupBy(): Unit = {
    util.verifyExecPlan("SELECT COUNT(a) FROM MyTable")
  }

  @Test
  def testAvgWithGroupBy(): Unit = {
    util.verifyExecPlan("SELECT AVG(a) FROM MyTable GROUP BY b")
  }

  @Test
  def testAvgWithoutGroupBy(): Unit = {
    util.verifyRelPlanWithType("SELECT AVG(CAST(a AS DOUBLE)) FROM MyTable")
  }

  @Test
  def testGroupAggregateWithFilter(): Unit = {
    util.verifyExecPlan("SELECT * FROM (SELECT b, SUM(a) FROM MyTable GROUP BY b) WHERE b = 2")
  }

  @Test
  def testGroupAggregateWithExpressionInSelect(): Unit = {
    util.verifyExecPlan(
      "SELECT MIN(c), AVG(a) FROM " +
        "(SELECT a, b + 3 AS d, c FROM MyTable) GROUP BY d")
  }

  @Test
  def testGroupAggregateWithConstant(): Unit = {
    util.verifyExecPlan(
      "SELECT four, SUM(a) FROM " +
        "(SELECT b, 4 AS four, a FROM MyTable) GROUP BY b, four")
  }
}
