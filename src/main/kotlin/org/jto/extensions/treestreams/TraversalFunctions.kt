package org.jto.extensions.treestreams

/**
 * BFS breadth-first-traversal -> node based
 */
fun <T, R, I> mapIndexedBreadthFirst(
    rootNodes: List<Tree<T>>,
    initialIndex: I,
    nodeTransform: (index: I, node: Tree<T>) -> R,
    nextItemIndexTransform: (index: I, node: Tree<T>) -> I,
    levelUpIndexTransform: (index: I, node: List<Tree<T>>) -> I
): List<List<R>> {
    var nodeIndex: I = initialIndex

    //Transformation of one level
    fun levelTransform(levelNodes: List<Tree<T>>): List<R> =
        levelNodes.map {
            nodeIndex = nextItemIndexTransform(nodeIndex, it)
            val transformedNode = nodeTransform(nodeIndex, it)
            transformedNode
        }

    //Recursive transformation of all levels
    fun mapBfsLevelsIndexedRecursive(levelNodes: List<Tree<T>>): List<List<R>> =
        when (levelNodes.size) {
            0 -> emptyList()
            else -> {
                nodeIndex = levelUpIndexTransform(nodeIndex, levelNodes)
                listOf(levelTransform(levelNodes)) + mapBfsLevelsIndexedRecursive(levelNodes.flatMap { it.children })
            }
        }

    return mapBfsLevelsIndexedRecursive(rootNodes)
}

fun <T, R, I> mapIndexedPostOrder(
    rootNodes: List<Tree<T>>,
    initialIndex: I,
    transform: (node: Tree<T>, index: I, transformedChildren: List<Tree<R>>) -> R,
    preOrderIndexTransform: (traversalIndex: I, node: Tree<T>) -> I,
    postOrderIndexTransform: (traversalIndex: I, node: Tree<T>) -> I
): List<Tree<R>> {
    var index: I = initialIndex
    fun mapPostOrderRecursive(originalNode: Tree<T>): Tree<R> {
        index = preOrderIndexTransform(index, originalNode)
        val children = originalNode.children.map { mapPostOrderRecursive(it) }
        index = postOrderIndexTransform(index, originalNode)
        val transformedNodeValue = transform(originalNode, index, children)
        return Tree(transformedNodeValue, children)
    }
    return rootNodes.map { mapPostOrderRecursive(it) }
}

fun <T, R, I> flatMapIndexedPostOrder(
    rootNodes: List<Tree<T>>,
    initialIndex: I,
    transform: (node: Tree<T>, preOrderIndex: I, transformedChildren: List<R>, postOrderIndex: I) -> R,
    preOrderIndexTransform: (traversalIndex: I, node: Tree<T>) -> I,
    postOrderIndexTransform: (traversalIndex: I, node: Tree<T>) -> I
): List<R> {
    var index: I = initialIndex
    val mappedResult: MutableList<R> = mutableListOf()
    fun mapPostOrderRecursive(originalNode: Tree<T>): R {
        index = preOrderIndexTransform(index, originalNode)
        val children = originalNode.children.map { mapPostOrderRecursive(it) }
        index = postOrderIndexTransform(index, originalNode)
        val transformedNodeValue = transform(originalNode, index, children, index)
        mappedResult += transformedNodeValue
        return transformedNodeValue
    }
    rootNodes.forEach { mapPostOrderRecursive(it) }
    return mappedResult
}

fun <T, R> mapPostOrder(
    rootNodes: List<Tree<T>>,
    transform: (node: Tree<T>, transformedChildren: List<Tree<R>>) -> R
): List<Tree<R>> {
    fun mapPostOrderRecursive(originalNode: Tree<T>): Tree<R> {
        val children = originalNode.children.map { mapPostOrderRecursive(it) }
        val transformedNodeValue = transform(originalNode, children)
        return Tree(transformedNodeValue, children)
    }
    return rootNodes.map { mapPostOrderRecursive(it) }
}

