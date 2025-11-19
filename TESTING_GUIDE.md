# ActionTree 分支功能测试指南

## 已集成的功能 ✅

### 核心功能
- ✅ Git-like 分支系统
- ✅ 三种操作模式：Checkout / Revert / View
- ✅ 分支管理器（创建、切换、删除）
- ✅ 视图模式（临时查看不修改）

### UI 组件
- ✅ ActionModeDialog - 选择操作模式
- ✅ BranchPicker - 分支选择器
- ✅ ActionTreeScreenWithBranches - 增强的 ActionTree 界面

### 集成点
- ✅ SudokuScreen 现在使用 `ActionTreeScreenWithBranches`
- ✅ 保持向后兼容（旧的 `ActionTreeScreen` 仍然可用）

## 如何测试

### 1. 编译并运行 App
```bash
cd /Users/artin/0Workspace/github.com/bytemain/SudoQ/sudoq-app
./gradlew :sudoqapp:assembleDebug
```

### 2. 安装到设备/模拟器
```bash
adb install -r sudoqapp/build/outputs/apk/debug/sudoqapp-debug.apk
```

### 3. 测试场景

#### 场景 1: 基本分支创建
1. 打开一个数独游戏
2. 填写几个数字（例如 3-5 个操作）
3. 点击 ActionTree 按钮（顶部工具栏）
4. 点击顶部的分支名称（"main"）或右侧的分支图标
5. 点击 "+" 创建新分支
6. 输入分支名称，例如 "test-branch"
7. 验证：分支列表中出现新分支

#### 场景 2: Checkout 到历史节点
1. 在 ActionTree 中，点击某个历史操作节点
2. 在弹出的对话框中，选择 "Checkout"
3. （可选）输入自定义分支名称
4. 点击 "Confirm"
5. 验证：
   - 游戏状态回到该节点
   - 创建了一个新分支
   - 顶部显示新分支名称
   - 原来的 main 分支保持不变

#### 场景 3: Revert 操作
1. 在 ActionTree 中，点击某个历史操作节点
2. 选择 "Revert"
3. 点击 "Confirm"
4. 验证：
   - 当前分支添加了反向操作
   - 游戏状态回到目标节点
   - 历史记录显示了新增的反向操作
   - 分支保持不变

#### 场景 4: View Only 模式
1. 在 ActionTree 中，点击某个历史操作节点
2. 选择 "View Only"
3. 点击 "Confirm"
4. 验证：
   - 顶部显示 "View Mode - Changes not saved"
   - 游戏状态临时切换到该节点
   - 可以查看棋盘状态
5. 点击 "Exit" 退出查看模式
6. 验证：
   - 回到原来的状态
   - 没有创建新分支或修改历史

#### 场景 5: 分支切换
1. 创建多个分支（重复场景 2）
2. 点击顶部的分支名称
3. 在分支列表中选择另一个分支
4. 验证：
   - 游戏状态切换到该分支的 HEAD
   - 顶部显示新的分支名称
   - ActionTree 显示该分支的历史

#### 场景 6: 多分支独立操作
1. 在 main 分支填写几个数字
2. Checkout 到某个节点，创建 branch-1
3. 在 branch-1 填写不同的数字
4. 切换回 main 分支
5. 验证：
   - main 分支保持原来的状态
   - 可以在两个分支间自由切换
   - 每个分支的操作历史独立

### 4. 预期行为

#### 分支保护
- ❌ 不能删除 main 分支
- ❌ 不能删除当前所在的分支
- ✅ 可以删除其他分支

#### 状态同步
- ✅ 切换分支时游戏状态正确更新
- ✅ ActionTree 显示正确的历史
- ✅ Undo/Redo 在当前分支内工作

#### UI 反馈
- ✅ 当前分支在列表中高亮显示
- ✅ 分支创建时间显示相对时间
- ✅ 操作数量正确显示
- ✅ View 模式有明显的 UI 指示

## 可能的问题和调试

### 如果编译失败
```bash
# 清理构建
./gradlew clean

# 重新编译
./gradlew :sudoqapp:assembleDebug
```

### 如果运行时崩溃
1. 查看 logcat 输出
```bash
adb logcat | grep -i "sudoq"
```

2. 常见问题：
   - 空指针：检查 `gameStateHandler` 是否正确初始化
   - 类型转换：确保 ActionBranch 和 BranchManager 正确导入

### 如果 UI 不显示
1. 检查 ActionTreeScreen 是否正确导入
2. 确认 SudokuScreen 使用了 `ActionTreeScreenWithBranches`
3. 验证 R.string 资源存在

## 已知限制

### 当前版本
- ✅ 基本分支功能完整
- ✅ UI 完全集成
- ⏳ 持久化尚未实现（游戏关闭后分支信息会丢失）
- ⏳ 高级功能待实现（Commit Message, Tags, Stash 等）

### 下一步开发
1. **持久化支持** - 保存分支信息到磁盘
2. **Commit Message** - 为重要状态添加说明
3. **分支对比** - 显示两个分支的差异
4. **更多 UI 优化** - 分支可视化图表

## 反馈

测试后请记录：
- ✅ 哪些功能正常工作
- ❌ 发现的 Bug
- 💡 改进建议
- 🎯 用户体验反馈

## 快速演示脚本

```
1. 打开游戏
2. 填写 5 个数字
3. 打开 ActionTree
4. 点击第 3 个操作
5. 选择 "Checkout" -> 创建 branch-experiment
6. 填写 3 个不同的数字
7. 打开 ActionTree，点击分支名称
8. 切换回 "main"
9. 验证：原来的 5 个数字都在
10. 再切换到 "branch-experiment"
11. 验证：看到不同的 3 个数字
12. 成功！🎉
```

## 代码位置

- **核心逻辑**: `sudoqmodel/src/main/kotlin/de/sudoq/model/actionTree/`
  - ActionBranch.kt
  - BranchManager.kt
  
- **GameStateHandler**: `sudoqmodel/src/main/kotlin/de/sudoq/model/game/GameStateHandler.kt`

- **UI 组件**: `sudoqapp/src/main/kotlin/de/sudoq/view/actionTree/`
  - ActionModeDialog.kt
  - BranchPicker.kt
  - ActionTreeScreen.kt (包含 ActionTreeScreenWithBranches)

- **集成点**: `sudoqapp/src/main/kotlin/de/sudoq/controller/sudoku/SudokuScreen.kt`
