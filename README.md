# TreeStreams
TreeStreams API is used to work with generic tree/forest structures:
- read only functions:
  - basic tree parameters (nodeCount, leafCount, height, width)
  - forEach loops 
  - flattening tree to list
- transforming functions:
  - tree transformations (map)
  - tree node filtering (filter)

The API supports both depth-first and breadth-first traversals.
 
### Create tree

```kotlin
/**
 *    1     
 *   / \
 *  2   3
 *  |
 *  4
 */
val intTree = treeOf(1,
        treeOf(2),
        treeOf(3, treeOf(4))
    )
```

### Print tree to standard output
This function can be used for debugging
```kotlin
println(intTree)
```


### Check basic characteristics of tree

```kotlin
assertEquals(3, intTree.height())
assertEquals(2, intTree.width())
assertEquals(4, intTree.nodeCount())
assertEquals(2, intTree.leafCount())
```

### Map tree of Int nodes to String nodes, pre-order
```kotlin
/**
 *    Input:         Output:
 *       1      =>     "2"
 *      / \           /  \
 *     2   3        "3"  "4"
 *     |             |
 *     4            "5"
 */
val resultStringTree = intTree.mapPreOrder<String>{node, _ ->  "${node.value + 1}"}

val expectedResultTree = treeOf("2",
    treeOf("3"),
    treeOf("4", Tree("5"))
)

assertEquals(expectedResultTree, resultStringTree)
```
### Map tree of Int nodes to String nodes, pre-order, using parent value
```kotlin
val resultStringTree = intTree.mapPreOrder<String>{ node, transformedParentValue ->  "${node.value + 1 + (transformedParentValue?.toInt() ?: 0)}"}

val expectedResultTree = treeOf("2",
    treeOf("5"),
    treeOf("6", Tree("11"))
)

assertEquals(expectedResultTree, resultStringTree)
```
### Map tree with node index - mapIndexed
This function is analogical to Collection.mapIndexed, providing tree relevant index 
```kotlin
val indexedStringTreePreOrder = intTree.mapIndexedPreOrder<String> { node, index, _ ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth}"}

assertEquals("""
└-V:1 NodeIndex=0 Depth=0
  ├-V:2 NodeIndex=1 Depth=1
  └-V:3 NodeIndex=2 Depth=1
    └-V:4 NodeIndex=3 Depth=2""".trimIndent(), indexedStringTreePreOrder.toString())

val indexedStringTreePostOrder = intTree.mapIndexedPostOrder<String> { node, index, _ ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth} Height=${index.height}"}

assertEquals("""
└-V:1 NodeIndex=3 Depth=0 Height=2
  ├-V:2 NodeIndex=0 Depth=1 Height=0
  └-V:3 NodeIndex=2 Depth=1 Height=1
    └-V:4 NodeIndex=1 Depth=2 Height=0""".trimIndent(), indexedStringTreePostOrder.toString())

val levelsListsStringBreadthFirst = intTree.flatMapLevelsIndexed{index, node ->  "V:${node.value} NodeIndex=${index.nodeIndex} Depth=${index.depth} Width=${index.width}"}

assertEquals("""[[V:1 NodeIndex=0 Depth=0 Width=0], [V:2 NodeIndex=1 Depth=1 Width=0, V:3 NodeIndex=2 Depth=1 Width=1], [V:4 NodeIndex=3 Depth=2 Width=0]]""",
    levelsListsStringBreadthFirst.toString())
```

### Loop over nodes - forEach
This function is analogical to Collection.forEachIndexed providing tree node index
```kotlin
//Expected results of for each (index to value)
val expectedResults:Queue<Pair<Int, Int>> = LinkedList()

//Pre order looping with index
expectedResults.addAll(listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4))
intTree.forEachIndexedPreOrder{ node, index ->
    val expectedResult = expectedResults.remove()
    assertEquals(expectedResult.first, index.nodeIndex)
    assertEquals(expectedResult.second, node.value)
}
assertTrue(expectedResults.isEmpty())

//Post order looping with index
expectedResults.addAll(listOf(0 to 2, 1 to 4, 2 to 3, 3 to 1))
intTree.forEachIndexedPostOrder{ node, index ->
    val expectedResult = expectedResults.remove()
    assertEquals(expectedResult.first, index.nodeIndex)
    assertEquals(expectedResult.second, node.value)
}
assertTrue(expectedResults.isEmpty())

//Breadth first looping with index
expectedResults.addAll(listOf(0 to 1, 1 to 2, 2 to 3, 3 to 4))
intTree.forEachIndexedBreadthFirst{ node, index ->
    val expectedResult = expectedResults.remove()
    assertEquals(expectedResult.first, index.nodeIndex)
    assertEquals(expectedResult.second, node.value)
}
assertTrue(expectedResults.isEmpty())
```

### Modify children of the tree
In order to add/remove children in a stream way you can use modifyChildrenPostOrder

```kotlin
val modifiedTree = intTree.modifyChildrenPostOrder { _, children ->
    when (children.isEmpty()) {
        //For leaves (no-children) add new child with constant value 11
        true -> listOf(Tree(11))
        //For nodes with children, keep existing children and add new child having value + 1 of the last existing child
        false -> children + listOf(Tree(children.last().value + 1)) }
    }

assertEquals("""
└-1
  ├-2
  | └-11
  ├-3
  | ├-4
  | | └-11
  | └-5
  └-4""".trimIndent(), modifiedTree.toString())
```

### Filter tree nodes
Filtering tree nodes in post-order manner using boolean predicate:

```kotlin
/**
* Input:         Output:
*     1      =>    1
*    / \          /
*   2  3         2
*   |
*   4
*/
val filteredTree = intTree.filterByPredicatePostOrder { node -> node.value <= 2 }

assertEquals("""
└-1
  └-2
""".trimIndent(), filteredTree.toString())
```

Filtering tree by calculated criteria value of given type, 
The criteria value is calculated in pre/post order manner
```kotlin
/**
* Input:         Output:
*     1      =>    1
*    / \          / \
*   2  3         2  3
*   | 
*   4 
*/
val filteredTree = intTree.filterByCriteriaPreOrder<Int>(
    calculateCriteriaValue = { node, parentCriteriaValue ->  node.value * 2 + 1 + (parentCriteriaValue ?: 0)},
    evaluateCriteria = { criteriaValue ->  criteriaValue <= 15 } )

assertEquals("""
└-1
  ├-2
  └-3
""".trimIndent(), filteredTree.toString())
```
