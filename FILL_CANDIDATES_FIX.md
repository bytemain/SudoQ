# FillAllCandidates 撤销功能修复

## 问题原因

原来的 `fillAllCandidates()` 方法直接修改 Cell 的候选数，没有通过 Action 系统记录操作，导致无法撤销。

## 修复内容

### 1. 创建了新的 Action 类：FillCandidatesAction.kt

位置：`/workspaces/SudoQ/sudoq-app/sudoqmodel/src/main/kotlin/de/sudoq/model/actionTree/FillCandidatesAction.kt`

这个类实现了批量候选数修改的可撤销操作：

- **CellChange**: 数据类，记录每个单元格的候选数变更（cell, candidate, shouldSet）
- **execute()**: 应用所有变更
  - 检查当前状态与目标状态（shouldSet）
  - 如果不匹配则 toggle（添加或删除候选数）
- **undo()**: 撤销所有变更
  - 检查当前状态是否与 shouldSet 匹配
  - 如果匹配则 toggle（反向操作）
- **inverse()**: 返回 false（不是自逆操作）

### 2. 修改了 SudokuController.fillAllCandidates()

位置：`/workspaces/SudoQ/sudoq-app/sudoqapp/src/main/kotlin/de/sudoq/controller/sudoku/SudokuController.kt`

修改逻辑：
```kotlin
// 之前：直接修改
cell.toggleNote(candidate)

// 之后：收集所有变更
changes.add(FillCandidatesAction.CellChange(cell, candidate, shouldSet))

// 通过 Action 系统执行
val fillAction = FillCandidatesAction(changes)
game.addAndExecute(fillAction)
```

## 关键逻辑说明

### Execute 和 Undo 的对称性

假设某个单元格的候选数 5：

**初始状态**: 未设置（false）
**目标状态**: 应设置（shouldSet = true）

1. **execute()** 时：
   ```
   isCurrentlySet = false
   shouldSet = true
   不匹配 -> toggle() -> 候选数被添加
   结果：isCurrentlySet = true
   ```

2. **undo()** 时：
   ```
   isCurrentlySet = true (execute 之后的状态)
   shouldSet = true (目标状态不变)
   匹配 -> toggle() -> 候选数被移除
   结果：isCurrentlySet = false (回到初始状态)
   ```

### 为什么 undo 要检查 "匹配" 而不是 "不匹配"？

- **execute()** 是从初始状态到目标状态：不匹配时 toggle
- **undo()** 是从目标状态回到初始状态：匹配时 toggle（反向）

## 测试方法

### 手动测试步骤

1. 打开 SudoQ 应用
2. 进入一个数独游戏
3. 点击"Fill Candidates"按钮（fillAllCandidates）
4. 观察：空白单元格应该填充了有效的候选数
5. 点击"Undo"按钮（撤回）
6. 验证：所有填充的候选数应该被清除
7. 点击"Redo"按钮（重做）
8. 验证：候选数应该重新出现

### 单元测试

创建了两个测试文件：

1. **FillCandidatesActionTest.kt** - 测试 Action 类本身的 execute/undo 逻辑
2. **FillCandidatesUndoTest.kt** - 测试在 Game 中的集成效果

运行测试（需要 Android SDK）：
```bash
cd /workspaces/SudoQ/sudoq-app
./gradlew :sudoqmodel:test --tests "de.sudoq.model.actionTree.FillCandidatesActionTest"
./gradlew :sudoqapp:testDebugUnitTest --tests "de.sudoq.controller.sudoku.FillCandidatesUndoTest"
```

## 验证清单

- ✅ FillCandidatesAction 类创建
- ✅ execute() 方法正确实现
- ✅ undo() 方法正确实现（对称逻辑）
- ✅ inverse() 返回 false
- ✅ SudokuController.fillAllCandidates() 使用 Action 系统
- ✅ 所有变更通过 game.addAndExecute() 记录
- ✅ 编译无错误

## 如何验证修复是否生效

如果不能运行完整的 Android 测试，可以通过以下方式验证：

1. **查看代码逻辑**：检查 `SudokuController.fillAllCandidates()` 是否调用了 `game.addAndExecute()`
2. **检查 Action 记录**：在应用中填充候选数后，检查 `game.stateHandler.canUndo()` 是否返回 true
3. **测试 Undo 按钮**：UI 中的 Undo 按钮应该变为可用状态
4. **执行撤销**：点击 Undo 应该移除所有候选数

## 可能的问题排查

如果撤销仍然不工作：

1. **检查是否调用了新代码**：在 `fillAllCandidates()` 开始处添加日志
2. **检查 changes 是否为空**：可能没有变更需要记录
3. **检查 Action 是否被添加**：在 `game.addAndExecute()` 后检查 ActionTree
4. **验证 undo() 逻辑**：确保 toggle 的条件正确

## 总结

这个修复确保了 `fillAllCandidates()` 操作与应用的其他操作保持一致，都通过 Action 系统记录，从而支持完整的撤销/重做功能。

关键点是理解：
- **execute()**: 初始 -> 目标（不匹配时 toggle）
- **undo()**: 目标 -> 初始（匹配时 toggle，反向操作）
