package org.jto.extensions.treestreams

class MutableTree<T>(
    var mutableNode: T,
    val mutableChildren: MutableList<Tree<T>>
) : Tree<T>(mutableNode, mutableChildren)