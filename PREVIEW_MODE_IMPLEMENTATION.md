# Preview Mode Implementation

## Overview

Replaced the confusing "View Only" mode with an intuitive "Preview" dialog that shows the game state inline with navigation controls. This significantly improves the user experience when exploring historical states in the ActionTree.

## Problem Statement

**Original Issue:**
- The old "View Only" mode was confusing - users had to close the ActionTree to see what the mode actually did
- No easy way to navigate through historical states once in view mode
- The interaction pattern was unclear and required multiple steps

**User Feedback:**
> "感觉 view only 可以改成在 Dialog 里预览。。。因为现在这个交互并不好，我还得返回关闭 actions tree 才能看出来，所以应该叫 preview？然后进一个页面，可以快速导航有左右按钮切换"

Translation: "View only should be changed to preview in a dialog... because the current interaction is not good, I have to go back and close the actions tree to see what it does, so it should be called preview? Then enter a page where you can quickly navigate with left/right buttons to switch"

## Implementation

### 1. Renamed ActionMode.VIEW to ActionMode.PREVIEW

**File:** `ActionModeDialog.kt`

```kotlin
enum class ActionMode {
    CHECKOUT, // Checkout to this node (create new branch)
    REVERT, // Revert to this node (add reverse actions)
    PREVIEW // Preview this state in a dialog with navigation
}
```

Updated the radio button option:
- Title: "View Only" → "Preview"
- Description: "Temporarily view this state without any modifications" → "View this state in a dialog with navigation controls."

### 2. Created PreviewDialog Component

**File:** `PreviewDialog.kt` (NEW, ~270 lines)

**Key Features:**

#### Dialog Structure
- Full-screen dialog with rounded corners
- Material3 design with proper elevation and colors
- Platform-agnostic width for better tablet support

#### Header Section
```kotlin
TopAppBar(
    title = { Text(getNodeDescription(currentPreviewNode)) },
    navigationIcon = { 
        IconButton(onClick = onDismiss) {
            Icon(Icons.Default.Close, "Close preview")
        }
    }
)
```

#### Info Banner
Shows "Previewing state - changes not saved" with yellow warning color:
```kotlin
Surface(
    color = MaterialTheme.colorScheme.tertiaryContainer,
    modifier = Modifier.fillMaxWidth()
) {
    Row(...) {
        Icon(Icons.Default.Info, ...)
        Text("Previewing state - changes not saved")
    }
}
```

#### Game State Display
Currently shows a placeholder for SudokuBoard integration:
```kotlin
Box(
    modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .background(MaterialTheme.colorScheme.surfaceVariant)
) {
    // TODO: Integrate actual SudokuBoard here
    Text("Preview of game state at this point")
}
```

#### Navigation Controls
Bottom row with Previous/Next buttons:
```kotlin
Row(...) {
    // Previous button (disabled if at root)
    OutlinedButton(
        onClick = { ... },
        enabled = hasPrevious
    ) {
        Icon(Icons.AutoMirrored.Default.ArrowBack, ...)
        Text("Previous")
    }
    
    // Next button (disabled if at latest)
    Button(
        onClick = { ... },
        enabled = hasNext
    ) {
        Text("Next")
        Icon(Icons.AutoMirrored.Default.ArrowForward, ...)
    }
}
```

#### Action Buttons
"Cancel" and "Checkout Here" buttons:
```kotlin
Row(...) {
    OutlinedButton(onClick = onDismiss) { 
        Text("Cancel") 
    }
    
    Button(onClick = { onCheckout(currentPreviewNode) }) { 
        Text("Checkout Here") 
    }
}
```

### 3. Lifecycle Management

**Automatic View Mode Entry/Exit:**
```kotlin
// Enter preview mode when dialog opens
LaunchedEffect(startNode) { 
    gameStateHandler.viewNode(startNode) 
}

// Clean up when dialog closes
DisposableEffect(Unit) { 
    onDispose { 
        gameStateHandler.exitViewMode() 
    } 
}
```

### 4. Navigation Logic

