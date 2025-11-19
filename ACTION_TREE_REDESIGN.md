# ActionTree Redesign Proposal - Git-like Branch Management

## 当前问题 (Current Problems)

### 核心问题
当用户在 ActionTree 中点击一个历史操作节点时，当前实现会永久性地跳转到那个状态，无法回到之前的最新状态。这导致用户的操作历史丢失，就像 Git 中直接 `checkout` 到一个旧 commit，HEAD 变成 detached 状态，之前的分支记录就找不回来了。

### 具体场景
```
Initial state:
Root -> A -> B -> C -> D (current, 用户在这里)

用户点击 B:
Root -> A -> B (current, 永远回不到 D 了!)
         ╰-> C -> D (这个分支完全丢失，无法访问)
```

现在如果用户从 B 继续操作：
```
Root -> A -> B (继续操作)
         ╰-> E -> F (新分支)
         ╰-> C -> D (旧分支永远消失了)
```

## 设计目标 (Design Goals)

1. **永不丢失历史** - 像 Git 一样，所有操作记录都应该保留
2. **灵活的时间旅行** - 可以在不同的状态间自由切换
3. **分支管理** - 支持多个平行的操作分支
4. **清晰的用户意图** - 明确区分"查看"和"修改"操作

## 解决方案：Git-like 分支系统

### 核心概念

#### 1. Branch (分支)
每个操作序列形成一个分支，分支有名字和指向最新操作的指针。

```kotlin
data class ActionBranch(
    val id: String,                    // 分支唯一标识
    val name: String,                  // 用户可见的分支名称
    val head: ActionTreeElement,       // 分支的 HEAD (最新节点)
    val createdAt: Long,               // 创建时间戳
    val createdFrom: ActionTreeElement // 从哪个节点创建的分支
)
```

#### 2. Current Branch (当前分支)
系统记录用户当前所在的分支，而不仅仅是当前节点。

```kotlin
class BranchManager {
    private val branches = mutableMapOf<String, ActionBranch>()
    var currentBranch: ActionBranch? = null
        private set
    var currentNode: ActionTreeElement? = null
        private set
}
```

### 用户交互方案

#### 点击历史节点时弹出选项对话框

```
┌─────────────────────────────────────┐
│  Choose Action                      │
├─────────────────────────────────────┤
│                                     │
│  ○ Checkout (切换到此状态)          │
│    - 新建分支从这里开始             │
│    - 当前分支保持不变，可随时切换   │
│                                     │
│  ○ Revert (撤销到此状态)            │
│    - 在当前分支创建新的"反向"操作   │
│    - 保持线性历史记录               │
│                                     │
│  ○ View Only (仅查看)               │
│    - 临时查看，不改变当前状态       │
│                                     │
│  [Cancel]  [Confirm]                │
└─────────────────────────────────────┘
```

### 三种操作模式详解

#### 1. Checkout (检出/切换分支)

**行为**：
- 创建新分支（如果目标节点不是任何分支的 HEAD）
- 切换到目标节点
- 原分支完整保留

**示例**：
```
当前状态：
  main: Root -> A -> B -> C -> D (current)

用户点击 B，选择 "Checkout":
  main: Root -> A -> B -> C -> D
  branch-1: Root -> A -> B (current) ← 新分支
```

从 branch-1 继续操作：
```
  main: Root -> A -> B -> C -> D
  branch-1: Root -> A -> B -> E -> F (current)
```

可以随时切换回 main 分支：
```
  main: Root -> A -> B -> C -> D (current)
  branch-1: Root -> A -> B -> E -> F
```

**实现**：
```kotlin
fun checkoutToNode(targetNode: ActionTreeElement) {
    // 保存当前分支的 HEAD
    currentBranch?.let {
        branches[it.id] = it.copy(head = currentNode!!)
    }
    
    // 检查目标节点是否是某个分支的 HEAD
    val existingBranch = branches.values.find { it.head.id == targetNode.id }
    
    if (existingBranch != null) {
        // 切换到已存在的分支
        currentBranch = existingBranch
        currentNode = targetNode
    } else {
        // 创建新分支
        val newBranch = createBranchAt(targetNode)
        currentBranch = newBranch
        currentNode = targetNode
    }
    
    // 执行所需的 undo/execute 操作以到达目标状态
    navigateToNode(currentNode!!, targetNode)
}
```

#### 2. Revert (回退/反向操作)

**行为**：
- 在当前分支末尾添加新的反向操作
- 保持线性历史，不创建分支
- 类似 Git 的 `git revert`

**示例**：
```
当前状态：
  main: Root -> A -> B -> C -> D (current)
  D 操作: Cell #5: 3 → 7

用户点击 B，选择 "Revert":
  main: Root -> A -> B -> C -> D -> D' -> C' (current)
  D' 操作: Cell #5: 7 → 3 (撤销 D)
  C' 操作: Cell #? (撤销 C)
```

历史记录清晰显示：
```
Root  初始状态
  ↓
  A   Cell #1: 0 → 5
  ↓
  B   Cell #2: 0 → 3
  ↓
  C   Cell #3: 0 → 8
  ↓
  D   Cell #5: 3 → 7
  ↓
  D'  Cell #5: 7 → 3  [Revert D]
  ↓
  C'  Cell #3: 8 → 0  [Revert C]
  ↓
(current - 回到了 B 的状态，但历史完整)
```

