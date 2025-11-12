# 缺失的数独提示技巧

## 已实现的技巧

### 一、Singles（单数技巧）
- ✅ **Full house / Last digit**（整行列宫的最后一个位置和数字）- `LastDigit`
- ✅ **Hidden single**（行列宫中只有一个位置可以填写该数字）- `HiddenSingle`
- ✅ **Naked single**（格子里只有一个数字可以填）- `LastCandidate`

### 二、Intersections（交叉消除 / Locked Candidates）
- ✅ **Pointing**（宫区块数对 - 行列消除法）- `LockedCandidatesExternal`
- ✅ **Claiming**（宫区块数对 - 反向）- `LockedCandidatesExternal`

### 三、数对（Subsets）

#### Hidden Subsets（隐性数组）
- ✅ **Hidden Pair**（隐性数对）- `HiddenPair`
- ✅ **Hidden Triple**（隐性三数组）- `HiddenTriple`
- ✅ **Hidden Quadruple**（隐性四数组）- `HiddenQuadruple`
- ✅ 更多隐性数组（5-13元组）

#### Naked Subsets（显性数组）
- ✅ **Naked Pair**（显性数对）- `NakedPair`
- ✅ **Naked Triple**（显性三数组）- `NakedTriple`
- ✅ **Naked Quadruple**（显性四数组）- `NakedQuadruple`
- ✅ 更多显性数组（5-13元组）

### 四、Fish（鱼类技巧）
- ✅ **X-Wing**（X翼）- `XWing`

---

## 缺失的技巧

### 一、Fish（鱼类技巧）- 进阶
- ❌ **Swordfish**（剑鱼）
  - 描述：类似X-Wing，但使用三组单元格而非两组
  - 难度：中高级
  - 原理：在三行（或三列）中，某个数字只出现在同样的三列（或三行）中，可以消除这三列（或三行）中其他位置的该数字

### 二、Wings（翼类技巧）
- ❌ **Y-Wing**（Y翼）
  - 描述：基于三个角的技巧，而不是X-Wing的四个角
  - 难度：中高级
  - 原理：找到一个"枢纽"单元格（有两个候选数AB）和两个"翼"单元格（分别有候选数AC和BC），可以消除同时看到两个翼的单元格中的候选数C

- ❌ **XYZ-Wing**（XYZ翼）
  - 描述：Y-Wing的变体，枢纽单元格有三个候选数
  - 难度：高级
  - 原理：类似Y-Wing但更复杂

### 三、Coloring & Chains（着色和链）
- ❌ **Simple Coloring**（简单着色）
  - 描述：基于强弱链的着色技巧
  - 难度：高级
  - 原理：使用强链关系对候选数进行着色，找到矛盾

- ❌ **X-Chains**（X链）
  - 描述：基于强弱链的技巧
  - 难度：高级

- ❌ **XY-Chains**（XY链）
  - 描述：基于双候选数单元格的链
  - 难度：高级

### 四、其他高级技巧
- ❌ **Finned X-Wing**（带鳍X翼）
  - 描述：X-Wing的变体，允许额外的候选数
  - 难度：高级

- ❌ **Finned Swordfish**（带鳍剑鱼）
  - 描述：Swordfish的变体
  - 难度：高级

---

## 优先实现计划

根据技巧的实用性和难度，建议按以下顺序实现：

1. **优先级 1**（最实用、相对容易实现）：
   - Swordfish（剑鱼）- X-Wing的自然扩展
   - Y-Wing（Y翼）- 非常常用的技巧

2. **优先级 2**（实用但较复杂）：
   - XYZ-Wing（XYZ翼）
   - Simple Coloring（简单着色）

3. **优先级 3**（高级技巧，较少使用）：
   - X-Chains
   - XY-Chains
   - Finned Fish技巧

---

## 实现说明

当前已有的代码结构：
- `HintTypes.kt` - 定义所有提示类型的枚举
- `SolvingAssistant.kt` - 协调所有Helper的主类
- `solver/helper/` - 各种Helper实现
- `view/Hints/` - 各种提示的视图实现

实现新技巧需要：
1. 在`HintTypes.kt`中添加新的枚举值
2. 创建新的Helper类（如`YWingHelper.kt`、`SwordfishHelper.kt`）
3. 在`SolvingAssistant.kt`中注册新的Helper
4. 创建对应的View类用于显示提示
5. 添加相应的字符串资源

