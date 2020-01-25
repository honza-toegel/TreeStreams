package org.jto.extensions.treestreams

import javafx.scene.control.TreeItem


fun <T> Iterable<T?>.filterNotNull(): List<T> {
    val out = mutableListOf<T>()
    forEach { it?.apply { out.add(it) } }
    return out
}

/**
 * BF breadth-first-traversal -> whole levels
 */
fun <T, R, I> mapForestBfLevelsIndexed(
    rootNodes: Iterable<T>,
    initialIndex: I,
    children: (T) -> Iterable<T>,
    levelTransform: (index: I, levelNodes: Iterable<T>) -> List<R>,
    levelUpIndexTransform: (index: I, levelNodes: Iterable<T>) -> I
): List<List<R>> {
    fun mapForestBsfLevelsRecursive(levelNodes: Iterable<T> = rootNodes, index: I): List<List<R>> =
        when (levelNodes.count()) {
            0 -> emptyList()
            else -> {
                //From root down to leaves
                listOf(levelTransform(index, levelNodes)) +
                        //Recursive for children
                        mapForestBsfLevelsRecursive(
                            levelNodes.flatMap(children),
                            levelUpIndexTransform(index, levelNodes)
                        )
            }
        }
    return mapForestBsfLevelsRecursive(rootNodes, initialIndex)
}

/**
 * BFS breadth-first-traversal -> node based
 */
fun <T, R, I> mapForestBfNodesIndexed(
    rootNodes: Iterable<T>,
    initialIndex: I,
    children: (T) -> Iterable<T>,
    nodeTransform: (index: I, node: T) -> R?,
    nextItemIndexTransform: (index: I, node: T) -> I,
    levelUpIndexTransform: (index: I, node: Iterable<T>) -> I
): List<List<R>> =
    mapForestBfLevelsIndexed(
        rootNodes, initialIndex, children,
        levelTransform = { levelIndex, levelNodes ->
            var nodeIndex = levelIndex
            levelNodes.map {
                val transformedNode = nodeTransform(nodeIndex, it)
                nodeIndex = nextItemIndexTransform(nodeIndex, it)
                transformedNode
            }.filterNotNull()
        }, levelUpIndexTransform = levelUpIndexTransform
    )


/**
 * Pre-order tree traversal transforming to new tree
 * -> pre-order-indexing => preOrderIndexTransform = {it + 1}
 * -> post-order-indexing => postOrderIndexTransform = {it + 1}
 */
fun <T, R, I> mapForestPreOrderIndexed(
    rootNodes: Iterable<T>,
    initialIndex: I,
    children: (T) -> Iterable<T>,
    preTransform: (item: T, index: I, parent: R?) -> R?,
    postAction: (item: R, index: I, children: List<R>) -> Unit = { _, _, _ -> },
    preOrderIndexTransform: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i },
    postOrderIndexTransform: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i }
): List<R> {

    var index = initialIndex
    fun mapTreeIndexedRecursive(originalParent: T?, originalItem: T, transformedParent: R?): R? {
        index = preOrderIndexTransform(index, originalItem, originalParent)
        //The index variable is given twice to the transformation function ->
        // - pre-order => before child processing,
        // - post-order => after child processing
        val transformedItem = preTransform(originalItem, index, transformedParent)
        if (transformedItem != null) {
            val transformedChildren =
                children(originalItem).map { mapTreeIndexedRecursive(originalItem, it, transformedItem) }
            postAction(transformedItem, index, transformedChildren.filterNotNull())
            index = postOrderIndexTransform(index, originalItem, originalParent)
        }
        return transformedItem
    }
    return rootNodes.map { mapTreeIndexedRecursive(null, it, null) }.filterNotNull()
}

/**
 * Post-order tree traversal transforming to new tree
 * -> pre-order-indexing => preOrderIndexTransform = {it + 1}
 * -> post-order-indexing => postOrderIndexTransform = {it + 1}
 */