**实现**：
```kotlin
fun revertToNode(targetNode: ActionTreeElement) {
    // 计算从 current 到 target 需要撤销的操作
    val pathToRevert = findPathBetween(targetNode, currentNode!!)
    
    // 为每个需要撤销的操作创建反向操作
    pathToRevert.reversed().forEach { nodeToRevert ->
        val reverseAction = createReverseAction(nodeToRevert.action)
        val newNode = actionTree.add(reverseAction, currentNode!!)
        newNode.execute()
        currentNode = newNode
    }
    
    // 更新当前分支的 HEAD
    currentBranch?.let {
        branches[it.id] = it.copy(head = currentNode!!)
    }
}

private fun createReverseAction(action: Action): Action {
    return when (action) {
        is SolveAction -> {
            val cell = action.cell
            val oldValue = cell.currentValue
            // 创建反向操作
            SolveAction(-action.diff, cell)
        }
        is NoteAction -> {
            // Note 的反向就是再次切换
            NoteAction(action.diff, action.cell)
        }
        else -> throw IllegalArgumentException("Unknown action type")
    }
}
```

#### 3. View Only (仅查看)

**行为**：
- 临时切换到目标状态查看棋盘
- 不改变当前分支
- 不记录在历史中
- 类似"只读模式"

**实现**：
```kotlin
fun viewNode(targetNode: ActionTreeElement): ViewContext {
    // 保存当前状态
    val savedBranch = currentBranch
    val savedNode = currentNode
    
    // 临时切换（不修改 branches）
    navigateToNode(currentNode!!, targetNode)
    
    // 返回恢复上下文
    return ViewContext(
        originalBranch = savedBranch,
        originalNode = savedNode,
        viewingNode = targetNode
    )
}

// 退出查看模式，恢复原状态
fun exitViewMode(context: ViewContext) {
    navigateToNode(currentNode!!, context.originalNode!!)
    currentBranch = context.originalBranch
    currentNode = context.originalNode
}
```

### UI 设计

#### 分支选择器
```
┌─────────────────────────────────┐
│ Branches                     [+]│
├─────────────────────────────────┤
│ ● main (current)                │
│   └─ Cell #5: 0 → 7             │
│   8 actions • 2 min ago         │
│                                 │
│ ○ branch-1                      │
│   └─ Cell #3: 0 → 4             │
│   5 actions • 5 min ago         │
│                                 │
│ ○ branch-2                      │
│   └─ Cell #9: 2 → 6             │
│   3 actions • 1 hour ago        │
└─────────────────────────────────┘
```

#### Timeline 视图（增强）
```
main                branch-1         branch-2
 │
 ● Root (all branches start here)
 │
 ● A: Cell #1 → 5
 │
 ● B: Cell #2 → 3
 ├─────────────────────────┐
 │                         │
 ● C: Cell #3 → 8          ● E: Cell #3 → 4 [branch-1]
 │                         │
 ● D: Cell #5 → 7 [main]   ● F: Cell #6 → 2
 │                         │
 ●◄ current               ● G: Cell #7 → 1
                          │
                          ● H: Cell #8 → 9 [branch-2]
```

#### 操作节点右键菜单
```
┌────────────────────────────┐
│ Action: Cell #5: 3 → 7     │
├────────────────────────────┤
│ Checkout Here...           │
│ Revert to Here...          │
│ View State                 │
│ ───────────────────────    │
│ Create Branch...           │
│ Toggle Bookmark       ☆    │
│ Copy Action Details        │
└────────────────────────────┘
```

## 数据结构设计

### 扩展 GameStateHandler
```kotlin
class GameStateHandler : ObservableModelImpl<ActionTreeElement>() {
    val actionTree: ActionTree = ActionTree()
    val branchManager: BranchManager = BranchManager()
    
    // 当前分支和节点
    var currentBranch: ActionBranch?
        get() = branchManager.currentBranch
        private set
    
    var currentState: ActionTreeElement?
        get() = branchManager.currentNode
        private set
    
    // 初始化默认 main 分支
    init {
        val mainBranch = ActionBranch(
            id = "main",
            name = "main",
            head = actionTree.root,
            createdAt = System.currentTimeMillis(),
            createdFrom = actionTree.root
        )
        branchManager.addBranch(mainBranch)
        branchManager.switchToBranch(mainBranch)
    }
    
    // 新增方法
    fun checkoutToNode(targetNode: ActionTreeElement, branchName: String? = null)
    fun revertToNode(targetNode: ActionTreeElement)
    fun viewNode(targetNode: ActionTreeElement): ViewContext
    fun switchBranch(branchId: String)
    fun createBranch(name: String, startNode: ActionTreeElement): ActionBranch
    fun deleteBranch(branchId: String)
    fun mergeBranch(sourceBranch: ActionBranch, targetBranch: ActionBranch)
}
```

### BranchManager
```kotlin
class BranchManager {
    private val branches = mutableMapOf<String, ActionBranch>()
    var currentBranch: ActionBranch? = null
        private set
    var currentNode: ActionTreeElement? = null
        private set
    
    fun addBranch(branch: ActionBranch) {
        branches[branch.id] = branch
    }
    
    fun removeBranch(branchId: String) {
        if (branchId == "main") throw IllegalArgumentException("Cannot delete main branch")
        branches.remove(branchId)
    }
    
    fun switchToBranch(branch: ActionBranch) {
        currentBranch = branch
        currentNode = branch.head
    }
    
    fun getAllBranches(): List<ActionBranch> = branches.values.toList()
    
    fun getBranch(id: String): ActionBranch? = branches[id]
    
    fun updateBranchHead(branchId: String, newHead: ActionTreeElement) {
        branches[branchId]?.let {
            branches[branchId] = it.copy(head = newHead)
        }
    }
}
```

### 持久化支持
```kotlin
// 扩展现有的保存格式，添加分支信息
data class GameStateBE(
    val actionTree: ActionTreeElementBE,
    val currentNodeId: Int,
    val branches: List<ActionBranchBE>,  // 新增
    val currentBranchId: String          // 新增
)

data class ActionBranchBE(
    val id: String,
    val name: String,
    val headNodeId: Int,
    val createdAt: Long,
    val createdFromNodeId: Int
)
```

## UI 组件设计

### 1. ActionTreeScreen（修改）
添加分支切换功能：

