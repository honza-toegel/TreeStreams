package org.jto.extensions.treestreams

import java.lang.StringBuilder

open class Tree<T>(
    val value: T,
    val children: List<Tree<T>> = emptyList()
) {

    /**
     * Map Tree<T> -> Tree<R>, keeping tree structure, pre-order
     */
    fun <R> mapPreOrder(transform: (node: Tree<T>, transformedParentValue: R?) -> R): Tree<R> =
        mapPreOrder(listOf(this), transform).single()

    /**
     * Map Tree<T> -> Tree<R>, keeping tree structure, pre-order indexed
     */
    private fun <R, I> mapIndexedPreOrder(
        initialIndex: I,
        transform: (node: Tree<T>, index: I, transformedParentValue: R?) -> R,
        preOrderIndexTransform: (traversalIndex: I, child: Tree<T>, transformedParentValue: R?) -> I,
        postOrderIndexTransform: (traversalIndex: I, child: Tree<T>, transformedNodeValue: R, transformedParentValue: R?) -> I
    ): Tree<R> =
        mapIndexedPreOrder(
            listOf(this),
            initialIndex,
            transform,
            preOrderIndexTransform,
            postOrderIndexTransform
        ).single()

    /**
     * Map Tree<T> -> Tree<R>, keeping tree structure, pre-order indexed
     */
    fun <R> mapIndexedPreOrder(
        transform: (node: Tree<T>, index: PreOrderIndex, transformedParentValue: R?) -> R
    ): Tree<R> =
        mapIndexedPreOrder(
            PreOrderIndex(-1, -1), transform,
            preOrderIndexTransform = { index, _, _ ->
                PreOrderIndex(
                    index.nodeIndex + 1,
                    index.depth + 1
                )
            },
            postOrderIndexTransform = { index, _, _, _ ->
                PreOrderIndex(
                    index.nodeIndex,
                    index.depth - 1
                )
            })

    /**
     * ForEach on Tree<T> pre-order
     */
    fun forEachIndexedPreOrder(action: (node: Tree<T>, index: PreOrderIndex) -> Unit) =
        mapIndexedPreOrder<Unit> { node, index, _ -> action(node, index) }

    /**
     * Map Tree<T> -> Tree<R>, keeping tree structure, post-order
     */
    fun <R> mapPostOrder(transform: (node: Tree<T>, transformedChildren: List<Tree<R>>) -> R): Tree<R> =
        mapPostOrder(listOf(this), transform).single()

    /**
     * Map Tree<T> -> Tree<R>, keeping tree structure, post-order indexed
     */
    private fun <R, I> mapIndexedPostOrder(
        initialIndex: I,
        transform: (node: Tree<T>, index: I, transformedChildren: List<Tree<R>>) -> R,
        preOrderIndex: (traversalIndex: I, node: Tree<T>) -> I,
        postOrderIndex: (traversalIndex: I, node: Tree<T>) -> I
    ): Tree<R> = mapIndexedPostOrder(listOf(this), initialIndex, transform, preOrderIndex, postOrderIndex).single()

    /**
     * Map Tree<T> -> Tree<R>, keeping tree structure, post-order indexed
     */
    fun <R> mapIndexedPostOrder(transform: (node: Tree<T>, index: PostOrderIndex, transformedChildren: List<Tree<R>>) -> R) =
        mapIndexedPostOrder(PostOrderIndex(-1, height() - 1, 0), transform,
            preOrderIndex = { index, _ ->
                PostOrderIndex(
                    index.nodeIndex,
                    0,
                    index.depth + 1
                )
            },
            postOrderIndex = { index, node ->
                PostOrderIndex(
                    index.nodeIndex + 1,
                    when (node.children.isEmpty()) {
                        true -> 0
                        false -> index.height + 1
                    },
                    index.depth - 1
                )
            })

    /**
     * ForEach on Tree<T> post-order
     */
    fun forEachIndexedPostOrder(action: (node: Tree<T>, index: PostOrderIndex) -> Unit) =
        mapIndexedPostOrder<Unit> { node, index, _ -> action(node, index) }


    /**
     * ******** MODIFY TREE<T> STRUCTURE, KEEP TREE ITEM TYPE ***************
     */

    /**
     * Change tree structure (add/remove children of tree nodes)
     */
    fun modifyChildrenPostOrder(modifyChildren: (node: Tree<T>, children: List<Tree<T>>) -> List<Tree<T>>): Tree<T> {
        fun modifyChildrenPostOrderRecursive(node: Tree<T>): Tree<T> =
            Tree<T>(node.value, modifyChildren(node, node.children.map { modifyChildrenPostOrderRecursive(it) }))
        return modifyChildrenPostOrderRecursive(this)
    }

    /**
     * Filter tree by criteria<C> which is calculated for each node (in post-order manner) and then evaluated.
     */
    fun <C> filterByCriteriaPostOrder(
        calculateCriteriaValue: (node: Tree<T>, childrenCriteriaValues: List<C>) -> C,
        evaluateCriteria: (criteriaValue: C) -> Boolean
    ): Tree<T> {
        fun filterByCriteriaPostOrderRecursive(node: Tree<Pair<T, C>>): Tree<T> =
            Tree<T>(
                node.value.first,
                node.children.filter { evaluateCriteria(it.value.second) }.map { filterByCriteriaPostOrderRecursive(it) })

        val treeEnrichedByCriteriaValue = mapPostOrder<Pair<T, C>> { node, transformedChildren ->
            Pair(
                node.value,
                calculateCriteriaValue(node, transformedChildren.map { it.value.second })
            )
        }
        return filterByCriteriaPostOrderRecursive(treeEnrichedByCriteriaValue)
    }

    /**
     * Filter tree by criteria<C> which is calculated for each node (in pre-order manner) and then evaluated.
     */
    fun <C> filterByCriteriaPreOrder(
        calculateCriteriaValue: (node: Tree<T>, parentCriteriaValue: C?) -> C,
        evaluateCriteria: (criteriaValue: C) -> Boolean
    ): Tree<T> {
        fun filterByCriteriaPostOrderRecursive(node: Tree<Pair<T, C>>): Tree<T> =
            Tree<T>(
                node.value.first,
                node.children.filter { evaluateCriteria(it.value.second) }.map { filterByCriteriaPostOrderRecursive(it) })

        val treeEnrichedByCriteriaValue =
            mapPreOrder<Pair<T, C>> { node: Tree<T>, transformedParentValue: Pair<T, C>? ->
                Pair(
                    node.value,
                    calculateCriteriaValue(node, transformedParentValue?.second)
                )
            }
        return filterByCriteriaPostOrderRecursive(treeEnrichedByCriteriaValue)
    }

    /**
     * Filter tree by predicate
     * subtrees for which is predicate of root node true are not included into result tree
     */
    fun filterByPredicatePostOrder(predicate: (node: Tree<T>) -> Boolean): Tree<T> {
        fun filterByPredicateRecursive(node: Tree<T>): Tree<T> =
            Tree<T>(node.value, node.children.map { filterByPredicateRecursive(it) }.filter(predicate))
        return filterByPredicateRecursive(this)
    }

    /**
     * Flat map levels
     * Transform Tree<T> -> List<List<T>> .. list of levels, each level contains its nodes (using breadth-first search)
     */
    fun <R, I> flatMapLevelsIndexed(
        initialIndex: I,
        nodeTransform: (index: I, node: Tree<T>) -> R,
        nextItemIndexTransform: (index: I, node: Tree<T>) -> I,
        levelUpIndexTransform: (index: I, node: List<Tree<T>>) -> I
    ): List<List<R>> =
        mapIndexedBreadthFirst(listOf(this), initialIndex, nodeTransform, nextItemIndexTransform, levelUpIndexTransform)

    fun <R> flatMapLevelsIndexed(transform: (index: BfIndex, node: Tree<T>) -> R): List<List<R>> =
        flatMapLevelsIndexed<R, BfIndex>(
            BfIndex(-1, -1, -1), transform,
            nextItemIndexTransform = { index, _ -> BfIndex(index.nodeIndex + 1, index.depth, index.width + 1) },
            levelUpIndexTransform = { index, _ -> BfIndex(index.nodeIndex, index.depth + 1, -1) })

    /**
     * ForEach on Tree<T> breadth first oder
     */
    fun forEachIndexedBreadthFirst(action: (node: Tree<T>, index: BfIndex) -> Unit) =
        flatMapLevelsIndexed<Unit> { index, node -> action(node, index) }


    fun <R> flatMapLevels(transform: (node: Tree<T>) -> R): List<List<R>> =
        flatMapLevelsIndexed { _, node -> transform(node) }

    /**
     * Flat map indexed
     * Transform Tree<T> -> List<R> (in breadth-first-order)
     */
    fun <R> flatMapIndexedBreadthFirst(transform: (index: BfIndex, node: Tree<T>) -> R): List<R> =
        flatMapLevelsIndexed(transform).flatten()

    /**
     * Flat map
     * Transform Tree<T> -> List<R> (in breadth-first-order)
     */
    fun <R> flatMapBreadthFirst(transform: (node: Tree<T>) -> R): List<R> = flatMapLevels(transform).flatten()

    /**
     * Flat map indexed
     * Transform Tree<T> -> List<R> (in post-order)
     */
    fun <R> flatMapIndexedPostOrder(transform: (node: Tree<T>, preOrderIndex: PostOrderIndex, transformedChildren: List<R>, postOrderIndex: PostOrderIndex) -> R): List<R> =
        flatMapIndexedPostOrder(listOf(this), PostOrderIndex(-1, height() - 1, 0), transform,
            preOrderIndexTransform = { index, _ ->
                PostOrderIndex(
                    index.nodeIndex,
                    index.height - 1,
                    index.depth + 1
                )
            },
            postOrderIndexTransform = { index, _ ->
                PostOrderIndex(
                    index.nodeIndex + 1,
                    index.height + 1,
                    index.depth - 1
                )
            })

    /**
     * Flat map
     * Transform Tree<T> -> List<R> (in post-order)
     */
    fun <R> flatMapPostOrder(transform: (node: Tree<T>, transformedChildren: List<R>) -> R): List<R> =
        flatMapPostOrder(listOf(this), transform)

    /**
     * Flat map indexed
     * Transform Tree<T> -> List<R> (in pre-order)
     */
    fun <R> flatMapIndexedPreOrder(transform: (node: Tree<T>, index: PreOrderIndex, transformedParentValue: R?) -> R): List<R> =
        flatMapIndexedPreOrder(listOf(this),
            PreOrderIndex(-1, -1), transform,
            preOrderIndexTransform = { index, _, _ ->
                PreOrderIndex(
                    index.nodeIndex + 1,
                    index.depth + 1
                )
            },
            postOrderIndexTransform = { index, _, _, _ ->
                PreOrderIndex(
                    index.nodeIndex,
                    index.depth - 1
                )
            })

    /**
     * Flat map
     * Transform Tree<T> -> List<R> (in pre-order)
     */
    fun <R> flatMapPreOrder(transform: (node: Tree<T>, transformedParentValue: R?) -> R): List<R> =
        flatMapPreOrder(listOf(this), transform)

    fun <R> flatten(order: TreeOrder = TreeOrder.PreOrder) {
        when (order) {
            TreeOrder.PreOrder -> flattenPreOrder()
            TreeOrder.PostOrder -> flattenPostOrder()
            TreeOrder.BreadthFirstOrder -> flattenBreadthFirst()
        }
    }

    /**
     * Flatten
     * Transform Tree<T> -> List<T> (in breadth-first order)
     */
    fun flattenBreadthFirst(): List<T> = flatMapBreadthFirst { it.value }

    /**
     * Flatten
     * Transform Tree<T> -> List<T> (in post-order)
     */
    fun flattenPostOrder(): List<T> = flatMapPostOrder { node, _ -> node.value }

    /**
     * Flatten
     * Transform Tree<T> -> List<T> (in pre-order)
     */
    fun flattenPreOrder(): List<T> = flatMapPreOrder { node, _ -> node.value }

    /**
     * Find subtree by given predicate in given order
     */
    fun find(order: TreeOrder = TreeOrder.PreOrder, predicate: (Tree<T>) -> Boolean): Tree<T>? =
        when (order) {
            TreeOrder.PreOrder -> findPreOrder(predicate)
            TreeOrder.PostOrder -> findPostOrder(predicate)
            TreeOrder.BreadthFirstOrder -> findBreadthFirst(predicate)
        }

    fun findBreadthFirst(predicate: (Tree<T>) -> Boolean): Tree<T>? = flatMapBreadthFirst { it }.find(predicate)
    fun findPostOrder(predicate: (Tree<T>) -> Boolean): Tree<T>? =
        flatMapPostOrder<Tree<T>> { node, _ -> node }.find(predicate)

    fun findPreOrder(predicate: (Tree<T>) -> Boolean): Tree<T>? =
        flatMapPreOrder<Tree<T>> { node, _ -> node }.find(predicate)

    private fun nodeDepths(): List<Pair<Tree<T>, Int>> = flatMapLevelsIndexed { index, t -> t to index.depth }.flatten()

    private fun leafDepths(): List<Pair<Tree<T>, Int>> = nodeDepths().filter { it.first.children.count() == 0 }

    fun height(): Int = nodeDepths().maxBy { it.second }?.second?.plus(1) ?: 0
    fun width(): Int = flatMapLevels { it.value }.maxBy { it.size }?.size ?: 0

    fun nodeCount(): Int = flattenBreadthFirst().count()
    fun leafCount(): Int = flatMapBreadthFirst { it }.filter { it.children.isEmpty() }.count()

    override fun toString(): String {
        val outputString = StringBuilder(1000)
        fun prettyPrintRecursive(node: Tree<T>, indent: String = "", last: Boolean) {
            outputString.append(indent)
            val newIndent = indent + if (last) {
                outputString.append("└-")
                "  "
            } else {
                outputString.append("├-")
                "| "
            }
            outputString.append("${node.value}\n")
            node.children.forEachIndexed { index, childNode ->
                prettyPrintRecursive(
                    childNode,
                    newIndent,
                    index == node.children.lastIndex
                )
            }
        }
        prettyPrintRecursive(this, "", children.isNotEmpty())
        return outputString.toString()
    }
}