fun <T, R, I> mapForestPostOrderIndexed(
    rootNodes: Iterable<T>,
    initialIndex: I,
    children: (T) -> Iterable<T>,
    transform: (item: T, preOrderIndex: I, children: List<R>, postOrderIndex: I) -> R?,
    preOrderIndexTransform: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i },
    postOrderIndexTransform: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i }
): List<R> {
    var index = initialIndex
    fun mapTreeIndexedRecursive(originalParent: T?, originalItem: T): R? {
        index = preOrderIndexTransform(index, originalItem, originalParent)
        //The index variable is given twice to the transformation function ->
        // - pre-order => before child processing,
        // - post-order => after child processing
        val transformedItem = transform(originalItem, index, children(originalItem).map {
            mapTreeIndexedRecursive(originalItem, it)
        }.filterNotNull(), index)
        index = postOrderIndexTransform(index, originalItem, originalParent)
        return transformedItem
    }
    return rootNodes.map { mapTreeIndexedRecursive(null, it) }.filterNotNull()
}

fun <T, R, I> mapTreeIndexed(
    rootItem: T,
    initialIndex: I,
    children: (T) -> Iterable<T>,
    transform: (item: T, preOrderIndex: I, children: List<R>, postOrderIndex: I) -> R?,
    preOrderIndexTransform: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i },
    postOrderIndexTransform: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i }
): R {

    return mapForestPostOrderIndexed(
        listOf(rootItem),
        initialIndex,
        children,
        transform,
        preOrderIndexTransform,
        postOrderIndexTransform
    ).single()
}

open class BaseForestStream<T>(val rootNodes: Iterable<T>) {
    open fun toStream(children: (T) -> Iterable<T>): GenericForestStream<T> = GenericForestStream(rootNodes, children)
    open fun toTreeStream(children: (T) -> Iterable<T>): GenericTreeStream<T> =
        GenericTreeStream(rootNodes.single(), children)

    open fun toTreeStream(): BaseTreeStream<T> = BaseTreeStream(rootNodes.single())
}

/**
 * Simple representation of tree index
 */
class DfIndex(val preOrderIndex: Int, val postOrderIndex: Int, val level: Int, val depth: Int)

class BfIndex(val nodeIndex: Int, val depth: Int, val width: Int)


