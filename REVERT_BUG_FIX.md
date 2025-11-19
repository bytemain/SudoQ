# Revert 功能 Crash Bug 修复

## 🐛 Bug 描述

### 错误日志
```
Cell ID: 74
Old value: 2
Diff: -9
New value: -7  ← 错误！Cell 值不能为负数
MaxValue: 8

java.lang.IllegalArgumentException: value must not be negative (except is -1 for empty) but is -7
```

### 复现步骤
1. 打开游戏，填写几个数字
2. 打开 ActionTree
3. 点击较早的节点
4. 选择 "Revert" 模式
5. **崩溃！**

## 🔍 根本原因分析

### 错误的实现逻辑

之前的 `revertToNode()` 实现有严重的逻辑错误：

```kotlin
// ❌ 错误的方法
fun revertToNode(targetNode: ActionTreeElement) {
    // 获取从 target 到 current 的路径
    val pathToRevert = findPath(targetNode, currentState!!)
    
    // 为每个操作创建"反向操作"
    val actionsToRevert = pathToRevert.drop(1).map { it.action }
    val compoundRevertAction = CompoundAction.createRevert(actionsToRevert)
    
    // 在当前状态下添加这个复合操作
    addStrategic(compoundRevertAction)
}
```

**问题 1: 简单取负的错误**

`createRevert()` 中：
```kotlin
when (action) {
    is SolveAction -> SolveAction(-action.diff, action.cell)  // ❌ 只是简单取负
}
```

这个假设是错误的，因为：
- `SolveAction.diff` 是**相对于执行时的 cell 值**的变化量
- 例如：Cell 从 0 → 5，diff = +5
- 但从 State B 回到 State A 时，Cell 的当前值可能已经不是 5 了！

**问题 2: 在错误的状态下执行**

假设历史：
```
State A: Cell #74 = 2
  ↓ (填入 +7)
State B: Cell #74 = 9  
  ↓ (填入 -7)  
State C: Cell #74 = 2 (当前状态)
```

用户想从 State C Revert 到 State A：

错误的逻辑：
1. 计算 State A → State C 的操作：填入 +7，填入 -7
2. 创建"反向"：填入 -7，填入 +7
3. 在 State C (Cell = 2) 执行：
   - Cell = 2 + (-7) = **-5** ❌ 崩溃！

### 正确的理解

Revert 操作的正确含义是：
> **从当前状态，通过添加操作，达到目标状态的 cell 值**

不是"反向执行历史操作"，而是"计算状态差异并生成转换操作"。

## ✅ 修复方案

### 新的实现逻辑

```kotlin
fun revertToNode(targetNode: ActionTreeElement) {
    val originalState = currentState!!
    
    // 1. 收集路径上所有受影响的 cells
    val pathBetween = findPath(targetNode, originalState)
    val affectedCells = mutableSetOf<Cell>()
    pathBetween.forEach { node ->
        when (val action = node.action) {
            is SolveAction -> affectedCells.add(action.cell)
            is NoteAction -> affectedCells.add(action.cell)
            is CompoundAction -> {
                action.actions.forEach { subAction ->
                    if (subAction is SolveAction) affectedCells.add(subAction.cell)
                    if (subAction is NoteAction) affectedCells.add(subAction.cell)
                }
            }
        }
    }
    
    // 2. 导航到目标状态，记录 cell 值
    goToState(targetNode)
    val targetCellStates = mutableMapOf<Cell, Int>()
    for (cell in affectedCells) {
        targetCellStates[cell] = cell.currentValue
    }
    
    // 3. 导航回原始状态
    goToState(originalState)
    
    // 4. 计算差异，创建转换操作
    val revertActions = mutableListOf<Action>()
    for (cell in affectedCells) {
        val currentValue = cell.currentValue
        val targetValue = targetCellStates[cell]!!
        val diff = targetValue - currentValue
        
        if (diff != 0) {
            revertActions.add(SolveAction(diff, cell))
        }
    }
    
    // 5. 创建并添加复合操作
    val compoundRevertAction = CompoundAction(
        actions = revertActions,
        type = CompoundActionType.REVERT,
        description = "Revert to state #${targetNode.id}: ${revertActions.size} change(s)"
    )
    
    addStrategic(compoundRevertAction)
}
```

### 关键改进

1. **基于实际状态差异**：不再尝试"反向执行"历史操作，而是直接计算当前状态和目标状态的差异

2. **只处理受影响的 cells**：不遍历所有 cells，只收集路径上操作过的 cells

