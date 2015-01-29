package net.mm.composer.relations.execution

import net.mm.composer.properties.{FieldProperty, RelationProperty}
import net.mm.composer.relations._
import org.scalatest.FunSuite
import org.scalatest.Matchers._

class ExecutionPlanBuilderSuite extends FunSuite with TestCasesRelationRegistry with TestCases {

  val executionPlan = new ExecutionPlanBuilderImpl

  //product(categories(products(categories())))

  test("no relation") {
    val propertyTree = RelationProperty("product")
    executionPlan[Product](propertyTree) shouldBe Seq.empty
  }

  test("with reviews relation") {
    val propertyTree = RelationProperty("product", RelationProperty("reviews"))
    executionPlan[Product](propertyTree) shouldBe Seq(TaskNode("reviews", relationRegistry.get[Product]("reviews").get))
  }

  test("optimized execution plan on key") {
    val propertyTree = RelationProperty("product", RelationProperty("reviews", RelationProperty("categories")))
    val plan = executionPlan[Product](propertyTree)

    plan shouldBe Seq(
      TaskNode("categories", relationRegistry.get[Review]("categories").get),
      TaskNode("reviews", relationRegistry.get[Product]("reviews").get)
    )
  }

  test("optimized execution plan on key with cost based sorting") {
    val propertyTree = RelationProperty("product", RelationProperty("reviews", RelationProperty("categories", RelationProperty("size"))))
    val plan = executionPlan[Product](propertyTree)

    plan shouldBe Seq(
      TaskNode("reviews", relationRegistry.get[Product]("reviews").get),
      TaskNode("categories", relationRegistry.get[Review]("categories").get,
        TaskNode("size", relationRegistry.get[Category]("size").get)
      )
    )
  }

  test("execution plan with leaf nodes") {
    val propertyTree = RelationProperty("product", FieldProperty("title"), FieldProperty("description"),
      RelationProperty("reviews", FieldProperty("rating"), FieldProperty("text"),
        RelationProperty("reviewer", FieldProperty("username"), FieldProperty("avatar"))
      )
    )

    val plan = executionPlan[Product](propertyTree)

    plan shouldBe Seq(
      TaskNode("reviews", relationRegistry.get[Product]("reviews").get,
        TaskNode("reviewer", relationRegistry.get[Review]("reviewer").get)
      )
    )
  }
}
