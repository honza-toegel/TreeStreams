# TreeStreams
TreeStreams API is used to work with generic tree/forest structures:
- read only functions:
  - basic tree parameters (nodeCount, leafCount,...)
  - forEach loops 
  - flattening tree to list
- transforming functions:
  - tree transformations (map)
  - tree node filtering (filter)

The API supports both depth-first and breadth-first traversals.

In order to use read functions tree *root node* and *childern function* is required. 
In order to use transforming functions *node creation function* is additionally required, the notation of tree creation function is dependent on depth-first traversal type.
 
### Create tree

```kotlin
class TreeItemA (val number:Int, val children: List<TreeItemA> = emptyList())
/**
 *    1     
 *   / \
 *  2   3
 *  |
 *  4
 */
val rootA = TreeItemA(1, listOf(TreeItemA(2, listOf(TreeItemA(4))), TreeItemA(3)))
```

### Define tree stream

```kotlin
val streamA = GenericTreeStream(rootA) {it.children}
```

### Check basic characteristics of tree

```kotlin
assertEquals(4, streamA.nodeCount())
assertEquals(2, streamA.leafCount())
assertEquals(3, streamA.height())
with(streamA.nodeDepthsMap()) {
  assertEquals(0, this[rootA])
  assertEquals(1, this[rootA.children[0]])
  assertEquals(2, this[rootA.children[0].children[0]])
}
```

### Flatten tree nodes to list
```kotlin
with(streamA.flatMapTreeBf()) {
  assertEquals(rootA, this[0])
  assertEquals(rootA.children[0], this[1])
  assertEquals(rootA.children[1], this[2])
  assertEquals(rootA.children[0].children[0], this[3])
}
```

### Loop over nodes
```kotlin
//Pre order: 1,2,4,3,
streamA.forEachPreOrder { print("${it.number},") }

//Post order: 4,2,3,1,
streamA.forEachPostOrder { print("${it.number},") }

//Breadth first order: 1,2,3,4,
streamA.forEachBf { print("${it.number},") }
```

### Transform to another tree
In order to map tree to another tree, map function is called. The treeMap functions requires tranformation function as parameter which is used to create transformed tree nodes.

```kotlin
/**
 *    Input:         Output:
 *       1      =>     "2"
 *      / \           /  \
 *     2   3        "3"  "4"
 *     |             |
 *     4            "5"
 */
class TreeItemB (val text:String, val children: List<TreeItemB> = emptyList())

val streamB = streamA.mapTreePostOrder<PreOrderTreeItemB> { node, children ->  PreOrderTreeItemB("${node.number + 1}", children) }.toStream { it.children }

val rootB = streamB.rootNode
assertEquals("2", rootB.text)
assertEquals("3", rootB.children[0].text)
assertEquals("5", rootB.children[0].children[0].text)
assertEquals("4", rootB.children[1].text)    
```

### Filter tree nodes
In order to filter tree items, map or filter function can be called. Filter function is returning tree of the same type, require filter predicate and node creation function. 

```kotlin
/**
* Input:         Output:
*     1      =>    1
*    / \          /
*   2  3         2
*   |
*   4
*/
val streamB = streamA.filterTreePostOrder({node,children -> TreeItemA(node.number,children)}, predicate = {node ->  node.number < 3}).toStream { it.children }

//Produce same output as filter above
val streamB = streamA.mapTreePostOrder<TreeItemA>{ node, children -> when(node.number < 3) {
            true -> TreeItemA(node.number, children)
            false -> null
        }}.toStream { it.children }
```