open class GenericForestStream<T>(rootNodes: Iterable<T>, val children: (T) -> Iterable<T>) :
    BaseForestStream<T>(rootNodes) {


    /**
     * Pre order functions
     */
    open fun <R, I> mapForestPreOrderIndexed(
        initialIndex: I,
        preOrderTransform: (node: T, index: I, parent: R?) -> R?,
        postOrderAction: (node: R, index: I, children: List<R>) -> Unit = { _, _, _ -> },
        preOrderIndex: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i },
        postOrderIndex: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i }
    ): BaseForestStream<R> =
        BaseForestStream(
            mapForestPreOrderIndexed(
                rootNodes,
                initialIndex,
                children,
                preOrderTransform,
                postOrderAction,
                preOrderIndex,
                postOrderIndex
            )
        )

    fun <R> mapForestPreOrderIndexed(
        postOrderAction: (node: R, index: DfIndex, children: List<R>) -> Unit = { _, _, _ -> },
        transform: (node: T, index: DfIndex, parent: R?) -> R?
    ): BaseForestStream<R> =
        mapForestPreOrderIndexed(DfIndex(-1, 0, height(), -1), transform, postOrderAction,
            preOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex + 1,
                    index.postOrderIndex,
                    index.level - 1,
                    index.depth + 1
                )
            },
            postOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex,
                    index.postOrderIndex + 1,
                    index.level + 1,
                    index.depth - 1
                )
            })

    fun <R> mapForestPreOrder(
        postOrderAction: (node: R, children: List<R>) -> Unit = { _, _ -> },
        transform: (node: T, parent: R?) -> R?
    ): BaseForestStream<R> =
        mapForestPreOrderIndexed(
            0,
            { node, _, parent -> transform(node, parent) },
            { node, _, children -> postOrderAction(node, children) })

    fun forEachPreOrder(action: (T) -> Unit) = mapForestPreOrder<Unit> { node, _ -> action(node) }
    fun forEachPreOrderIndexed(action: (preOrderIndex: DfIndex, T) -> Unit) =
        mapForestPreOrderIndexed<Unit> { node, index, _ -> action(index, node) }

    /**
     * Post order functions
     */
    open fun <R, I> mapForestPostOrderIndexed(
        initialIndex: I,
        transform: (node: T, preOrderIndex: I, children: List<R>, postOrderIndex: I) -> R?,
        preOrderIndex: (traversalIndex: I, childNode: T, parentNode: T?) -> I = { i, _, _ -> i },
        postOrderIndex: (traversalIndex: I, childNode: T, parentNode: T?) -> I = { i, _, _ -> i }
    ): BaseForestStream<R> =
        BaseForestStream(
            mapForestPostOrderIndexed(
                rootNodes,
                initialIndex,
                children,
                transform,
                preOrderIndex,
                postOrderIndex
            )
        )

    fun <R> mapForestPostOrderIndexed(transform: (node: T, preOrderIndex: DfIndex, children: List<R>, postOrderIndex: DfIndex) -> R?): BaseForestStream<R> =
        mapForestPostOrderIndexed(DfIndex(-1, 0, height(), 0), transform,
            preOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex + 1,
                    index.postOrderIndex,
                    index.level - 1,
                    index.depth + 1
                )
            },
            postOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex,
                    index.postOrderIndex + 1,
                    index.level + 1,
                    index.depth - 1
                )
            })

    fun <R> mapForestPostOrder(transform: (node: T, children: List<R>) -> R?): BaseForestStream<R> =
        mapForestPostOrderIndexed(0, { node, _, children, _ -> transform(node, children) })

    fun forEachPostOrder(action: (T) -> Unit) = mapForestPostOrder<Unit> { node, _ -> action(node) }
    fun forEachPostOrderIndexed(action: (preOrderIndex: DfIndex, postOrderIndex: DfIndex, T) -> Unit) =
        mapForestPostOrderIndexed<Unit> { node, preOrderIndex, _, postOrderIndex ->
            action(
                preOrderIndex,
                postOrderIndex,
                node
            )
        }

    /**
     * Input:
     *      a0
     *     /  \
     *    a1  a4
     *   / \   \
     *  a2 a3  a5
     *
     * Input predicate (a2,a5)=> false (a0,a1,a3,a5)=>true
     *
     * Output:
     *
     *      a0
     *     /  \
     *    a1  a4
     *     \
     *     a3
     *
     * Post-order traversal, applying filter to all nodes,
     * - nodes which doesn't fulfill predicate are filtered out,
     * - so for example if the root node doesn't fulfill predicate its whole tree filtered out
     */
    fun filterForestPostOrder(
        filteredNodeFactory: (node: T, children: List<T>) -> T,
        predicate: (node: T, children: List<T>) -> Boolean
    ): GenericForestStream<T> =
        mapForestPostOrder<T> { node, children ->
            when (predicate(node, children)) {
                false -> null //Predicate is not fulfilled, node is filtered out
                true -> filteredNodeFactory(
                    node,
                    children
                ) //Predicate is fulfilled, node is returned including children
            }
        }.toStream(children)

    fun filterForestPostOrder(
        filteredNodeFactory: (node: T, children: List<T>) -> T,
        predicate: (node: T) -> Boolean
    ): GenericForestStream<T> = filterForestPostOrder(filteredNodeFactory, { node, _ -> predicate(node) })

    fun filterForestPreOrder(
        filteredNodeFactory: (node: T) -> T,
        postOrderAction: (node: T, children: List<T>) -> Unit = { _, _ -> },
        predicate: (node: T, parent: T?) -> Boolean
    ): GenericForestStream<T> =
        mapForestPreOrder<T>(transform = { node, parent ->
            when (predicate(node, parent)) {
                false -> null //Predicate is not fulfilled, node is filtered out
                true -> filteredNodeFactory(node) //Predicate is fulfilled, node is returned including children
            }
        }, postOrderAction = { node, children -> postOrderAction(node, children) }).toStream(children)

    fun filterForestPreOrder(
        filteredNodeFactory: (node: T) -> T,
        predicate: (node: T, parent: T?) -> Boolean
    ): GenericForestStream<T> = filterForestPreOrder(filteredNodeFactory, predicate = predicate)


    /**
     * Input:
     *      a0
     *     /  \
     *    a1  a4
     *   / \   \
     *  a2 a3  a5
     *
     * Input predicate (a0,a1,a2,a5)=> false (a3,a5)=>true
     *
     * Output:
     *
     *      a0
     *     /  \
     *    a1  a4
     *     \
     *     a3
     *
     * Post-order traversal, applying filter to all nodes,
     * - subtrees which doesn't have any children satisfying the predicate are filtered out
     */
    fun filterForestByLeavesPostOrder(
        filteredNodeFactory: (node: T, children: List<T>) -> T,
        predicate: (node: T) -> Boolean
    ): GenericForestStream<T> =
        filterForestPostOrder(filteredNodeFactory, { node, children -> children.isEmpty() && predicate(node) })

    /**
     * Transform tree into flat list (BF-traverse)
     * Input:
     *   a
     *  / |
     * b1  b2
     * \
     * c1
     * Result:
     * listOf(a,b1,b2,c1)
     */
    fun mapForestBf(): List<List<T>> = mapForestBf { it }

    fun flatMapForestBf(): List<T> = mapForestBf().flatten()

    fun <R> mapForestBf(transform: (node: T) -> R): List<List<R>> = mapForestBfIndexed { _, node -> transform(node) }
    fun <R> flatMapForestBf(transform: (node: T) -> R) = mapForestBf(transform).flatten()

    fun <R> mapForestBfIndexed(transform: (index: BfIndex, node: T) -> R): List<List<R>> =
        mapForestBfNodesIndexed(rootNodes, BfIndex(0, 0, 0), children, transform,
            nextItemIndexTransform = { index, _ -> BfIndex(index.nodeIndex + 1, index.depth, index.width + 1) },
            levelUpIndexTransform = { index, _ -> BfIndex(index.nodeIndex + 1, index.depth + 1, 0) })

    fun <R> flatMapForestBfIndexed(transform: (index: BfIndex, node: T) -> R): List<R> =
        mapForestBfIndexed(transform).flatten()

    fun <R> mapForestBfLevelIndexed(transform: (depth: Int, levelNodes: Iterable<T>) -> List<R>): List<List<R>> =
        mapForestBfLevelsIndexed(rootNodes, 0, children, transform, { index, _ -> index + 1 })

    fun <R> flatMapForestBfLevelIndexed(transform: (depth: Int, levelNodes: Iterable<T>) -> List<R>): List<R> =
        mapForestBfLevelIndexed(transform).flatten()

    fun forEachBf(action: (T) -> Unit) = flatMapForestBf { action(it) }
    fun forEachBfIndexed(action: (index: BfIndex, T) -> Unit) = flatMapForestBfIndexed(action)
    fun forEachLevelBf(action: (Iterable<T>) -> Unit) =
        flatMapForestBfLevelIndexed { _, levelNodes -> listOf(action(levelNodes)) }

    fun forEachLevelBfIndexed(action: (depth: Int, Iterable<T>) -> Unit) =
        flatMapForestBfLevelIndexed { depth, levelNodes -> listOf(action(depth, levelNodes)) }

    /**
     * Post-order
     */
    fun <R> flatMapForestPostOrderIndexed(transform: (node: T, preOrderIndex: DfIndex, children: List<R>, postOrderIndex: DfIndex) -> R?): List<R> {
        val flattenedResult = mutableListOf<R>()
        mapForestPostOrderIndexed<R> { node, preOrderIndex, children, postOrderIndex ->
            transform(node, preOrderIndex, children, postOrderIndex)?.apply { flattenedResult.add(this) }
        }
        return flattenedResult
    }


    /**
     * Input:
     *   a
     *  / |
     * b1  b2
     * \
     * c1
     * Result:
     * (a to 0), (b1 to 1) (b2 to 1) (c1 to 3)
     * Approach:
     *  - DFS tree traverse with indexing of depth
     */
    fun nodeDepths(): List<Pair<T, Int>> = flatMapForestBfIndexed { index, t -> t to index.depth }

    fun nodeDepthsMap(): Map<T, Int> = nodeDepths().toMap()
    fun leafDepths(): List<Pair<T, Int>> = nodeDepths().filter { children(it.first).count() == 0 }
    fun leafDepthsMap(): Map<T, Int> = leafDepths().toMap()
    fun height(): Int = nodeDepths().maxBy { it.second }?.second?.plus(1) ?: 0
    fun nodeCount(): Int = flatMapForestBf().count()
    fun leafCount(): Int = flatMapForestBf().filter { children(it).count() == 0 }.count()
}