**Find Previous Node (Parent):**
```kotlin
val hasPrevious = currentPreviewNode.parent != null

fun navigatePrevious() {
    currentPreviewNode.parent?.let { parent ->
        navigateToNode(parent)
    }
}
```

**Find Next Node (First Child or Sibling):**
```kotlin
fun findNextNode(node: ActionTreeElement): ActionTreeElement? {
    // Strategy 1: Try first child (move forward in history)
    val firstChild = node.children.firstOrNull()
    if (firstChild != null) return firstChild
    
    // Strategy 2: No children means we're at a leaf
    return null
}

val hasNext = findNextNode(currentPreviewNode) != null
```

### 5. Integration with ActionTreeScreen

**File:** `ActionTreeScreen.kt`

**Added State Variable:**
```kotlin
var showPreviewDialog by remember { mutableStateOf<ActionTreeElement?>(null) }
```

**Updated ActionMode Handler:**
```kotlin
ActionMode.PREVIEW -> {
    showPreviewDialog = element
}
```

**Wire PreviewDialog:**
```kotlin
// Preview dialog
showPreviewDialog?.let { element ->
    PreviewDialog(
        startNode = element,
        gameStateHandler = gameStateHandler,
        onCheckout = { node ->
            showPreviewDialog = null
            gameStateHandler.checkoutToNode(node)
        },
        onDismiss = { showPreviewDialog = null }
    )
}
```

**Removed Old View Mode Banner:**
Deleted the old inline banner that showed "View Mode - Changes not saved" with an Exit button. This is now handled within PreviewDialog itself.

## Helper Functions

### getNodeDescription()
Provides human-readable descriptions for different action types:
```kotlin
private fun getNodeDescription(node: ActionTreeElement): String {
    val action = node.action
    return when (action) {
        is CompoundAction ->
            "${action.type.icon} ${action.type.displayName}: ${action.description ?: action.getActionSummary()}"
        is SolveAction -> {
            val cellId = action.cell.id
            val oldValue = action.cell.currentValue - action.diff
            val newValue = action.cell.currentValue
            "Solve Cell #$cellId: $oldValue → $newValue"
        }
        is NoteAction -> {
            val cellId = action.cell.id
            "Note in Cell #$cellId: ${action.actionType} ${action.diff}"
        }
        else -> "Action #${node.id}"
    }
}
```

### getNodeDepth()
Calculates the depth of a node in the tree:
```kotlin
private fun getNodeDepth(node: ActionTreeElement): Int {
    var depth = 0
    var current: ActionTreeElement? = node
    while (current?.parent != null) {
        depth++
        current = current.parent
    }
    return depth
}
```

## User Flow

### Old Flow (View Mode):
1. User opens ActionTree
2. Clicks on historical node
3. Selects "View Only"
4. **Must close ActionTree to see the game state**
5. Back button to exit view mode
6. Reopen ActionTree to continue

### New Flow (Preview Mode):
1. User opens ActionTree
2. Clicks on historical node
3. Selects "Preview"
4. **Dialog opens immediately showing the game state**
5. Use Previous/Next buttons to navigate history
6. Click "Checkout Here" to create branch or "Cancel" to exit
7. Returns to ActionTree seamlessly

## Benefits

1. **Inline Preview:** No need to close ActionTree to see the state
2. **Easy Navigation:** Previous/Next buttons for quick history exploration
3. **Clear Labeling:** "Preview" is more intuitive than "View Only"
4. **Better Context:** Shows node description in header
5. **Visual Feedback:** Yellow info banner makes it clear this is temporary
6. **Quick Checkout:** Easy to create branch from preview state
7. **Proper Cleanup:** Automatic view mode exit on dialog close

## Pending Work

### 1. Integrate Actual SudokuBoard ⏳
**Priority:** High

Currently shows a placeholder Box. Need to:
- Add SudokuBoard component to preview area
- Pass game instance or board state
- Handle read-only mode (disable user input)
- Ensure proper refresh when navigating between nodes

**Challenge:** SudokuBoard likely expects a Game instance. May need refactoring to work in preview mode.

### 2. Multi-Select for Squash 🔄
**Priority:** Medium

