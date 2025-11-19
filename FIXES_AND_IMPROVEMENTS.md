# ActionTree 问题修复和改进 - 2025-11-20

## 🐛 修复的问题

### 问题 1: View Mode 时 ActionTree 显示节点变少

**症状**: 点击 View 模式后，ActionTree 中显示的节点突然减少，看起来像有 bug。退出游戏重进后节点又恢复了。

**根本原因**: `buildActionList` 函数只构建从 root 到 current 的线性路径，当进入 View Mode 临时查看某个历史节点时，只显示到那个节点的路径，其他分支被隐藏了。

**解决方案**: 
重写 `buildActionList` 函数，使用深度优先遍历构建完整的树结构：

```kotlin
private fun buildActionList(
    root: ActionTreeElement,
    current: ActionTreeElement?
): List<ActionItem> {
    val result = mutableListOf<ActionItem>()

    // Build complete tree structure depth-first
    fun traverseTree(node: ActionTreeElement, depth: Int) {
        val branches = if (node.hasChildren()) {
            node.childrenList.toList()
        } else {
            emptyList()
        }

        result.add(
            ActionItem(
                element = node,
                action = node.action,
                depth = depth,
                isCurrent = node == current,
                isMistake = node.isMistake,
                isCorrect = node.isCorrect,
                isMarked = node.isMarked,
                branches = branches
            )
        )

        // Recursively add all children
        branches.forEach { child ->
            traverseTree(child, depth + 1)
        }
    }

    traverseTree(root, 0)
    return result
}
```

**效果**: 现在无论在哪个模式下，用户都能看到完整的操作历史树，View Mode 只是临时查看，不会隐藏其他节点。

---

### 问题 2: Revert 功能和预期不一样

**症状**: Revert 操作会添加多个反向操作节点，而用户期望只看到一个"Revert"节点。

**之前的实现**:
```kotlin
fun revertToNode(targetNode: ActionTreeElement) {
    // 为每个需要撤销的操作添加一个反向操作
    pathToRevert.drop(1).reversed().forEach { nodeToRevert ->
        val reverseAction = createReverseAction(nodeToRevert.action)
        addStrategic(reverseAction)  // 添加多个节点
    }
}
```

这会产生类似这样的历史：
```
→ Solve Cell 5: 3
→ Solve Cell 7: 8
→ Note Cell 12
→ Solve Cell 5: -3    (反向操作 1)
→ Note Cell 12         (反向操作 2)
→ Solve Cell 7: -8    (反向操作 3)
```

**解决方案**: 
创建 `CompoundAction` 类来将多个操作合并为单个节点：

```kotlin
class CompoundAction(
    private val actions: List<Action>,
    val type: CompoundActionType,
    val description: String? = null
) : Action(...)

fun revertToNode(targetNode: ActionTreeElement) {
    val actionsToRevert = pathToRevert.drop(1).map { it.action }
    
    // 创建单个复合操作
    val compoundRevertAction = CompoundAction.createRevert(actionsToRevert)
    
    // 只添加一个节点
    addStrategic(compoundRevertAction)
}
```

**效果**: 现在 Revert 只创建一个节点：
```
→ Solve Cell 5: 3
→ Solve Cell 7: 8
→ Note Cell 12
→ ↶ Revert: Revert 3 operation(s)  ← 单个复合节点
```

UI 显示：
- 标题: `↶ Revert`
- 描述: `Revert 3 operation(s)` 或 `3 × SolveAction, 1 × NoteAction`
- 可以展开查看详细的操作列表（待实现）

---

### 问题 3: 应该实现 Rebase 功能

**用户需求**: 能够将多个连续的操作节点合并成一个，简化历史树。

**解决方案**: 实现 Squash/Rebase 功能

#### 1. 扩展 CompoundAction 类

```kotlin
enum class CompoundActionType(val displayName: String, val icon: String) {
    REVERT("Revert", "↶"),
    SQUASH("Squash", "⊡"),
    MERGE("Merge", "⑂")
}

companion object {
    fun createSquash(actionsToSquash: List<Action>, description: String? = null): CompoundAction {
        return CompoundAction(
            actions = actionsToSquash,
            type = CompoundActionType.SQUASH,
            description = description ?: "Squash ${actionsToSquash.size} operation(s)"
        )
    }
}
```

#### 2. 在 GameStateHandler 中实现 squashNodes

```kotlin
fun squashNodes(
    startNode: ActionTreeElement,
    endNode: ActionTreeElement,
    description: String? = null
): Boolean {
    // 验证是线性路径（没有分支）
    // 收集所有操作
    val actionsToSquash = pathToSquash.map { it.action }
    
    // 创建复合操作
    val compoundAction = CompoundAction.createSquash(actionsToSquash, description)
    
    // 导航回起始节点的父节点
    // 添加复合操作
    addStrategic(compoundAction)
    
    // 更新分支 HEAD
    return true
}
```

#### 3. 创建 SquashDialog UI

```kotlin
@Composable
fun SquashDialog(
    startNode: ActionTreeElement,
    endNode: ActionTreeElement,
    onSquash: (description: String?) -> Unit,
    onDismiss: () -> Unit
) {
    // 显示要合并的操作列表
    // 可选：添加自定义描述
    // 确认按钮
}
```

**效果**:

合并前：
```
→ Solve Cell 1: 5
→ Solve Cell 2: 3
→ Solve Cell 3: 7
→ Solve Cell 4: 2
→ Note Cell 5
```