open class BaseTreeStream<T>(val rootNode: T) {
    open fun toStream(children: (T) -> Iterable<T>): GenericTreeStream<T> = GenericTreeStream(rootNode, children)
    open fun toForestStream(children: (T) -> Iterable<T>): GenericForestStream<T> =
        GenericForestStream(listOf(rootNode), children)

    open fun toForestStream(): BaseForestStream<T> = BaseForestStream(listOf(rootNode))
}

open class GenericTreeStream<T>(rootNode: T, val children: (T) -> Iterable<T>) : BaseTreeStream<T>(rootNode) {

    /**
     * Delegation pattern to reuse ForestStream
     */
    protected val forestStream = GenericForestStream<T>(listOf(rootNode), children)


    /**
     * Pre order functions
     */
    open fun <R, I> mapTreePreOrderIndexed(
        initialIndex: I,
        preOrderTransform: (node: T, index: I, parent: R?) -> R?,
        postOrderAction: (node: R, index: I, children: List<R>) -> Unit = { _, _, _ -> },
        preOrderIndex: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i },
        postOrderIndex: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i }
    ): BaseTreeStream<R> =
        forestStream.mapForestPreOrderIndexed(
            initialIndex,
            preOrderTransform,
            postOrderAction,
            preOrderIndex,
            postOrderIndex
        ).toTreeStream()

    fun <R> mapTreePreOrderIndexed(
        postOrderAction: (node: R, index: DfIndex, children: List<R>) -> Unit = { _, _, _ -> },
        transform: (node: T, index: DfIndex, parent: R?) -> R?
    ): BaseTreeStream<R> =
        mapTreePreOrderIndexed(DfIndex(-1, 0, height(), -1), transform, postOrderAction,
            preOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex + 1,
                    index.postOrderIndex,
                    index.level - 1,
                    index.depth + 1
                )
            },
            postOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex,
                    index.postOrderIndex + 1,
                    index.level + 1,
                    index.depth - 1
                )
            })

    fun <R> mapTreePreOrder(
        postOrderAction: (node: R, children: List<R>) -> Unit = { _, _ -> },
        transform: (node: T, parent: R?) -> R?
    ): BaseTreeStream<R> =
        mapTreePreOrderIndexed(
            0,
            { node, _, parent -> transform(node, parent) },
            { node, _, children -> postOrderAction(node, children) })

    fun forEachPreOrder(action: (T) -> Unit) = mapTreePreOrder<Unit> { node, _ -> action(node) }
    fun forEachPreOrderIndexed(action: (preOrderIndex: DfIndex, T) -> Unit) =
        mapTreePreOrderIndexed<Unit> { node, index, _ -> action(index, node) }


    /**
     * Post-order functions
     */
    open fun <R, I> mapTreePostOrderIndexed(
        initialIndex: I,
        transform: (node: T, preOrderIndex: I, children: List<R>, postOrderIndex: I) -> R?,
        preOrderIndex: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i },
        postOrderIndex: (traversalIndex: I, child: T, parent: T?) -> I = { i, _, _ -> i }
    ): BaseTreeStream<R> =
        forestStream.mapForestPostOrderIndexed(initialIndex, transform, preOrderIndex, postOrderIndex).toTreeStream()

    fun <R> mapTreePostOrderIndexed(transform: (node: T, preOrderIndex: DfIndex, children: List<R>, postOrderIndex: DfIndex) -> R?): BaseTreeStream<R> =
        mapTreePostOrderIndexed(DfIndex(-1, 0, height(), 0), transform,
            preOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex + 1,
                    index.postOrderIndex,
                    index.level - 1,
                    index.depth + 1
                )
            },
            postOrderIndex = { index, _, _ ->
                DfIndex(
                    index.preOrderIndex,
                    index.postOrderIndex + 1,
                    index.level + 1,
                    index.depth - 1
                )
            })


    fun <R> mapTreePostOrder(transform: (node: T, children: List<R>) -> R?): BaseTreeStream<R> =
        forestStream.mapForestPostOrder(transform).toTreeStream()

    fun forEachPostOrder(action: (T) -> Unit) = mapTreePostOrder<Unit> { node, _ -> action(node) }
    fun forEachPostOrderIndexed(action: (preOrderIndex: DfIndex, postOrderIndex: DfIndex, T) -> Unit) =
        mapTreePostOrderIndexed<Unit> { node, preOrderIndex, _, postOrderIndex ->
            action(
                preOrderIndex,
                postOrderIndex,
                node
            )
        }

    /**
     * Input:
     *      a0
     *     /  \
     *    a1  a4
     *   / \   \
     *  a2 a3  a5
     *
     * Input predicate (a2,a5)=> false (a0,a1,a3,a5)=>true
     *
     * Output:
     *
     *      a0
     *     /  \
     *    a1  a4
     *     \
     *     a3
     *
     * Post-order traversal, applying filter to all nodes,
     * - nodes which doesn't fulfill predicate are filtered out,
     * - so for example if the root node doesn't fulfill predicate its whole tree filtered out
     */
    fun filterTreePostOrder(
        filteredNodeFactory: (node: T, children: List<T>) -> T,
        predicate: (node: T, children: List<T>) -> Boolean
    ): GenericTreeStream<T> =
        mapTreePostOrder<T> { node, children ->
            when (predicate(node, children)) {
                false -> null //Predicate is not fulfilled, node is filtered out
                true -> filteredNodeFactory(
                    node,
                    children
                ) //Predicate is fulfilled, node is returned including children
            }
        }.toStream(children)

    fun filterTreePostOrder(
        filteredNodeFactory: (node: T, children: List<T>) -> T,
        predicate: (node: T) -> Boolean
    ): GenericTreeStream<T> = filterTreePostOrder(filteredNodeFactory, { node, _ -> predicate(node) })

    fun filterTreePreOrder(
        filteredNodeFactory: (node: T) -> T,
        postOrderAction: (node: T, children: List<T>) -> Unit = { _, _ -> },
        predicate: (node: T, parent: T?) -> Boolean
    ): GenericTreeStream<T> =
        mapTreePreOrder<T>(transform = { node, parent ->
            when (predicate(node, parent)) {
                false -> null //Predicate is not fulfilled, node is filtered out
                true -> filteredNodeFactory(node) //Predicate is fulfilled, node is returned including children
            }
        }, postOrderAction = { node, children -> postOrderAction(node, children) }).toStream(children)

    fun filterTreePreOrder(
        filteredNodeFactory: (node: T) -> T,
        predicate: (node: T, parent: T?) -> Boolean
    ): GenericTreeStream<T> = filterTreePreOrder(filteredNodeFactory, predicate = predicate)


    /**
     * Input:
     *      a0
     *     /  \
     *    a1  a4
     *   / \   \
     *  a2 a3  a5
     *
     * Input predicate (a0,a1,a2,a5)=> false (a3,a5)=>true
     *
     * Output:
     *
     *      a0
     *     /  \
     *    a1  a4
     *     \
     *     a3
     *
     * Post-order traversal, applying filter to all nodes,
     * - subtrees which doesn't have any children satisfying the predicate are filtered out
     */
    fun filterTreeByLeavesPostOrder(
        filteredNodeFactory: (node: T, children: List<T>) -> T,
        predicate: (node: T) -> Boolean
    ): GenericTreeStream<T> =
        filterTreePostOrder(filteredNodeFactory, { node, children -> children.isEmpty() && predicate(node) })

    /**
     * Transform tree into flat list (BF-traverse)
     * Input:
     *   a
     *  / |
     * b1  b2
     * \
     * c1
     * Result:
     * listOf(a,b1,b2,c1)
     */
    fun mapTreeBf(): List<List<T>> = mapTreeBf { it }

    fun flatMapTreeBf(): List<T> = mapTreeBf().flatten()

    fun <R> mapTreeBf(transform: (node: T) -> R): List<List<R>> = mapTreeBfIndexed { _, node -> transform(node) }
    fun <R> flatMapTreeBf(transform: (node: T) -> R) = mapTreeBf(transform).flatten()

    fun <R> mapTreeBfIndexed(transform: (index: BfIndex, node: T) -> R): List<List<R>> =
        forestStream.mapForestBfIndexed(transform)

    fun <R> flatMapTreeBfIndexed(transform: (index: BfIndex, node: T) -> R): List<R> =
        mapTreeBfIndexed(transform).flatten()

    fun <R> mapTreeBfLevelIndexed(transform: (depth: Int, levelNodes: Iterable<T>) -> List<R>): List<List<R>> =
        forestStream.mapForestBfLevelIndexed(transform)

    fun <R> flatMapTreeBfLevelIndexed(transform: (depth: Int, levelNodes: Iterable<T>) -> List<R>): List<R> =
        mapTreeBfLevelIndexed(transform).flatten()

    fun forEachBf(action: (T) -> Unit) = flatMapTreeBf { action(it) }
    fun forEachBfIndexed(action: (index: BfIndex, T) -> Unit) = flatMapTreeBfIndexed(action)
    fun forEachLevelBf(action: (Iterable<T>) -> Unit) =
        flatMapTreeBfLevelIndexed { _, levelNodes -> listOf(action(levelNodes)) }

    fun forEachLevelBfIndexed(action: (depth: Int, Iterable<T>) -> Unit) =
        flatMapTreeBfLevelIndexed { depth, levelNodes -> listOf(action(depth, levelNodes)) }

    /**
     * Input:
     *   a
     *  / |
     * b1  b2
     * \
     * c1
     * Result:
     * (a to 0), (b1 to 1) (b2 to 1) (c1 to 3)
     * Approach:
     *  - DFS tree traverse with indexing of depth
     */
    fun nodeDepths(): List<Pair<T, Int>> = forestStream.nodeDepths()

    fun nodeDepthsMap(): Map<T, Int> = nodeDepths().toMap()
    fun leafDepths(): List<Pair<T, Int>> = forestStream.leafDepths()
    fun leafDepthsMap(): Map<T, Int> = leafDepths().toMap()
    fun height(): Int = forestStream.height()
    fun nodeCount(): Int = forestStream.nodeCount()
    fun leafCount(): Int = forestStream.leafCount()
}

