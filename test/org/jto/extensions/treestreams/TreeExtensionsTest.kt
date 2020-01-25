package org.jto.extensions.treestreams

import javafx.scene.control.TreeItem
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class TreeExtensionsTest {

    open class PreOrderTreeItemBaseTest(val parent: PreOrderTreeItemBaseTest?) {
        val children: List<PreOrderTreeItemBaseTest> = mutableListOf()

        init {
            parent?.addChild(this)
        }

        private fun addChild(child: PreOrderTreeItemBaseTest) = (children as MutableList).add(child)
    }

    class PreOrderTreeItemTest(val secondParamSum: Int, val parentItemsCount: Int, parent: PreOrderTreeItemTest?) : PreOrderTreeItemBaseTest(parent)

    @Test
    fun mapForestPreOrderTest() {
        val a1 = TreeItem("A1" to 2).apply { children.addAll(TreeItem("1B1" to 3), TreeItem("1B2" to 1)) }
        val a0 = TreeItem("A0" to 1).apply { children.addAll(TreeItem("0B1" to 2)) }
        val a2 = TreeItem("A2" to 3).apply { children.addAll(TreeItem("2B1" to 1), TreeItem("2B2" to 1), TreeItem("2B3" to 1)) }
        val rootChildren = listOf<TreeItem<Pair<String, Int>>>(a1, a0, a2)

        val resultTree = GenericForestStream<TreeItem<Pair<String, Int>>>(rootChildren) { it.children }.mapForestPreOrder<PreOrderTreeItemTest>
        { node, parent -> PreOrderTreeItemTest(
                    parentItemsCount = (parent?.parentItemsCount?.plus(1)) ?: 0,
                    secondParamSum = node.value.second + (parent?.secondParamSum ?: 0),
                    parent = parent) }

        with(resultTree.rootNodes.first()) {
            assertEquals(0, parentItemsCount)
            assertEquals(2, secondParamSum)
            with(children[0] as PreOrderTreeItemTest) {
                assertEquals(1, parentItemsCount)
                assertEquals(5, secondParamSum)
            }
            with(children[1] as PreOrderTreeItemTest) {
                assertEquals(1, parentItemsCount)
                assertEquals(3, secondParamSum)
            }
        }
        with(resultTree.rootNodes.last()) {
            assertEquals(0, parentItemsCount)
            assertEquals(3, secondParamSum)
        }
    }



    class PreOrderTreeItemIndexTest(val preOrderIndex:DfIndex, parent: PreOrderTreeItemIndexTest?) : PreOrderTreeItemBaseTest(parent) {
        var postOrderIndex:DfIndex = DfIndex(0,0,0,0)
    }

    @Test
    fun mapForestPreOrderIndexedTest() {
        val a0 = TreeItem("A0" to 2).apply { children.addAll(TreeItem("1B1" to 3), TreeItem("1B2" to 1)) }
        val a1 = TreeItem("A1" to 1).apply { children.addAll(TreeItem("0B1" to 2)) }
        val a2 = TreeItem("A2" to 3).apply { children.addAll(TreeItem("2B1" to 1).apply { children.add(TreeItem("CC5" to 1)) }, TreeItem("2B2" to 2), TreeItem("2B3" to 1)) }
        val rootChildren = listOf<TreeItem<Pair<String, Int>>>(a0, a1, a2)

        val resultTree = GenericForestStream<TreeItem<Pair<String, Int>>>(rootChildren) { it.children }.mapForestPreOrderIndexed<PreOrderTreeItemIndexTest> (
                postOrderAction = {node, index, _ -> node.postOrderIndex = index },
                transform = { _, index, parent -> PreOrderTreeItemIndexTest(index, parent) })

        with(resultTree.rootNodes.first()) {
            assertEquals(0, preOrderIndex.depth)
            assertEquals(2, preOrderIndex.level)
            assertEquals( 0, preOrderIndex.preOrderIndex )
            assertEquals( 2, postOrderIndex.postOrderIndex )
            with(children[0] as PreOrderTreeItemIndexTest) {
                assertEquals(1, preOrderIndex.depth)
                assertEquals(1, preOrderIndex.level)
                assertEquals( 1, preOrderIndex.preOrderIndex )
                assertEquals( 0, postOrderIndex.postOrderIndex )
            }
            with(children[1] as PreOrderTreeItemIndexTest) {
                assertEquals(1, preOrderIndex.depth)
                assertEquals(1, preOrderIndex.level)
                assertEquals( 2, preOrderIndex.preOrderIndex )
                assertEquals( 1, postOrderIndex.postOrderIndex )
            }
        }
        with(resultTree.rootNodes.last()) {
            assertEquals(0, preOrderIndex.depth)
            assertEquals(2, preOrderIndex.level)
            assertEquals( 5, preOrderIndex.preOrderIndex )
            assertEquals( 9, postOrderIndex.postOrderIndex )
            with(children[0]  as PreOrderTreeItemIndexTest) {
                assertEquals(1, preOrderIndex.depth)
                assertEquals(1, preOrderIndex.level)
                assertEquals( 6, preOrderIndex.preOrderIndex )
                assertEquals( 6, postOrderIndex.postOrderIndex )
                with(children[0]  as PreOrderTreeItemIndexTest) {
                    assertEquals(2, preOrderIndex.depth)
                    assertEquals(0, preOrderIndex.level)
                    assertEquals( 7, preOrderIndex.preOrderIndex )
                    assertEquals( 5, postOrderIndex.postOrderIndex )
                }
            }
            with(children[1]  as PreOrderTreeItemIndexTest) {
                assertEquals(1, preOrderIndex.depth)
                assertEquals(1, preOrderIndex.level)
                assertEquals( 8, preOrderIndex.preOrderIndex )
                assertEquals( 7, postOrderIndex.postOrderIndex )
            }
        }
    }


    class TreeItemTest(val colIndex: Int, val leavesCount: Int, val secondParamSum: Int, val children: List<TreeItemTest>)


    @Test
    fun mapTreePostOrderIndexedTest() {
        val a1 = TreeItem("A1" to 2).apply { children.addAll(TreeItem("1B1" to 1), TreeItem("1B2" to 1)) }
        val a0 = TreeItem("A0" to 1).apply { children.addAll(TreeItem("0B1" to 2)) }
        val a2 = TreeItem("A2" to 3).apply { children.addAll(TreeItem("2B1" to 1), TreeItem("2B2" to 1), TreeItem("2B3" to 1)) }
        val rootChildren = listOf<TreeItem<Pair<String, Int>>>(a1, a0, a2)
        val a00 = TreeItem("00" to 0).apply { children.addAll(rootChildren) }

        val resultTree = GenericTreeStream<TreeItem<Pair<String, Int>>>(a00, { it.children }).mapTreePostOrderIndexed<TreeItemTest, Int>(
                transform = { it, _, children, postOrderIndex ->
                    TreeItemTest(colIndex = postOrderIndex,
                            leavesCount = maxOf(children.map { it.leavesCount }.sum(), 1),
                            secondParamSum = when (children.isEmpty()) {
                                true -> it.value.second; false -> children.map { it.secondParamSum }.sum()
                            },
                            children = children)
                }, initialIndex = 0, postOrderIndex = { it, _, _ -> it + 1 })


        with(resultTree.rootNode) {
            assertEquals(6, leavesCount)
            assertEquals(7, secondParamSum)
            with(children[0]) {
                assertEquals(2, leavesCount)
                assertEquals(2, secondParamSum)
                with(children[0]) {
                    assertEquals(1, leavesCount)
                    assertEquals(1, secondParamSum)
                }
                with(children[1]) {
                    assertEquals(1, leavesCount)
                    assertEquals(1, secondParamSum)
                }
            }
            with(children[1]) {
                assertEquals(1, leavesCount)
                assertEquals(2, secondParamSum)
            }
        }
    }

    @Test
    fun mapTreePostOrderIndexedTest2() {
        val a1 = TreeItem("A1" to 2).apply { children.addAll(TreeItem("1B1" to 1), TreeItem("1B2" to 1)) }
        val a0 = TreeItem("A0" to 1).apply { children.addAll(TreeItem("0B1" to 1)) }
        val a2 = TreeItem("A2" to 3).apply { children.addAll(TreeItem("2B1" to 1), TreeItem("2B2" to 1), TreeItem("2B3" to 1)) }
        val rootChildren = listOf<TreeItem<Pair<String, Int>>>(a1, a0, a2)
        val a00 = TreeItem("00" to 0).apply { children.addAll(rootChildren) }

        val resultTree = GenericTreeStream(a00, { it.children })
                .mapTreePostOrder<TreeItem<String>> { item, children -> TreeItem(item.value.first).apply { this.children.addAll(children) } }
                .toStream { it.children }
                .mapTreePostOrderIndexed<TreeItem<Pair<String, Int>>> { item, preOrderIndex, children, _ -> TreeItem(item.value to preOrderIndex.preOrderIndex).apply { this.children.addAll(children) } }

        with(resultTree.rootNode) {
            assertEquals("00", value.first)
            assertEquals(0, value.second)
            assertEquals(3, children.size)
            with(children[0]) {
                assertEquals("A1", value.first)
                assertEquals(1, value.second)
                assertEquals(2, children.size)
                with(children[0]) {
                    assertEquals("1B1", value.first)
                    assertEquals(2, value.second)
                }
                with(children[1]) {
                    assertEquals("1B2", value.first)
                    assertEquals(3, value.second)
                }
            }
            with(children[1]) {
                assertEquals("A0", value.first)
                assertEquals(4, value.second)
            }
        }
    }

    @Test
    fun mapForestIndexedPreOrder() {
        val b2 = TreeItem("1B2" to 1)
        val a1 = TreeItem("A1" to 2).apply { children.addAll(TreeItem("1B1" to 1), b2) }
        val a0 = TreeItem("A0" to 1).apply { children.addAll(TreeItem("0B1" to 1)) }
        val a2 = TreeItem("A2" to 3).apply { children.addAll(TreeItem("2B1" to 1), TreeItem("2B2" to 1), TreeItem("2B3" to 1)) }
        val rootChildren = listOf<TreeItem<Pair<String, Int>>>(a1, a0, a2)
        val a00 = TreeItem("00" to 0).apply { children.addAll(rootChildren) }

        val resultTree = TreeItemForestStream(a00)
                .mapForest<String> { item, _ -> TreeItem(item.value.first) }
                .mapForestIndexedPreOrder<Pair<String, Int>> { item, preOrderIndex, _ -> TreeItem(item.value to preOrderIndex) }

        assertEquals(3, resultTree.height())
        with(resultTree.rootNodes.first()) {
            assertEquals(0, resultTree.nodeDepthsMap()[this])
            assertEquals("00", value.first)
            assertEquals(0, value.second)
            assertEquals(3, children.size)
            with(children[0]) {
                assertEquals(1, resultTree.nodeDepthsMap()[this])
                assertEquals("A1", value.first)
                assertEquals(1, value.second)
                assertEquals(2, children.size)
                with(children[0]) {
                    assertEquals(2, resultTree.nodeDepthsMap()[this])
                    assertEquals("1B1", value.first)
                    assertEquals(2, value.second)
                }
                with(children[1]) {
                    assertEquals(2, resultTree.nodeDepthsMap()[this])
                    assertEquals("1B2", value.first)
                    assertEquals(3, value.second)
                }
            }
            with(children[1]) {
                assertEquals(1, resultTree.nodeDepthsMap()[this])
                assertEquals("A0", value.first)
                assertEquals(4, value.second)
            }
        }
    }

}