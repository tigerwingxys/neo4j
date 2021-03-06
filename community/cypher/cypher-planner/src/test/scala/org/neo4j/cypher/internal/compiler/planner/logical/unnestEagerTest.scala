/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.cypher.internal.compiler.planner.logical

import org.neo4j.cypher.internal.compiler.planner.LogicalPlanningTestSupport
import org.neo4j.cypher.internal.compiler.planner.logical.Eagerness.unnestEager
import org.neo4j.cypher.internal.expressions.PropertyKeyName
import org.neo4j.cypher.internal.ir.CreateNode
import org.neo4j.cypher.internal.logical.plans.Apply
import org.neo4j.cypher.internal.logical.plans.Create
import org.neo4j.cypher.internal.logical.plans.DeleteNode
import org.neo4j.cypher.internal.logical.plans.DeleteRelationship
import org.neo4j.cypher.internal.logical.plans.DetachDeleteNode
import org.neo4j.cypher.internal.logical.plans.LogicalPlan
import org.neo4j.cypher.internal.logical.plans.RemoveLabels
import org.neo4j.cypher.internal.logical.plans.SetLabels
import org.neo4j.cypher.internal.logical.plans.SetNodePropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetNodeProperty
import org.neo4j.cypher.internal.logical.plans.SetPropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetProperty
import org.neo4j.cypher.internal.logical.plans.SetRelationshipPropertiesFromMap
import org.neo4j.cypher.internal.logical.plans.SetRelationshipProperty
import org.neo4j.cypher.internal.util.attribution.Attributes
import org.neo4j.cypher.internal.util.helpers.fixedPoint
import org.neo4j.cypher.internal.util.test_helpers.CypherFunSuite

class unnestEagerTest extends CypherFunSuite with LogicalPlanningTestSupport {

  test("should unnest create from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val create = Create(rhs, nodes = List(CreateNode("a", Seq.empty, None)), Nil)
    val input = Apply(lhs, create)

    rewrite(input) should equal(create.copy(source = Apply(lhs, rhs)))
  }

  test("should unnest delete relationship from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val delete = DeleteRelationship(rhs, null)
    val input = Apply(lhs, delete)

    rewrite(input) should equal(DeleteRelationship(Apply(lhs, rhs), null))
  }

  test("should unnest delete node from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val delete = DeleteNode(rhs, null)
    val input = Apply(lhs, delete)

    rewrite(input) should equal(DeleteNode(Apply(lhs, rhs), null))
  }

  test("should unnest detach delete node from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val delete = DetachDeleteNode(rhs, null)
    val input = Apply(lhs, delete)

    rewrite(input) should equal(DetachDeleteNode(Apply(lhs, rhs), null))
  }

  test("should unnest set node property from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetNodeProperty(rhs, "a", PropertyKeyName("prop")(pos), null)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetNodeProperty(Apply(lhs, rhs), "a", PropertyKeyName("prop")(pos), null))
  }

  test("should unnest set rel property from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetRelationshipProperty(rhs, "a", PropertyKeyName("prop")(pos), null)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetRelationshipProperty(Apply(lhs, rhs), "a", PropertyKeyName("prop")(pos), null))
  }

  test("should unnest set generic property from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetProperty(rhs, varFor("a"), PropertyKeyName("prop")(pos), null)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetProperty(Apply(lhs, rhs), varFor("a"), PropertyKeyName("prop")(pos), null))
  }

  test("should unnest set node property from map from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetNodePropertiesFromMap(rhs, "a", null, removeOtherProps = false)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetNodePropertiesFromMap(Apply(lhs, rhs), "a", null, removeOtherProps = false))
  }

  test("should unnest set relationship property from map from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetRelationshipPropertiesFromMap(rhs, "a", null, removeOtherProps = false)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetRelationshipPropertiesFromMap(Apply(lhs, rhs), "a", null, removeOtherProps = false))
  }

  test("should unnest set generic property from map from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetPropertiesFromMap(rhs, varFor("a"), null, removeOtherProps = false)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetPropertiesFromMap(Apply(lhs, rhs), varFor("a"), null, removeOtherProps = false))
  }

  test("should unnest set labels from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val set = SetLabels(rhs, "a", Seq.empty)
    val input = Apply(lhs, set)

    rewrite(input) should equal(SetLabels(Apply(lhs, rhs), "a", Seq.empty))
  }

  test("should unnest remove labels from rhs of apply") {
    val lhs = newMockedLogicalPlan()
    val rhs = newMockedLogicalPlan()
    val remove = RemoveLabels(rhs, "a", Seq.empty)
    val input = Apply(lhs, remove)

    rewrite(input) should equal(RemoveLabels(Apply(lhs, rhs), "a", Seq.empty))
  }

  private def rewrite(p: LogicalPlan) =
    fixedPoint((p: LogicalPlan) => p.endoRewrite(unnestEager(new StubSolveds, Attributes(idGen))))(p)
}