3. **正确计算 diff**：
   ```kotlin
   diff = targetValue - currentValue  // ✅ 相对于当前值的差异
   ```

4. **更详细的描述**：
   ```kotlin
   description = "Revert to state #${targetNode.id}: ${revertActions.size} change(s)"
   ```

### 示例验证

现在同样的场景：
```
State A: Cell #74 = 2 (目标)
  ↓ (填入 +7)
State B: Cell #74 = 9  
  ↓ (填入 -7)  
State C: Cell #74 = 2 (当前)
```

新逻辑执行：
1. 收集受影响的 cells：{Cell #74}
2. 导航到 State A，记录：Cell #74 = 2
3. 导航回 State C，当前：Cell #74 = 2
4. 计算 diff = 2 - 2 = 0
5. 无需添加任何操作（已经在目标状态）

更复杂的例子：
```
State A: Cell #74 = 2, Cell #75 = 5 (目标)
State C: Cell #74 = 9, Cell #75 = 3 (当前)
```

新逻辑：
1. 收集：{Cell #74, Cell #75}
2. 导航到 A，记录：#74 = 2, #75 = 5
3. 导航回 C，当前：#74 = 9, #75 = 3
4. 计算：
   - Cell #74: diff = 2 - 9 = -7 ✅
   - Cell #75: diff = 5 - 3 = +2 ✅
5. 创建复合操作包含 2 个 SolveAction
6. 执行时：
   - Cell #74 = 9 + (-7) = 2 ✅
   - Cell #75 = 3 + 2 = 5 ✅

## 📝 代码更改

### 修改的文件

1. **GameStateHandler.kt**
   - 完全重写 `revertToNode()` 方法
   - 添加 `import de.sudoq.model.sudoku.Cell`

2. **CompoundAction.kt**
   - 将 `actions` 从 `private` 改为 `internal`（允许同模块访问）
   - 删除有 bug 的 `createRevert()` 静态方法

## 🧪 测试验证

### 测试场景 1: 简单回退
```
操作序列：
1. 填入 Cell A = 5
2. 填入 Cell B = 3
3. 填入 Cell C = 7

Revert 到步骤 1：
预期：创建包含 2 个操作的 Revert 节点
- Cell B: 3 → 0 (diff = -3)
- Cell C: 7 → 0 (diff = -7)
```

### 测试场景 2: 修改同一个 Cell
```
操作序列：
1. 填入 Cell A = 5
2. 填入 Cell A = 8 (修改)
3. 填入 Cell B = 3

Revert 到步骤 1：
预期：创建包含 2 个操作的 Revert 节点
- Cell A: 8 → 5 (diff = -3)
- Cell B: 3 → 0 (diff = -3)
```

### 测试场景 3: 已在目标状态
```
当前在步骤 3，Revert 到步骤 3：
预期：不创建任何操作，直接返回
```

## 📦 构建状态

```
✅ :sudoqmodel:compileKotlin - SUCCESS
✅ :sudoqapp:assembleDebug - SUCCESS
📦 APK: sudoqapp/build/outputs/apk/debug/sudoqapp-debug.apk
```

## 🎯 下一步

安装新 APK 并测试：
```bash
adb install -r sudoqapp/build/outputs/apk/debug/sudoqapp-debug.apk
```

测试步骤：
1. 填写 5 个数字
2. 打开 ActionTree
3. 点击第 2 个操作
4. 选择 "Revert"
5. 验证：应该创建单个 "↶ Revert" 节点
6. 验证：数字正确回退到第 2 步的状态
7. 验证：**不应该崩溃！**

## 📚 经验教训

1. **不要简单地"反向"操作**：`diff` 的含义依赖于执行时的状态

2. **基于状态差异而非操作历史**：更健壮的方法是比较两个状态，而不是试图"反向"历史

3. **充分测试边界情况**：
   - 同一个 cell 被多次修改
   - Revert 到当前状态
   - Revert 跨越分支点

4. **详细的错误日志很重要**：`SolveAction.execute()` 中的调试日志帮助快速定位了问题

## 🔗 相关文件

- [GameStateHandler.kt](./sudoqmodel/src/main/kotlin/de/sudoq/model/game/GameStateHandler.kt) - 主要修复
- [CompoundAction.kt](./sudoqmodel/src/main/kotlin/de/sudoq/model/actionTree/CompoundAction.kt) - 删除错误方法
- [FIXES_AND_IMPROVEMENTS.md](./FIXES_AND_IMPROVEMENTS.md) - 原始问题记录