```kotlin
@Composable
fun ActionTreeScreen(
    gameStateHandler: GameStateHandler,
    onActionSelected: (ActionTreeElement, ActionMode) -> Unit,
    onClose: () -> Unit
) {
    var showBranchPicker by remember { mutableStateOf(false) }
    var showActionDialog by remember { mutableStateOf<ActionTreeElement?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.clickable { showBranchPicker = true }
                    ) {
                        Icon(Icons.Default.Branch, "Branch")
                        Spacer(Modifier.width(8.dp))
                        Text(gameStateHandler.currentBranch?.name ?: "main")
                        Icon(Icons.Default.ArrowDropDown, "Switch")
                    }
                },
                actions = {
                    IconButton(onClick = { /* Show branch manager */ }) {
                        Icon(Icons.Default.AccountTree, "Branches")
                    }
                }
            )
        }
    ) {
        // Timeline view with branches
        ActionTreeTimelineWithBranches(
            branches = gameStateHandler.branchManager.getAllBranches(),
            currentNode = gameStateHandler.currentState,
            onNodeClick = { node ->
                showActionDialog = node
            }
        )
    }
    
    // Action selection dialog
    showActionDialog?.let { node ->
        ActionModeDialog(
            node = node,
            onModeSelected = { mode ->
                onActionSelected(node, mode)
                showActionDialog = null
            },
            onDismiss = { showActionDialog = null }
        )
    }
}

enum class ActionMode {
    CHECKOUT,   // 检出到新分支
    REVERT,     // 反向操作回退
    VIEW        // 仅查看
}
```

### 2. ActionModeDialog（新增）
```kotlin
@Composable
fun ActionModeDialog(
    node: ActionTreeElement,
    onModeSelected: (ActionMode) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMode by remember { mutableStateOf<ActionMode?>(null) }
    var branchName by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Action") },
        text = {
            Column {
                RadioButtonOption(
                    selected = selectedMode == ActionMode.CHECKOUT,
                    onClick = { selectedMode = ActionMode.CHECKOUT },
                    title = "Checkout",
                    description = "Create new branch from here"
                )
                
                if (selectedMode == ActionMode.CHECKOUT) {
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        label = { Text("Branch name (optional)") },
                        modifier = Modifier.padding(start = 32.dp)
                    )
                }
                
                Spacer(Modifier.height(8.dp))
                
                RadioButtonOption(
                    selected = selectedMode == ActionMode.REVERT,
                    onClick = { selectedMode = ActionMode.REVERT },
                    title = "Revert",
                    description = "Undo to this state with reverse actions"
                )
                
                Spacer(Modifier.height(8.dp))
                
                RadioButtonOption(
                    selected = selectedMode == ActionMode.VIEW,
                    onClick = { selectedMode = ActionMode.VIEW },
                    title = "View Only",
                    description = "Temporarily view this state"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedMode?.let { onModeSelected(it) }
                },
                enabled = selectedMode != null
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 3. BranchPicker（新增）
```kotlin
@Composable
fun BranchPicker(
    branches: List<ActionBranch>,
    currentBranch: ActionBranch?,
    onBranchSelected: (ActionBranch) -> Unit,
    onCreateBranch: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Branches",
                    style = MaterialTheme.typography.titleLarge
                )
                IconButton(onClick = onCreateBranch) {
                    Icon(Icons.Default.Add, "Create branch")
                }
            }
            
            Spacer(Modifier.height(16.dp))
            
            branches.forEach { branch ->
                BranchListItem(
                    branch = branch,
                    isCurrent = branch.id == currentBranch?.id,
                    onClick = { onBranchSelected(branch) }
                )
            }
        }
    }
}
```

## 实现步骤

### Phase 1: 核心数据结构（1-2天）✅ 已完成
1. ✅ 创建 `ActionBranch` 数据类
2. ✅ 实现 `BranchManager` 类
3. ✅ 扩展 `GameStateHandler` 以支持分支
4. ✅ 编译验证通过

### Phase 2: 基本操作（2-3天）✅ 已完成
1. ✅ 实现 `checkoutToNode()` 方法
2. ✅ 实现 `revertToNode()` 方法
3. ✅ 实现 `viewNode()` 方法
4. ✅ 实现分支切换逻辑
5. ✅ 创建反向操作逻辑

### Phase 3: UI 组件（2-3天）✅ 已完成
1. ✅ 创建 `ActionModeDialog` - 用户选择操作模式
2. ✅ 创建 `BranchPicker` - 分支选择和管理
3. ✅ 创建 `ActionTreeScreenWithBranches` - 集成所有功能
4. ✅ 更新 `SudokuScreen` 使用新的分支系统
5. ✅ 构建 APK 成功

### Phase 4: 持久化（1-2天）⏳ 待开始
1. 扩展保存/加载格式
2. 迁移旧数据
3. 测试持久化逻辑

### Phase 5: 高级功能（可选，3-5天）⏳ 待开始
1. 分支对比（Branch Comparison）- 显示两个分支的差异
2. 选择性应用（Selective Apply）- 从其他分支挑选操作应用到当前分支
3. 提交点和消息（Commit Points & Messages）- 为重要状态添加说明
4. 分支删除（带安全检查）
5. 分支重命名
6. 分支统计和可视化增强

### Phase 6: Git 高级特性（可选，2-3天）⏳ 待开始
1. 标签系统（Tags）- 多种类型的标签标记重要节点
2. 暂存功能（Stash）- 临时保存当前思路
3. 历史视图增强（Enhanced History）- 多维度查看历史
4. 格子追溯（Cell Blame）- 查看某个格子的完整填写历史
5. 引用日志（Reflog）- 防止意外丢失操作

---

## 🎉 MVP 已完成！

### 可以使用的功能
✅ 创建和切换分支  
✅ Checkout 到历史节点  
✅ Revert 回退操作  
✅ View Only 临时查看模式  
✅ 分支管理（创建、重命名、删除）  
✅ 完整的 UI 集成  

### 测试指南
请查看 [TESTING_GUIDE.md](./TESTING_GUIDE.md) 了解如何测试所有功能。

### 安装测试
```bash
# APK 位置
/Users/artin/0Workspace/github.com/bytemain/SudoQ/sudoq-app/sudoqapp/build/outputs/apk/debug/sudoqapp-debug.apk