合并后：
```
→ ⊡ Squash: Filled row 1  (包含 5 个操作)
```

或使用自动生成的描述：
```
→ ⊡ Squash: 4 × SolveAction, 1 × NoteAction
```

---

## 📝 新增的文件

1. **CompoundAction.kt** - 复合操作类
   - 支持 Revert、Squash、Merge 三种类型
   - 可以包含任意数量的子操作
   - 提供摘要和详细信息方法
   - 正确处理 execute 和 undo

2. **SquashDialog.kt** - Squash UI 对话框
   - 显示要合并的操作列表
   - 可选自定义描述
   - 操作摘要显示
   - Material 3 设计

## 🔄 修改的文件

1. **GameStateHandler.kt**
   - `revertToNode()` - 使用 CompoundAction
   - `squashNodes()` - 新增合并节点方法

2. **ActionTreeScreen.kt**
   - `buildActionList()` - 显示完整树结构
   - `getActionTitle()` - 支持 CompoundAction
   - `getActionDescription()` - 支持 CompoundAction

## 🎯 还需要做的 (待实现)

### 集成 Squash UI 到 ActionTree

需要添加：
1. **多选模式**: 允许用户选择节点范围
2. **范围选择 UI**: 显示选择的起始和结束节点
3. **Squash 按钮**: 在顶部栏或浮动按钮
4. **验证逻辑**: 确保选择的是线性路径（无分支）

建议的 UI 流程：
```
1. 用户长按某个节点 → 进入多选模式
2. 点击另一个节点 → 选择范围
3. 点击"Squash"按钮 → 显示 SquashDialog
4. 用户确认 → 执行 squashNodes()
5. 退出多选模式
```

### CompoundAction 详情展开

可以添加：
- 点击 CompoundAction 节点显示详细的子操作列表
- 展开/折叠动画
- 每个子操作的详细信息

### 持久化支持

需要扩展保存格式：
```xml
<action type="compound" compound-type="REVERT">
    <description>Revert 3 operation(s)</description>
    <actions>
        <action type="solve" cell="5" diff="-3"/>
        <action type="note" cell="12" diff="..." />
        <action type="solve" cell="7" diff="-8"/>
    </actions>
</action>
```

## 🧪 测试场景

### 测试 1: View Mode 显示完整树
1. 创建多个分支
2. 点击历史节点选择 "View"
3. **验证**: 仍然能看到所有分支和节点

### 测试 2: Revert 创建单个节点
1. 填写 5 个数字
2. 点击第 2 个操作
3. 选择 "Revert"
4. **验证**: 只添加了 1 个 "↶ Revert" 节点，不是 3 个节点

### 测试 3: Squash 合并操作 (需要 UI 完成)
1. 填写 5 个连续的数字
2. 选择第 1-5 个节点
3. 点击 "Squash"
4. 输入描述 "Filled first row"
5. **验证**: 5 个节点被合并为 1 个 "⊡ Squash" 节点

### 测试 4: Squash 验证线性路径
1. 创建分支结构
2. 尝试 squash 跨越分支点的节点
3. **验证**: 应该失败或提示"只能 squash 线性路径"

## 📊 改进统计

| 改进项 | 之前 | 现在 |
|--------|------|------|
| View Mode 显示节点数 | 只显示到当前的路径 | 显示完整树结构 ✅ |
| Revert 操作节点数 | N 个反向操作 | 1 个 Revert 节点 ✅ |
| 历史合并功能 | ❌ 无 | ✅ Squash 功能 |
| CompoundAction 类型 | ❌ 无 | ✅ Revert/Squash/Merge |
| 自定义操作描述 | ❌ 无 | ✅ 支持自定义描述 |

## 🎨 视觉改进

### CompoundAction 显示图标

| 类型 | 图标 | 颜色提示 |
|------|------|----------|
| Revert | ↶ | 次要色 |
| Squash | ⊡ | 三级色 |
| Merge | ⑂ | 待定 |

### 操作描述格式

- **自动生成**: `3 × SolveAction, 1 × NoteAction`
- **用户自定义**: `Filled row 3`
- **默认**: `Revert 5 operation(s)`

## 💡 设计哲学

这些改进遵循以下原则：

1. **清晰性**: 用户应该始终知道历史树的完整状态
2. **简洁性**: 可以将多个操作合并为有意义的单元
3. **可逆性**: 所有操作都可以撤销（Revert 是复合操作，也可以撤销）
4. **灵活性**: 用户可以自定义操作描述，增加可读性

## 🔗 相关文件

- [CompoundAction.kt](./sudoqmodel/src/main/kotlin/de/sudoq/model/actionTree/CompoundAction.kt)
- [SquashDialog.kt](./sudoqapp/src/main/kotlin/de/sudoq/view/actionTree/SquashDialog.kt)
- [GameStateHandler.kt](./sudoqmodel/src/main/kotlin/de/sudoq/model/game/GameStateHandler.kt)
- [ActionTreeScreen.kt](./sudoqapp/src/main/kotlin/de/sudoq/view/actionTree/ActionTreeScreen.kt)
- [ACTION_TREE_REDESIGN.md](./ACTION_TREE_REDESIGN.md) - 原始设计文档
- [IMPLEMENTATION_SUMMARY.md](./IMPLEMENTATION_SUMMARY.md) - 实现总结
