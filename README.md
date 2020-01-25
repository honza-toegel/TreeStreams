# TreeStreams
The goal is to provide clean API for generic tree/forest structures in order to allow transformations, filtering, for-each loops and all basic operations which are expected from tree. In order to read generic tree structures this API requires tree root item and childern function. In order to map tree<T> into new tree<R> the item tranforming function (T, List<R>)->R needs to be provided in order to create item and bound to its children.
 
### Create tree

```kotlin
class TreeItemA (val number:Int, val children: List<TreeItemA> = emptyList())
/**
 *    1     
 *   / \
 *  2   3
 */
val rootA = TreeItemA(1, listOf(TreeItemA(2), TreeItemA(3)))
```

### Define tree stream

```kotlin
val streamA = GenericTreeStream(rootA) {it.children}
```

### Check base characteristics of tree

```kotlin
assertEquals(3, streamA.nodeCount())
assertEquals(2, streamA.leafCount())
```

### Transform to another tree
```kotlin
/**
 *    Input:         Output:
 *       1      =>     "2"
 *      / \           /  \
 *     2   3        "3"  "4"
 */
class TreeItemB (val text:String, val children: List<TreeItemB> = emptyList())

val streamB = streamA.mapTreePostOrder<PreOrderTreeItemB> { node, children ->  PreOrderTreeItemB("${node.number + 1}", children) }.toStream { it.children }

assertEquals(streamA.nodeCount(), streamB.nodeCount())
assertEquals(streamA.leafCount(), streamB.leafCount())

val rootB = streamB.rootNode
assertEquals("2", rootB.text)
assertEquals("3", rootB.children[0].text)
assertEquals("4", rootB.children[1].text)    
```
