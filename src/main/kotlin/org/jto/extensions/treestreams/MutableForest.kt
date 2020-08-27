package org.jto.extensions.treestreams

class MutableForest<T>(val mutableNodes:MutableList<MutableTree<T>>): Forest<T>(mutableNodes)

fun <T>emptyMutableForest():MutableForest<T> = MutableForest(mutableListOf())
fun <T>mutableForestOf(vararg nodes: MutableTree<T>) =
    if (nodes.isEmpty()) emptyMutableForest()
    else MutableForest(nodes.toMutableList())