# 安装到设备
adb install -r sudoqapp/build/outputs/apk/debug/sudoqapp-debug.apk
```

## 向后兼容性

### 旧数据迁移
```kotlin
fun migrateOldGameState(oldState: GameStateBE): GameStateBE {
    // 为旧游戏创建默认 main 分支
    val mainBranch = ActionBranchBE(
        id = "main",
        name = "main",
        headNodeId = oldState.currentNodeId,
        createdAt = System.currentTimeMillis(),
        createdFromNodeId = 1  // root
    )
    
    return oldState.copy(
        branches = listOf(mainBranch),
        currentBranchId = "main"
    )
}
```

## 测试策略

### 单元测试
```kotlin
class BranchManagerTest {
    @Test
    fun `checkout creates new branch from target node`()
    
    @Test
    fun `revert adds reverse actions to current branch`()
    
    @Test
    fun `view mode does not modify branches`()
    
    @Test
    fun `switch branch updates current node correctly`()
    
    @Test
    fun `cannot delete main branch`()
}

class GameStateHandlerBranchTest {
    @Test
    fun `checkout preserves original branch`()
    
    @Test
    fun `revert creates correct reverse actions`()
    
    @Test
    fun `multiple branches maintain independent histories`()
}
```

### 用户场景测试
1. 用户回到历史节点并继续游戏
2. 用户在多个分支间切换
3. 用户回退后想恢复
4. 用户创建多层嵌套分支

## 用户体验提升

### 1. 智能分支命名
```kotlin
fun generateBranchName(fromNode: ActionTreeElement): String {
    val timestamp = SimpleDateFormat("MMdd-HHmm").format(Date())
    val action = fromNode.action
    val hint = when (action) {
        is SolveAction -> "solve-${action.cell.id}"
        is NoteAction -> "note-${action.cell.id}"
        else -> "action"
    }
    return "branch-$hint-$timestamp"
}
```

### 2. 分支统计
```kotlin
data class BranchStats(
    val actionCount: Int,
    val mistakeCount: Int,
    val completionPercentage: Int,
    val lastModified: Long
)
```

### 快速切换
- 双击节点 = Checkout（最常用）
- 长按节点 = 显示完整菜单
- 侧滑节点 = Revert

## 借鉴更多 Git 概念

### 1. Commit Message（提交信息）⭐⭐⭐ 强烈推荐

允许用户为重要的游戏状态添加说明，类似 Git 的 commit message。

#### 数据结构
```kotlin
data class ActionTreeElement(
    val id: Int,
    val action: Action,
    val parent: ActionTreeElement?,
    var commitMessage: String? = null,      // 新增：提交信息
    var commitTitle: String? = null,        // 新增：简短标题
    var isCommitPoint: Boolean = false,     // 新增：是否是"提交点"
    val timestamp: Long = System.currentTimeMillis()
)
```

#### UI 设计
```
Timeline view:
┌─────────────────────────────────────┐
│ Root                                │
│  ↓                                  │
│ ○ Cell #1 → 5                       │
│  ↓                                  │
│ ○ Cell #2 → 3                       │
│  ↓                                  │
│ 📝 "Filled top-left corner"         │
│ ● Commit Point #1                   │
│    Message: "Trying pattern A,      │
│    filling corners first. This      │
│    approach seems promising."       │
│  ↓                                  │
│ ○ Cell #5 → 7                       │
│  ↓                                  │
│ ○ Cell #8 → 9                       │
│  ↓                                  │
│ 📝 "First row complete!"            │
│ ● Commit Point #2                   │
│  ↓                                  │
│ ○ Cell #9 → 4 (current)             │
└─────────────────────────────────────┘
```

#### 使用场景
1. **记录思路** - "发现了 naked pair，准备填 3 和 7"
2. **标记里程碑** - "完成了前三行！"
3. **记录疑问** - "这里不确定，先试试 5"
4. **学习复盘** - 游戏结束后回顾思考过程

#### 实现
```kotlin
fun createCommitPoint(message: String, title: String? = null) {
    currentState?.let { node ->
        node.commitMessage = message
        node.commitTitle = title ?: message.take(30)
        node.isCommitPoint = true
        
        // 通知 UI 更新
        notifyListeners(node)
    }
}

