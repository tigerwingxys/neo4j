package org.neo4j.cypher.internal.compiler.v2_1.planner

import org.neo4j.cypher.internal.commons.CypherFunSuite
import org.neo4j.cypher.internal.compiler.v2_1.parser.ParserFixture
import org.neo4j.cypher.internal.compiler.v2_1.ast._
import org.neo4j.cypher.internal.compiler.v2_1.ast.Where
import org.neo4j.cypher.internal.compiler.v2_1.ast.Match
import org.neo4j.cypher.internal.compiler.v2_1.ast.SingleQuery
import org.neo4j.cypher.internal.compiler.v2_1.ast.Query
import org.neo4j.cypher.internal.compiler.v2_1.{RelTypeId, LabelId, PropertyKeyId}
import org.neo4j.cypher.internal.compiler.v2_1.spi.PlanContext
import org.mockito.Mockito._
import org.neo4j.graphdb.Direction

class SimpleTokenResolverTest extends CypherFunSuite {

  import ParserFixture._

  val resolver = new SimpleTokenResolver
  
  parseTest("match n where n.name = 'Resolved' return *") { query =>
    val planContext = mock[PlanContext]
    when(planContext.getOptPropertyKeyId("name")).thenReturn(Some(12))

    resolver.resolve(query)(planContext) match {
      case Query(_,
        SingleQuery(Seq(
          Match(
            false,
            Pattern(Seq(EveryPath(NodePattern(Some(Identifier("n")), Seq(), None, true)))),
            Seq(),
            Some(Where(Equals(Property(Identifier("n"), pkToken), StringLiteral("Resolved"))))
          ),
          Return(false, ReturnAll(), None, None, None)
        ))) =>
            pkToken.name should equal("name")
            pkToken.id should equal(Some(PropertyKeyId(12)))
    }
  }

  parseTest("match n where n.name = 'Unresolved' return *") { query =>
    val planContext = mock[PlanContext]
    when(planContext.getOptPropertyKeyId("name")).thenReturn(None)

    resolver.resolve(query)(planContext) match {
      case Query(_,
        SingleQuery(Seq(
          Match(
            false,
            Pattern(Seq(EveryPath(NodePattern(Some(Identifier("n")), Seq(), None, true)))),
            Seq(),
            Some(Where(Equals(Property(Identifier("n"), pkToken), StringLiteral("Unresolved"))))
          ),
          Return(false, ReturnAll(), None, None, None)
        ))) =>
            pkToken.name should equal("name")
            pkToken.id should equal(None)
    }
  }

  parseTest("match n where n:Resolved return *") { query =>
    val planContext = mock[PlanContext]
    when(planContext.getOptLabelId("Resolved")).thenReturn(Some(12))

    resolver.resolve(query)(planContext) match {
      case Query(_,
      SingleQuery(Seq(
        Match(
          false,
          Pattern(Seq(EveryPath(NodePattern(Some(Identifier("n")), Seq(), None, true)))),
          Seq(),
          Some(Where(HasLabels(Identifier("n"), Seq(labelToken))))
        ),
        Return(false, ReturnAll(), None, None, None)
      ))) =>
        labelToken.name should equal("Resolved")
        labelToken.id should equal(Some(LabelId(12)))
    }
  }

  parseTest("match n where n:Unresolved return *") { query =>
    val planContext = mock[PlanContext]
    when(planContext.getOptLabelId("Unresolved")).thenReturn(None)

    resolver.resolve(query)(planContext) match {
      case Query(_,
      SingleQuery(Seq(
        Match(
          false,
          Pattern(Seq(EveryPath(NodePattern(Some(Identifier("n")), Seq(), None, true)))),
          Seq(),
          Some(Where(HasLabels(Identifier("n"), Seq(labelToken))))
        ),
        Return(false, ReturnAll(), None, None, None)
      ))) =>
        labelToken.name should equal("Unresolved")
        labelToken.id should equal(None)
    }
  }

  parseTest("match ()-[:RESOLVED]->() return *") { query =>
    val planContext = mock[PlanContext]
    when(planContext.getOptRelTypeId("RESOLVED")).thenReturn(Some(12))

    resolver.resolve(query)(planContext) match {
      case Query(_,
      SingleQuery(Seq(
        Match(
          false,
          Pattern(Seq(EveryPath(RelationshipChain(
            NodePattern(None, Seq(), None, false),
            RelationshipPattern(None, false, Seq(relTypeToken), None, None, Direction.OUTGOING),
            NodePattern(None, Seq(), None, false)
          )))),
          Seq(),
          None
        ),
        Return(false, ReturnAll(), None, None, None)
      ))) =>
        relTypeToken.name should equal("RESOLVED")
        relTypeToken.id should equal(Some(RelTypeId(12)))
    }
  }

  parseTest("match ()-[:UNRESOLVED]->() return *") { query =>
    val planContext = mock[PlanContext]
    when(planContext.getOptRelTypeId("UNRESOLVED")).thenReturn(None)

    resolver.resolve(query)(planContext) match {
      case Query(_,
      SingleQuery(Seq(
        Match(
          false,
          Pattern(Seq(EveryPath(RelationshipChain(
            NodePattern(None, Seq(), None, false),
            RelationshipPattern(None, false, Seq(relTypeToken), None, None, Direction.OUTGOING),
            NodePattern(None, Seq(), None, false)
          )))),
          Seq(),
          None
        ),
        Return(false, ReturnAll(), None, None, None)
      ))) =>
        relTypeToken.name should equal("UNRESOLVED")
        relTypeToken.id should equal(None)
    }
  }

  def parseTest(queryText: String)(f: Query => Unit) = test(queryText) { parser.parse(queryText) match {
    case query: Query => f(query)
    } 
  }
}
