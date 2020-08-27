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
    fun basicTreeCharacteristics() {
        println(intTree)
        assertEquals(3, intTree.height())
        assertEquals(2, intTree.width())
        assertEquals(4, intTree.nodeCount())
        assertEquals(2, intTree.leafCount())
    }

    @Test
    fun mapPreOrderIntToString() {
        val stringTree = intTree.mapPreOrder<String>{node, _ ->  "${node.value + 1}"}

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
    fun mapPreOrderIntToString2() {
        val stringTree = intTree.mapPreOrder<String>{ node, transformedParentValue ->  "${node.value + 1 + (transformedParentValue?.toInt() ?: 0)}"}

        with (stringTree) {
            assertEquals("2", value)
            assertEquals(2, children.size)
            with(children[0]) {
                assertEquals("5", value)
                assertTrue(children.isEmpty())
            }
            with(children[1]) {
                assertEquals("6", value)
                assertEquals(1, children.size)
                with(children[0]) {
                    assertEquals("11", value)
                    assertTrue(children.isEmpty())
                }
            }
        }
    }

    @Test
    fun mapIndexed() {
        val indexedStringTreePreOrder = intTree.mapIndexedPreOrder<String> { node, index, _ ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth}"}
        println(indexedStringTreePreOrder)

        val indexedStringTreePostOrder = intTree.mapIndexedPostOrder<String> { node, index, _ ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth} Height=${index.height}"}
        println(indexedStringTreePostOrder)

        val levelsListsStringBreadthFirst = intTree.flatMapLevelsIndexed{index, node ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth} Width=${index.width}"}
        println(levelsListsStringBreadthFirst)
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
                //For leaves (no-children) add new child (11)
                true -> listOf(Tree(11))
                //For nodes with children, add new child having value + 1 of the last child
                false -> children + listOf(Tree(children.last().value + 1)) }
            }

        println(modifiedTree)

        with (modifiedTree) {
            assertEquals(1, value)
            assertEquals(3, children.size)
            with (children[0]) {
                assertEquals(2, value)
                assertEquals(1, children.size)
                with(children[0]) {
                    assertEquals(11, value)
                    assertTrue(children.isEmpty())
                }
            }
            with (children[1]) {
                assertEquals(3, value)
                assertEquals(2, children.size)
                with(children[0]) {
                    assertEquals(4, value)
                    assertEquals(1, children.size)
                    with(children[0]) {
                        assertEquals(11, value)
                        assertTrue(children.isEmpty())
                    }
                }
                with(children[1]) {
                    assertEquals(5, value)
                    assertTrue(children.isEmpty())
                }
            }
            with(children[2]) {
                assertEquals(4, value)
                assertTrue(children.isEmpty())
            }
        }
    }

}