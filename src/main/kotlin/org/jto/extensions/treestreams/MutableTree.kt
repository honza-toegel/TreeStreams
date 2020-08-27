package org.jto.extensions.treestreams

class MutableTree<T>(
    var mutableNode: T,
    val mutableChildren: MutableList<MutableTree<T>> = mutableListOf()
) : Tree<T>(mutableNode, mutableChildren)

fun <T> mutableTreeOf(value:T) = MutableTree(value)
fun <T> mutableTreeOf(value: T, vararg children:MutableTree<T>) =
    if (children.isEmpty()) mutableTreeOf(value)
    else MutableTree(value, children.toMutableList())