class TreeItemForestStream<T>(rootNodes: Iterable<TreeItem<T>>) :
    GenericForestStream<TreeItem<T>>(rootNodes, { it.children }) {
    constructor(rootItem: TreeItem<T>) : this(listOf(rootItem))

    fun <R, I> mapForestIndexed(
        initialIndex: I,
        transform: (node: TreeItem<T>, preOrderIndex: I, children: List<TreeItem<R>>, postOrderIndex: I) -> TreeItem<R>,
        preOrderIndex: (traversalIndex: I, child: TreeItem<T>, parent: TreeItem<T>?) -> I,
        postOrderIndex: (traversalIndex: I, child: TreeItem<T>, parent: TreeItem<T>?) -> I
    ): TreeItemForestStream<R> =
        TreeItemForestStream(
            super.mapForestPostOrderIndexed<TreeItem<R>, I>(
                initialIndex,
                { node, preOrderIndex, children, postOrderIndex ->
                    transform(
                        node,
                        preOrderIndex,
                        children,
                        postOrderIndex
                    ).also { it.children.addAll(children) }
                },
                preOrderIndex,
                postOrderIndex
            ).rootNodes
        )

    /**
     * TreeItem<T> ->> TreeItem<R>
     */
    fun <R> mapForestIndexedPreOrder(transform: (node: TreeItem<T>, preOrderIndex: Int, children: List<TreeItem<R>>) -> TreeItem<R>): TreeItemForestStream<R> =
        mapForestIndexed(
            -1,
            { node, preOrderIndex, children, _ -> transform(node, preOrderIndex, children) },
            { i, _, _ -> i + 1 },
            { i, _, _ -> i })

    fun <R> mapForestIndexedPostOrder(transform: (node: TreeItem<T>, preOrderIndex: Int, children: List<TreeItem<R>>) -> TreeItem<R>): TreeItemForestStream<R> =
        mapForestIndexed(
            0,
            { node, preOrderIndex, children, _ -> transform(node, preOrderIndex, children) },
            { i, _, _ -> i },
            { i, _, _ -> i + 1 })

    fun <R> mapForest(transform: (node: TreeItem<T>, children: List<TreeItem<R>>) -> TreeItem<R>): TreeItemForestStream<R> =
        mapForestIndexed(
            0,
            { node, _, children, _ -> transform(node, children) },
            preOrderIndex = { i, _, _ -> i },
            postOrderIndex = { i, _, _ -> i })
}

