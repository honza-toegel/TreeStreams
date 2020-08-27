package org.jto.extensions.treestreams

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class MutableForestTest {
    val mutableIntForest = mutableForestOf(
            mutableTreeOf(1),
            mutableTreeOf(2,
                mutableTreeOf(3),
                mutableTreeOf(4)
            )
    )

    fun changeMutableTree() {
        mutableIntForest.mutableNodes[1].mutableChildren[1].mutableChildren.add(mutableTreeOf(5))
        mutableIntForest.mutableNodes.add(mutableTreeOf(6))
    }

    @Test
    fun basicTreeCharacteristics() {
        println("Before:")
        println(mutableIntForest)
        assertEquals(2, mutableIntForest.height())
        assertEquals(2, mutableIntForest.width())
        assertEquals(4, mutableIntForest.nodeCount())
        assertEquals(3, mutableIntForest.leafCount())

        changeMutableTree()

        println("After:")
        println(mutableIntForest)
        assertEquals(3, mutableIntForest.height())
        assertEquals(3, mutableIntForest.width())
        assertEquals(6, mutableIntForest.nodeCount())
        assertEquals(4, mutableIntForest.leafCount())
    }
}