SquashDialog exists but needs UI integration:
- Add long-press gesture to start selection mode
- Show selection checkboxes in ActionTree
- Allow range selection (start → end)
- Show SquashDialog when range confirmed
- Wire to GameStateHandler.squashNodes()

### 3. Branch Persistence 📦
**Priority:** High

Branches are currently lost on app restart:
- Extend save format for ActionBranchBE data class
- Store branch metadata (id, name, head node id, timestamps)
- Store CompoundAction as nested XML
- Migration logic for old saves (auto-create main branch)

## Testing Checklist

- [x] Compile succeeds without errors
- [x] APK builds successfully
- [ ] Open ActionTree and click historical node
- [ ] Select "Preview" mode
- [ ] Verify dialog opens with correct node description
- [ ] Test Previous button navigation
- [ ] Test Next button navigation
- [ ] Verify buttons disable at boundaries (root/leaf)
- [ ] Test "Cancel" button closes dialog properly
- [ ] Test "Checkout Here" creates branch correctly
- [ ] Verify view mode exits automatically on dialog close
- [ ] Test with CompoundAction nodes
- [ ] Test with SolveAction nodes
- [ ] Test with NoteAction nodes

## Files Modified

1. **ActionModeDialog.kt**
   - Renamed `VIEW` to `PREVIEW` in enum
   - Updated display text and description

2. **ActionTreeScreen.kt**
   - Added `showPreviewDialog` state variable
   - Changed ActionMode.VIEW handling to ActionMode.PREVIEW
   - Wire PreviewDialog component
   - Removed old view mode banner

3. **PreviewDialog.kt** (NEW)
   - Complete dialog implementation (~270 lines)
   - Navigation controls
   - Lifecycle management
   - Helper functions

## Architecture Notes

**Composable Hierarchy:**
```
ActionTreeScreenWithBranches
├─ Scaffold (ActionTree)
├─ BranchPickerSheet (when showBranchPicker = true)
├─ CreateBranchDialog (when showCreateBranchDialog = true)
├─ ActionModeDialog (when showActionDialog != null)
└─ PreviewDialog (when showPreviewDialog != null)  ← NEW
```

**State Flow:**
```
User clicks node
    ↓
ActionModeDialog shows
    ↓
User selects PREVIEW
    ↓
showPreviewDialog = element
    ↓
PreviewDialog.LaunchedEffect
    ↓
gameStateHandler.viewNode(startNode)
    ↓
User navigates with buttons
    ↓
gameStateHandler.viewNode(newNode)
    ↓
User clicks "Checkout Here"
    ↓
onCheckout(currentPreviewNode)
    ↓
gameStateHandler.checkoutToNode(node)
    ↓
Dialog closes
    ↓
DisposableEffect cleanup
    ↓
gameStateHandler.exitViewMode()
```

## Known Issues

1. **Deprecation Warning:** `Divider()` in ActionModeDialog.kt should be changed to `HorizontalDivider()` (non-critical, Material3 API change)

2. **SudokuBoard Placeholder:** Preview shows placeholder text instead of actual board. Requires integration work.

3. **Next Navigation Limited:** Currently only finds first child. In a branching tree, there might be multiple next options. Consider:
   - Show branch selector when multiple children exist
   - Default to current branch's path
   - Add "Show All Children" option

## Performance Considerations

- **View Mode Overhead:** Each navigation call to `viewNode()` recalculates game state
- **Dialog Lifecycle:** LaunchedEffect and DisposableEffect ensure proper cleanup
- **State Preservation:** Original state is remembered and restored on cleanup

## Design Decisions

1. **Full-Screen Dialog:** Provides enough space to show both controls and game board
2. **Linear Navigation:** Previous/Next is simpler than tree-based navigation for preview
3. **Automatic Cleanup:** DisposableEffect ensures view mode always exits, even if dialog crashes
4. **Checkout Integration:** Quick access to create branch from preview state
5. **Material3 Styling:** Consistent with rest of app UI

---

**Implementation Date:** 2024 (current)
**Status:** ✅ Complete (pending SudokuBoard integration)
**Build Status:** ✅ Compiles successfully, APK generated
