package org.jto.extensions.treestreams

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ForestTest {
    val intForest = forestOf(
        treeOf(
            1,
            treeOf(2),
            treeOf(3, Tree(4))
        ),
        treeOf(5)
    )

    @Test
    fun basicTreeCharacteristics() {
        println(intForest)
        assertEquals(3, intForest.height())
        assertEquals(2, intForest.width())
        assertEquals(5, intForest.nodeCount())
        assertEquals(3, intForest.leafCount())
    }

    @Test
    fun mapPreOrderIntToString() {
        val stringForest = intForest.mapPreOrder<String> { node, _ -> "${node.value + 1}" }

        with(stringForest) {
            assertEquals(2, nodes.size)
            with(nodes[0]) {
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
            with(nodes[1]) {
                assertEquals("6", value)
                assertTrue(children.isEmpty())
            }
        }
    }


}