// 快速创建提交点
fun quickCommit(predefinedMessage: QuickCommitType) {
    val message = when (predefinedMessage) {
        QuickCommitType.MILESTONE -> "Reached a milestone"
        QuickCommitType.BREAKTHROUGH -> "Found a breakthrough!"
        QuickCommitType.UNCERTAIN -> "Not sure about this move"
        QuickCommitType.ROW_COMPLETE -> "Completed a row"
        QuickCommitType.TECHNIQUE_USED -> "Applied solving technique"
    }
    createCommitPoint(message)
}
```

#### UI 交互
```kotlin
@Composable
fun CommitMessageDialog(
    node: ActionTreeElement,
    onSave: (String, String?) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Commit Message") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    placeholder = { Text("e.g., 'Completed first row'") },
                    singleLine = true
                )
                
                Spacer(Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    placeholder = { 
                        Text("Describe your thinking or strategy...") 
                    },
                    minLines = 3,
                    maxLines = 6
                )
                
                Spacer(Modifier.height(8.dp))
                
                // 快速选择
                Text("Quick messages:", style = MaterialTheme.typography.labelSmall)
                FlowRow(Modifier.padding(top = 4.dp)) {
                    QuickCommitChip("Milestone 🎯", onClick = { 
                        message = "Reached an important milestone" 
                    })
                    QuickCommitChip("Breakthrough 💡", onClick = { 
                        message = "Found a key insight!" 
                    })
                    QuickCommitChip("Uncertain 🤔", onClick = { 
                        message = "Not sure about this approach" 
                    })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(message, title.ifBlank { null }) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

### 2. Stash（暂存）⭐⭐ 推荐

保存当前未完成的思路，稍后恢复。

#### 概念
```
当前游戏状态：
main: Root → A → B → C → D (current)

玩家想尝试新思路，但不想丢失当前进度：
1. Stash current work: 保存 D 状态
2. Checkout to B: 回到 B
3. 继续新思路...
4. 随时可以 Apply Stash 恢复之前的工作
```

#### 数据结构
```kotlin
data class GameStash(
    val id: String,
    val name: String,
    val description: String?,
    val stashedBranch: String,
    val stashedNode: ActionTreeElement,
    val timestamp: Long,
    val preview: StashPreview  // 预览信息
)

data class StashPreview(
    val cellsFilled: Int,
    val completionPercentage: Int,
    val lastAction: String
)

class StashManager {
    private val stashes = mutableMapOf<String, GameStash>()
    
    fun stash(name: String, description: String?): GameStash
    fun listStashes(): List<GameStash>
    fun applyStash(stashId: String, deleteAfterApply: Boolean = false)
    fun deleteStash(stashId: String)
}
```

#### 实现
```kotlin
fun stashCurrentWork(name: String, description: String? = null): GameStash {
    val currentBranch = branchManager.currentBranch 
        ?: throw IllegalStateException("No current branch")
    val currentNode = branchManager.currentNode 
        ?: throw IllegalStateException("No current node")
    
    val stash = GameStash(
        id = UUID.randomUUID().toString(),
        name = name,
        description = description,
        stashedBranch = currentBranch.id,
        stashedNode = currentNode,
        timestamp = System.currentTimeMillis(),
        preview = createStashPreview(currentNode)
    )
    
    stashManager.addStash(stash)
    return stash
}

fun applyStash(stashId: String, deleteAfterApply: Boolean = true) {
    val stash = stashManager.getStash(stashId) 
        ?: throw IllegalArgumentException("Stash not found")
    
    // 切换到 stash 保存的状态
    goToState(stash.stashedNode)
    
    if (deleteAfterApply) {
        stashManager.deleteStash(stashId)
    }
}
```

#### UI 设计
```
┌──────────────────────────────────────┐
│ Stashes                           [+]│
├──────────────────────────────────────┤
│                                      │
│ 📦 "Trying naked pairs"              │
│    Branch: main, 15 actions          │
│    45% complete • 2 hours ago        │
│    [Apply] [Delete]                  │
│                                      │
│ 📦 "Alternative approach"            │
│    Branch: branch-1, 8 actions       │
│    20% complete • 1 day ago          │
│    [Apply] [Delete]                  │
│                                      │
└──────────────────────────────────────┘
```

### 3. Tag（标签系统）⭐⭐⭐ 强烈推荐

比简单的 bookmark 更强大，支持多种类型的标签。

#### 数据结构
```kotlin
data class ActionTag(
    val id: String,
    val name: String,
    val nodeId: Int,
    val type: TagType,
    val color: Color? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

enum class TagType {
    MILESTONE,      // 🎯 里程碑（如"完成第一行"）
    BOOKMARK,       // 🔖 书签（保存位置）
    QUESTION,       // ❓ 疑问点（不确定是否正确）
    MISTAKE,        // ❌ 已知错误
    BREAKTHROUGH,   // 💡 突破点
    TECHNIQUE,      // 🎓 使用了某个技巧
    DECISION_POINT  // 🔀 重要决策点
}

class TagManager {
    private val tags = mutableMapOf<String, ActionTag>()
    
    fun addTag(nodeId: Int, type: TagType, name: String, description: String? = null): ActionTag
    fun removeTag(tagId: String)
    fun getTagsForNode(nodeId: Int): List<ActionTag>
    fun getAllTags(): List<ActionTag>
    fun getTagsByType(type: TagType): List<ActionTag>
}
```

#### UI 显示
```
Timeline with tags:
┌──────────────────────────────────────┐
│ Root                                 │
│  ↓                                   │
│ ○ Cell #1 → 5                        │
│    🎯 MILESTONE: "First cell!"       │
│  ↓                                   │
│ ○ Cell #2 → 3                        │
│  ↓                                   │
│ ○ Cell #5 → 7                        │
│    ❓ QUESTION: "Should this be 8?"  │
│  ↓                                   │
│ ○ Cell #8 → 4                        │
│    ❌ MISTAKE: "Wrong! Should be 9"  │
│  ↓                                   │
│ ○ Cell #8 → 9                        │
│    💡 BREAKTHROUGH: "Found pattern!" │
│  ↓                                   │
│ ○ Cell #12 → 6 (current)             │
└──────────────────────────────────────┘
```

#### 快速添加标签
```kotlin
@Composable
fun QuickTagMenu(
    node: ActionTreeElement,
    onTagAdded: (TagType, String) -> Unit
) {
    DropdownMenu(...) {
        TagMenuItem(
            icon = "🎯",
            label = "Milestone",
            onClick = { onTagAdded(TagType.MILESTONE, "Milestone") }
        )
        TagMenuItem(
            icon = "💡",
            label = "Breakthrough",
            onClick = { onTagAdded(TagType.BREAKTHROUGH, "Found key insight!") }
        )
        TagMenuItem(
            icon = "❓",
            label = "Question",
            onClick = { onTagAdded(TagType.QUESTION, "Not sure about this") }
        )
        TagMenuItem(
            icon = "🎓",
            label = "Technique Used",
            onClick = { /* Show technique picker */ }
        )
    }
}
```

### 4. Enhanced History View（历史视图增强）⭐⭐⭐

多维度查看游戏历史。

#### 视图模式
```kotlin
enum class HistoryViewMode {
    CHRONOLOGICAL,   // 按时间顺序（默认）
    BY_CELL,        // 按格子分组
    BY_REGION,      // 按区域（行/列/宫）
    BY_BRANCH,      // 按分支
    COMMITS_ONLY,   // 只看提交点
    TAGS_ONLY,      // 只看标签
    MISTAKES_ONLY   // 只看错误
}
```

#### 按格子查看
```
View Mode: BY_CELL
Selected Cell: #5

Cell #5 History:
├─ Action #12 (main branch)
│  Initial: 0 → 3
│  Time: 2:34 PM
│  💡 "Trying this value"
│
├─ Action #45 (main branch)
│  Modified: 3 → 7
│  Time: 2:38 PM
│  ❌ "Mistake, correcting"
│
└─ Current: 7
   Status: ✓ Correct
   Time: 2:40 PM
```

#### 按区域查看
```
View Mode: BY_REGION
Selected: Row 1

Row 1 Operations:
┌───┬───┬───┬───┬───┬───┬───┬───┬───┐
│ 5 │ 3 │ 7 │ 9 │ 4 │ 1 │ 6 │ 2 │ 8 │
└───┴───┴───┴───┴───┴───┴───┴───┴───┘
  #3  #5  #7  #12 #15 #22 #28 #34 #40
  ↑   ↑   ↑   ↑   ↑   ↑   ↑   ↑   ↑
Action numbers (click to jump)

Completed: 3:15 PM
Total time: 8 minutes
Mistakes: 1 (Cell #3)
```

### 5. Cell Blame（格子追溯）⭐⭐

类似 `git blame`，查看某个格子的完整填写历史。

#### 实现
```kotlin
data class CellHistory(
    val cellId: Int,
    val operations: List<CellOperation>,
    val finalValue: Int,
    val isCorrect: Boolean
)

data class CellOperation(
    val actionId: Int,
    val branch: String,
    val timestamp: Long,
    val operation: OperationType,
    val oldValue: Int,
    val newValue: Int,
    val reason: String?  // 如果有 commit message
)

enum class OperationType {
    FILL,      // 填入值
    MODIFY,    // 修改值
    CLEAR,     // 清空
    NOTE_ADD,  // 添加笔记
    NOTE_REMOVE // 删除笔记
}

fun getCellHistory(cellId: Int): CellHistory {
    val cell = sudoku.getCell(cellId)
    val operations = mutableListOf<CellOperation>()
    
    // 遍历整个 ActionTree，找到所有涉及这个格子的操作
    actionTree.forEach { node ->
        if (node.action.cell.id == cellId) {
            operations.add(
                CellOperation(
                    actionId = node.id,
                    branch = findBranchForNode(node)?.name ?: "unknown",
                    timestamp = node.timestamp,
                    operation = determineOperationType(node.action),
                    oldValue = /* ... */,
                    newValue = /* ... */,
                    reason = node.commitMessage
                )
            )
        }
    }
    
    return CellHistory(
        cellId = cellId,
        operations = operations.sortedBy { it.timestamp },
        finalValue = cell.currentValue,
        isCorrect = cell.isCorrect()
    )
}
```

#### UI 展示
```
┌─────────────────────────────────────────┐
│ Cell #5 History                         │
├─────────────────────────────────────────┤
│                                         │
│ 📅 Action #12 • main • 2:34 PM          │
│    0 → 3                                │
│    💭 "Naked single in row 2"           │
│                                         │
│ 📅 Action #23 • main • 2:36 PM          │
│    3 → 0 (cleared)                      │
│    💭 "Wait, this conflicts with col 5" │
│                                         │
│ 📅 Action #45 • main • 2:38 PM          │
│    0 → 7                                │
│    💭 "Corrected value"                 │
│                                         │
│ 📅 Action #67 • branch-1 • 3:15 PM      │
│    7 → 8 (different branch)             │
│    ❌ Led to mistake                    │
│                                         │
│ ✅ Final: 7 (Correct)                   │
│                                         │
│ [Close] [View in Timeline]              │
└─────────────────────────────────────────┘
```

### 6. Reflog（引用日志）⭐⭐

记录所有分支切换和状态变化，防止意外丢失。

#### 数据结构
```kotlin
data class ActionRefLog(
    val id: String,
    val timestamp: Long,
    val operation: RefLogOperation,
    val fromBranch: String?,
    val toBranch: String?,
    val fromNodeId: Int,
    val toNodeId: Int,
    val description: String
)

enum class RefLogOperation {
    CHECKOUT,           // 切换分支
    BRANCH_CREATE,      // 创建分支
    BRANCH_DELETE,      // 删除分支
    BRANCH_RENAME,      // 重命名分支
    REVERT,            // 回退操作
    STASH_SAVE,        // 保存 stash
    STASH_APPLY,       // 应用 stash
    RESET,             // 重置到某个状态
    COMMIT            // 创建提交点
}

class RefLogManager {
    private val logs = mutableListOf<ActionRefLog>()
    private val maxSize = 100  // 最多保留 100 条记录
    
    fun log(operation: RefLogOperation, details: String)
    fun getRecent(count: Int = 20): List<ActionRefLog>
    fun findDeletedBranch(branchName: String): ActionRefLog?
}
```

#### 使用场景
**场景 1：误删分支**
```
用户不小心删除了 "important-try" 分支

查看 Reflog:
┌─────────────────────────────────────────┐
│ Recent Operations                       │
├─────────────────────────────────────────┤
│ 2 min ago: BRANCH_DELETE                │
│   Deleted branch "important-try"        │
│   Last node: #234                       │
│   [Restore] ← 点击恢复！                │
│                                         │
│ 5 min ago: CHECKOUT                     │
│   main → branch-1                       │
│   Node #123 → #145                      │
└─────────────────────────────────────────┘
```

**场景 2：找回之前的状态**
```
用户记不清之前在哪个节点了

查看 Reflog:
10:30 AM - Checked out to node #45 (branch: main)
10:35 AM - Created commit "Trying pattern A"
10:40 AM - Reverted to node #30
10:45 AM - Created branch "alternative"
10:50 AM - Checked out to branch "alternative"
```

#### UI 设计
```kotlin
@Composable
fun RefLogScreen(
    logs: List<ActionRefLog>,
    onRestore: (ActionRefLog) -> Unit
) {
    LazyColumn {
        items(logs) { log ->
            RefLogItem(
                log = log,
                onRestore = { onRestore(log) }
            )
        }
    }
}

@Composable
fun RefLogItem(log: ActionRefLog, onRestore: () -> Unit) {
    Card(modifier = Modifier.padding(8.dp)) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    log.description,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    formatTimestamp(log.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (log.operation in listOf(
                RefLogOperation.BRANCH_DELETE,
                RefLogOperation.RESET
            )) {
                TextButton(onClick = onRestore) {
                    Text("Restore")
                }
            }
        }
    }
}
```

### 7. Git Reset（重置）⭐

快速重置到某个状态（类似 `git reset`）。

#### 三种模式
```kotlin
enum class ResetMode {
    SOFT,    // 只移动 HEAD，保留后续操作为"未提交"
    MIXED,   // 移动 HEAD，清除后续操作（默认）
    HARD     // 移动 HEAD，完全删除后续操作
}

fun reset(targetNode: ActionTreeElement, mode: ResetMode) {
    when (mode) {
        ResetMode.SOFT -> {
            // 移动当前节点，但保留后续操作在树中
            currentNode = targetNode
            // 后续操作变成"未附加"状态，可以选择重新应用
        }
        ResetMode.MIXED -> {
            // 移动当前节点，创建新分支保存原来的工作
            val oldBranch = currentBranch
            checkoutToNode(targetNode)  // 这会创建新分支
        }
        ResetMode.HARD -> {
            // 强制移动到目标节点，但保留在 reflog 中以防万一
            refLogManager.log(
                RefLogOperation.RESET,
                "Reset to node #${targetNode.id} (HARD)"
            )
            goToState(targetNode)
        }
    }
}
```

### 功能优先级总结

| 功能 | 优先级 | 实用性 | 实现难度 | 推荐指数 |
|------|--------|--------|----------|----------|
| **Commit Message** | 🔴 高 | ⭐⭐⭐⭐⭐ | 简单 | 必须实现 |
| **Tag System** | 🔴 高 | ⭐⭐⭐⭐⭐ | 中等 | 必须实现 |
| **Cell Blame** | 🟡 中 | ⭐⭐⭐⭐ | 中等 | 强烈推荐 |
| **Enhanced History** | 🟡 中 | ⭐⭐⭐⭐ | 中等 | 强烈推荐 |
| **Stash** | 🟡 中 | ⭐⭐⭐ | 中等 | 推荐 |
| **Reflog** | 🟢 低 | ⭐⭐⭐ | 简单 | 推荐（安全网） |
| **Reset** | 🟢 低 | ⭐⭐ | 简单 | 可选 |

### 推荐的实现顺序

**MVP (最小可行产品)**：
1. 基础分支系统 (Checkout, Revert, View)
2. Commit Message
3. 简单 Tag (只有 Bookmark 类型)

**V1.0 (完整版本)**：
4. 完整 Tag System (多种类型)
5. Cell Blame
6. Enhanced History View
7. Reflog

**V2.0 (高级功能)**：
8. Stash
9. Branch Comparison
10. Selective Apply

## 关于分支合并的说明

### 为什么不支持自动合并？

与 Git 代码合并不同，数独游戏的操作合并面临**根本性的逻辑冲突**：

#### 问题示例
```
Branch A: Cell #5 填 3
Branch B: Cell #5 填 7

合并后 Cell #5 应该是什么？
```

在 Git 中，两个分支修改同一行代码会标记为冲突，需要人工解决。但在数独中：
- 一个格子**只能有一个值**
- 没有"保留双方修改"的概念
- 无法像代码那样显示冲突标记

#### 更复杂的问题
```
Branch A: 
  Cell #1 = 5
  Cell #2 = 3  (因为 #1=5 所以推导出)
  Cell #5 = 7  (因为 #2=3 所以推导出)

Branch B:
  Cell #1 = 5
  Cell #2 = 8  (同样的起点，不同的选择)
  Cell #6 = 2  (因为 #2=8 所以推导出)

即使只有 Cell #2 冲突，但后续的所有推导都基于这个选择，
无法简单地"合并"两个分支的后续操作。
```

### 替代方案：分支对比 + 选择性应用

#### 1. Branch Comparison（分支对比）

显示两个分支的完整差异，帮助用户理解不同选择的后果：

```kotlin
data class BranchComparison(
    val commonBase: ActionTreeElement,           // 共同起点
    val divergePoint: ActionTreeElement,         // 分叉点
    val branchAUnique: List<ActionTreeElement>,  // A 分支独有
    val branchBUnique: List<ActionTreeElement>,  // B 分支独有
    val cellConflicts: List<CellConflict>        // 冲突的格子
)

data class CellConflict(
    val cellId: Int,
    val inBranchA: CellState,
    val inBranchB: CellState,
    val conflictType: ConflictType  // VALUE, NOTE, BOTH
)

enum class ConflictType {
    VALUE,      // 两个分支填了不同的值
    NOTE,       // 两个分支的笔记不同
    BOTH        // 值和笔记都不同
}
```

**UI 展示**：
```
┌─────────────────────────────────────────────────────┐
│ Branch Comparison: main vs branch-try-7             │
├─────────────────────────────────────────────────────┤
│                                                     │
│ 📍 Diverged at: Cell #5 (Action #3)                │
│ ⏱️  main: 15 actions after divergence               │
│ ⏱️  branch-try-7: 8 actions after divergence        │
│                                                     │
│ ⚠️  Conflicts: 3 cells                              │
│                                                     │
│ Cell #5:                                            │
│   main: 3 ✓ (leads to solution)                    │
│   branch-try-7: 7 ✗ (leads to mistake)             │
│                                                     │
│ Cell #8:                                            │
│   main: 9                                           │
│   branch-try-7: 2                                   │
│                                                     │
│ 💡 Recommendation:                                  │
│   Branch "main" seems more promising                │
│   (15 actions, 85% completion, no mistakes)         │
│                                                     │
│ [Switch to main] [Switch to branch-try-7] [Close]   │
└─────────────────────────────────────────────────────┘
```

#### 2. Selective Apply（选择性应用操作）

允许用户查看另一个分支，**挑选没有冲突的操作**应用到当前分支：

```kotlin
fun selectiveApplyFromBranch(
    targetBranch: ActionBranch,
    selectedNodes: List<ActionTreeElement>
): SelectiveApplyResult {
    val safeToApply = mutableListOf<ActionTreeElement>()
    val conflicts = mutableListOf<ActionTreeElement>()
    
    selectedNodes.forEach { node ->
        val action = node.action
        val cell = action.cell
        
        when {
            // 格子是空的，可以安全应用
            cell.currentValue == Cell.EMPTYVAL -> {
                safeToApply.add(node)
            }
            // 格子已经有值了
            cell.currentValue != Cell.EMPTYVAL -> {
                // 检查是否是相同的操作
                if (action is SolveAction && 
                    cell.currentValue == action.cell.currentValue) {
                    // 相同操作，跳过
                    Log.d("SelectiveApply", "Skipping duplicate action")
                } else {
                    // 冲突，需要用户决定
                    conflicts.add(node)
                }
            }
        }
    }
    
    return SelectiveApplyResult(safeToApply, conflicts)
}
```

**使用流程**：
```
1. 用户在 ActionTree 中切换到 branch-1 查看
2. 长按某个操作节点，选择"Apply to main branch"
3. 可以多选多个操作
4. 系统分析：
   ✓ Cell #6 填 2 - 可以应用（格子为空）
   ✓ Cell #9 填 4 - 可以应用（格子为空）
   ✗ Cell #5 填 7 - 冲突（main 中已经是 3）
5. 显示确认对话框：
   "2 operations can be applied safely.
    1 operation conflicts with current branch.
    Apply safe operations?"
```

#### 3. 笔记合并（可行的特殊情况）

对于**笔记（Notes）**，如果操作的是不同的格子，是可以合并的：

```kotlin
fun mergeNotesFromBranch(
    sourceBranch: ActionBranch,
    targetBranch: ActionBranch
): NoteMergeResult {
    val sourceNotes = extractAllNotes(sourceBranch)
    val targetNotes = extractAllNotes(targetBranch)
    
    val mergedNotes = mutableMapOf<Int, Set<Int>>()
    val conflicts = mutableListOf<NoteConflict>()
    
    // 合并不同格子的笔记
    sourceNotes.forEach { (cellId, notes) ->
        if (cellId !in targetNotes) {
            // 目标分支没有这个格子的笔记，直接添加
            mergedNotes[cellId] = notes
        } else {
            // 两个分支都有这个格子的笔记，需要决定如何合并
            val targetCellNotes = targetNotes[cellId]!!
            if (notes == targetCellNotes) {
                // 笔记相同，保持
                mergedNotes[cellId] = notes
            } else {
                // 笔记不同，提供选项
                conflicts.add(NoteConflict(cellId, notes, targetCellNotes))
            }
        }
    }
    
    return NoteMergeResult(mergedNotes, conflicts)
}
```

### 实现优先级建议

根据实用性排序：

1. **分支对比（Branch Comparison）** - 高优先级 ⭐⭐⭐
   - 帮助用户理解不同选择
   - 纯查看功能，无风险
   - 实现相对简单

2. **选择性应用（Selective Apply）** - 中优先级 ⭐⭐
   - 有实际使用场景
   - 需要仔细处理冲突
   - 实现较复杂

3. **笔记合并（Note Merge）** - 低优先级 ⭐
   - 使用场景较少
   - 笔记不是核心功能
   - 可以后续添加

### 总结：分支哲学的不同

| 维度 | Git (代码) | SudoQ (数独) |
|------|-----------|-------------|
| **合并可行性** | ✅ 可以自动合并 | ❌ 无法自动合并 |
| **冲突解决** | ✅ 人工选择保留哪段代码 | ❌ 一个格子只能有一个值 |
| **分支目的** | 并行开发不同功能 | 尝试不同的解题路径 |
| **最佳实践** | 定期合并避免大冲突 | 保持多个独立的尝试 |
| **价值** | 整合团队工作 | 探索和回溯 |

**结论**：在数独游戏中，分支的价值不在于"合并"，而在于：
1. 🔀 **探索多条路径** - 尝试不同的填数策略
2. 🔍 **对比分析** - 查看哪个分支更接近正确答案
3. ↩️ **安全回退** - 随时回到任何历史状态
4. 🎯 **学习工具** - 理解为什么某个选择导致错误

## 总结

这个设计方案解决了当前 ActionTree 的核心问题：

✅ **永不丢失历史** - 所有操作分支都被保存
✅ **灵活切换** - 可以在任意状态间自由跳转
✅ **清晰意图** - 三种模式明确用户的不同需求
✅ **Git 哲学** - 熟悉的概念模型，易于理解
✅ **可扩展性** - 支持未来添加对比、选择性应用等高级功能
✅ **适配场景** - 认识到数独和代码的本质区别，不强行套用不适用的功能

通过借鉴 Git 的分支管理理念，同时认识到数独游戏的特殊性，我们将 ActionTree 从一个简单的历史记录工具升级为一个强大的时间旅行和状态探索系统。
