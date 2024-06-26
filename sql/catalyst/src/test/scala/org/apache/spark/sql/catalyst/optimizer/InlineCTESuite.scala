/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.spark.sql.catalyst.optimizer

import org.apache.spark.sql.catalyst.analysis.TestRelation
import org.apache.spark.sql.catalyst.dsl.expressions._
import org.apache.spark.sql.catalyst.dsl.plans._
import org.apache.spark.sql.catalyst.plans.PlanTest
import org.apache.spark.sql.catalyst.plans.logical.{AppendData, CTERelationDef, CTERelationRef, LogicalPlan, OneRowRelation, WithCTE}
import org.apache.spark.sql.catalyst.rules.RuleExecutor

class InlineCTESuite extends PlanTest {

  object Optimize extends RuleExecutor[LogicalPlan] {
    val batches = Batch("inline CTE", FixedPoint(100), InlineCTE()) :: Nil
  }

  test("SPARK-48307: not-inlined CTE relation in command") {
    val cteDef = CTERelationDef(OneRowRelation().select(rand(0).as("a")))
    val cteRef = CTERelationRef(cteDef.id, cteDef.resolved, cteDef.output, cteDef.isStreaming)
    val plan = AppendData.byName(
      TestRelation(Seq($"a".double)),
      WithCTE(cteRef.except(cteRef, isAll = true), Seq(cteDef))
    ).analyze
    comparePlans(Optimize.execute(plan), plan)
  }
}