fun <T, R> flatMapPostOrder(
    rootNodes: List<Tree<T>>,
    transform: (node: Tree<T>, transformedChildren: List<R>) -> R
): List<R> {
    val mappedResult: MutableList<R> = mutableListOf()
    fun mapPostOrderRecursive(originalNode: Tree<T>): R {
        val children = originalNode.children.map { mapPostOrderRecursive(it) }
        val transformedNodeValue = transform(originalNode, children)
        mappedResult += transformedNodeValue
        return transformedNodeValue
    }
    rootNodes.forEach { mapPostOrderRecursive(it) }
    return mappedResult
}

fun <T, R, I> mapIndexedPreOrder(
    rootNodes: List<Tree<T>>,
    initialIndex: I,
    transform: (node: Tree<T>, index: I, transformedParentValue: R?) -> R,
    preOrderIndexTransform: (traversalIndex: I, child: Tree<T>, transformedParentValue: R?) -> I,
    postOrderIndexTransform: (traversalIndex: I, child: Tree<T>, transformedNodeValue: R, transformedParentValue: R?) -> I
): List<Tree<R>> {
    var index = initialIndex
    fun mapPreOrderRecursive(
        originalNode: Tree<T>,
        transformedParentValue: R?
    ): Tree<R> {
        index = preOrderIndexTransform(index, originalNode, transformedParentValue)
        val transformedNodeValue = transform(originalNode, index, transformedParentValue)
        val children = originalNode.children.map { mapPreOrderRecursive(it, transformedNodeValue) }
        index = postOrderIndexTransform(index, originalNode, transformedNodeValue, transformedParentValue)
        return Tree(transformedNodeValue, children)
    }
    return rootNodes.map { mapPreOrderRecursive(it, null) }
}

fun <T, R, I> flatMapIndexedPreOrder(
    rootNodes: List<Tree<T>>,
    initialIndex: I,
    transform: (node: Tree<T>, index: I, transformedParentValue: R?) -> R,
    preOrderIndexTransform: (traversalIndex: I, child: Tree<T>, transformedParentValue: R?) -> I,
    postOrderIndexTransform: (traversalIndex: I, child: Tree<T>, transformedNodeValue: R, transformedParentValue: R?) -> I
): List<R> {
    var index = initialIndex
    val mappedResult: MutableList<R> = mutableListOf()
    fun mapPreOrderRecursive(
        originalNode: Tree<T>,
        transformedParentValue: R?
    ): R {
        index = preOrderIndexTransform(index, originalNode, transformedParentValue)
        val transformedNodeValue = transform(originalNode, index, transformedParentValue)
        mappedResult += transformedNodeValue
        originalNode.children.forEach { mapPreOrderRecursive(it, transformedNodeValue) }
        index = postOrderIndexTransform(index, originalNode, transformedNodeValue, transformedParentValue)
        return transformedNodeValue
    }
    rootNodes.forEach { mapPreOrderRecursive(it, null) }
    return mappedResult
}

fun <T, R> mapPreOrder(
    rootNodes: List<Tree<T>>,
    transform: (node: Tree<T>, transformedParentValue: R?) -> R
): List<Tree<R>> {
    fun mapPreOrderRecursive(
        originalNode: Tree<T>,
        transformedParentValue: R?
    ): Tree<R> {
        val transformedNodeValue = transform(originalNode, transformedParentValue)
        val children = originalNode.children.map { mapPreOrderRecursive(it, transformedNodeValue) }
        return Tree(transformedNodeValue, children)
    }
    return rootNodes.map { mapPreOrderRecursive(it, null) }
}

fun <T, R> flatMapPreOrder(
    rootNodes: List<Tree<T>>,
    transform: (node: Tree<T>, transformedParentValue: R?) -> R
): List<R> {
    val mappedResult: MutableList<R> = mutableListOf()
    fun mapPreOrderRecursive(
        originalNode: Tree<T>,
        transformedParentValue: R?
    ) {
        val transformedNodeValue = transform(originalNode, transformedParentValue)
        mappedResult += transformedNodeValue
        originalNode.children.forEach { mapPreOrderRecursive(it, transformedNodeValue) }
    }
    rootNodes.forEach { mapPreOrderRecursive(it, null) }
    return mappedResult
}
