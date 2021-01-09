package org.jto.extensions.treestreams

import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TreeTest {
    val intTree = treeOf(1,
        treeOf(2),
        treeOf(3, Tree(4))
    )


    @Test
    fun printOutTree() {
        println(intTree)
    }

    @Test
    fun basicTreeCharacteristics() {
        assertEquals(3, intTree.height())
        assertEquals(2, intTree.width())
        assertEquals(4, intTree.nodeCount())
        assertEquals(2, intTree.leafCount())
    }

    @Test
    fun mapPreOrderIntToString() {
        //Map tree of Int nodes to String nodes
        val resultStringTree = intTree.mapPreOrder<String>{node, _ ->  "${node.value + 1}"}

        val expectedResultTree = treeOf("2",
            treeOf("3"),
            treeOf("4", Tree("5"))
        )

        assertEquals(expectedResultTree, resultStringTree)
    }

    @Test
    fun mapPreOrderIntToString2() {
        val resultStringTree = intTree.mapPreOrder<String>{ node, transformedParentValue ->  "${node.value + 1 + (transformedParentValue?.toInt() ?: 0)}"}

        val expectedResultTree = treeOf("2",
            treeOf("5"),
            treeOf("6", Tree("11"))
        )

        assertEquals(expectedResultTree, resultStringTree)
    }

    @Test
    fun mapIndexed() {
        val indexedStringTreePreOrder = intTree.mapIndexedPreOrder<String> { node, index, _ ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth}"}

        assertEquals("""
        └-V:1 NodeIndex=0 Depth=0
          ├-V:2 NodeIndex=1 Depth=1
          └-V:3 NodeIndex=2 Depth=1
            └-V:4 NodeIndex=3 Depth=2""".trimIndent(), indexedStringTreePreOrder.toString())

        val indexedStringTreePostOrder = intTree.mapIndexedPostOrder<String> { node, index, _ ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth} Height=${index.height}"}

        assertEquals("""
        └-V:1 NodeIndex=3 Depth=0 Height=2
          ├-V:2 NodeIndex=0 Depth=1 Height=0
          └-V:3 NodeIndex=2 Depth=1 Height=1
            └-V:4 NodeIndex=1 Depth=2 Height=0""".trimIndent(), indexedStringTreePostOrder.toString())

        val levelsListsStringBreadthFirst = intTree.flatMapLevelsIndexed{index, node ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth} Width=${index.width}"}

        assertEquals("""[[V:1 NodeIndex=0 Depth=0 Width=0], [V:2 NodeIndex=1 Depth=1 Width=0, V:3 NodeIndex=2 Depth=1 Width=1], [V:4 NodeIndex=3 Depth=2 Width=0]]""",
            levelsListsStringBreadthFirst.toString())
    }

    @Test
    fun forEachIndexed() {
        val expectedResults:Queue<Pair<Int, Int>> = LinkedList()

        expectedResults.addAll(listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4))
        intTree.forEachIndexedPreOrder{ node, index ->
            val expectedResult = expectedResults.remove()
            assertEquals(expectedResult.first, index.nodeIndex)
            assertEquals(expectedResult.second, node.value)
        }
        assertTrue(expectedResults.isEmpty())

        expectedResults.addAll(listOf(0 to 2, 1 to 4, 2 to 3, 3 to 1))
        intTree.forEachIndexedPostOrder{ node, index ->
            val expectedResult = expectedResults.remove()
            assertEquals(expectedResult.first, index.nodeIndex)
            assertEquals(expectedResult.second, node.value)
        }
        assertTrue(expectedResults.isEmpty())

        expectedResults.addAll(listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4))
        intTree.forEachIndexedBreadthFirst{ node, index ->
            val expectedResult = expectedResults.remove()
            assertEquals(expectedResult.first, index.nodeIndex)
            assertEquals(expectedResult.second, node.value)
        }
        assertTrue(expectedResults.isEmpty())
    }

    @Test
    fun mapPostOrderIntToString() {
        val stringTree = intTree.mapPostOrder<String>{ node, _ ->  "${node.value + 1}"}

        with (stringTree) {
            assertEquals("2", value)
            assertEquals(2, children.size)
            with(children[0]) {
                assertEquals("3", value)
                assertTrue(children.isEmpty())
            }
            with(children[1]) {
                assertEquals("4", value)
                assertEquals(1, children.size)
                with(children[0]) {
                    assertEquals("5", value)
                    assertTrue(children.isEmpty())
                }
            }
        }
    }

    @Test
    fun mapPostOrderIntToString2() {
        val stringTree = intTree.mapPostOrder<String>{ node, transformedChildren ->  "${node.value + transformedChildren.sumBy { it.value.toInt() }}"}

        with (stringTree) {
            assertEquals("10", value)
            assertEquals(2, children.size)
            with(children[0]) {
                assertEquals("2", value)
                assertTrue(children.isEmpty())
            }
            with(children[1]) {
                assertEquals("7", value)
                assertEquals(1, children.size)
                with(children[0]) {
                    assertEquals("4", value)
                    assertTrue(children.isEmpty())
                }
            }
        }
    }

    @Test
    fun modifyChildrenPostOrder() {
        val modifiedTree = intTree.modifyChildrenPostOrder { _, children ->
            when (children.isEmpty()) {
                //For leaves (no-children) add new child with constant value 11
                true -> listOf(Tree(11))
                //For nodes with children, keep existing children and add new child having value + 1 of the last existing child
                false -> children + listOf(Tree(children.last().value + 1)) }
            }

        assertEquals("""
        └-1
          ├-2
          | └-11
          ├-3
          | ├-4
          | | └-11
          | └-5
          └-4
        """.trimIndent(), modifiedTree.toString())
    }

    /**
     * Input:         Output:
     *     1      =>    1
     *    / \          /
     *   2  3         2
     *   |
     *   4
     */
    @Test
    fun filterByPredicate() {
        val filteredTree = intTree.filterByPredicatePostOrder { node -> node.value <= 2 }

        assertEquals("""
        └-1
          └-2
        """.trimIndent(), filteredTree.toString())
    }

    @Test
    fun filterByCriteria() {
        val filteredTree = intTree.filterByCriteriaPreOrder<Int>(
            calculateCriteriaValue = { node, parentCriteriaValue ->  node.value * 2 + 1 + (parentCriteriaValue ?: 0)},
            evaluateCriteria = { criteriaValue ->  criteriaValue <= 15 } )

        assertEquals("""
        └-1
          ├-2
          └-3
        """.trimIndent(), filteredTree.toString())
    }

}