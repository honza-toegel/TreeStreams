# TreeStreams
The goal is to provide clean API for generic tree/forest structures in order to allow transformations, filtering, for-each loops and all basic operations which are expected from tree. In order to read generic tree structures this API requires tree root item and childern function. In order to map tree<T> into new tree<R> the item tranforming function (T, List<R>)->R needs to be provided in order to create item and bound to its children.
 
### Create tree

```kotlin
class TreeItemA (val number:Int, val children: List<TreeItemA> = emptyList())
/**
 *    1     
 *   / \
 *   2  3
 */
val rootA = PreOrderTreeItemA(1, listOf(PreOrderTreeItemA(2), PreOrderTreeItemA(3)))
```

### Define tree stream

```kotlin
val treeStreamA = GenericTreeStream(rootA) {it.children}
```

### Check base characteristics of tree

```kotlin
assertEquals(3, treeStreamA.nodeCount())
assertEquals(2, treeStreamA.leafCount())
```

### Transform to another tree
  Input:         Output:
    1      =>     "2"
   / \           /  \
  2   3        "3"  "4"


```kotlin
class TreeItemB (val text:String, val children: List<TreeItemB> = emptyList())

val treeStreamB = treeStreamA.mapTreePostOrder<PreOrderTreeItemB> { node, children ->  PreOrderTreeItemB("${node.number + 1}", children) }.toStream { it.children }

assertEquals(treeStreamA.nodeCount(), treeStreamB.nodeCount())
assertEquals(treeStreamA.leafCount(), treeStreamB.leafCount())
assertEquals("2", rootB.text)
assertEquals("3", rootB.children[0].text)
assertEquals("4", rootB.children[1].text)    
```
