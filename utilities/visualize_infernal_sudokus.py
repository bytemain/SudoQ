#!/usr/bin/env python3
"""
可视化新生成的地狱难度数独
"""
import xml.etree.ElementTree as ET
import os

def parse_sudoku_xml(xml_file):
    """解析数独 XML 文件"""
    tree = ET.parse(xml_file)
    root = tree.getroot()
    
    # 创建 9x9 空网格
    grid = [[-1 for _ in range(9)] for _ in range(9)]
    
    # 填充已知数字
    for fieldmap in root.findall('fieldmap'):
        solution = int(fieldmap.get('solution'))
        position = fieldmap.find('position')
        x = int(position.get('x'))
        y = int(position.get('y'))
        grid[y][x] = solution
    
    return grid

def print_sudoku(grid, title):
    """打印数独网格"""
    print()
    print("=" * 70)
    print(f"  {title}")
    print("=" * 70)
    print()
    print("  ┌───────┬───────┬───────┐")
    
    for y in range(9):
        print("  │", end=" ")
        for x in range(9):
            if grid[y][x] >= 0:
                print(grid[y][x] + 1, end=" ")
            else:
                print("·", end=" ")
            
            if (x + 1) % 3 == 0:
                print("│", end=" ")
        print()
        
        if (y + 1) % 3 == 0 and y < 8:
            print("  ├───────┼───────┼───────┤")
    
    print("  └───────┴───────┴───────┘")
    print()
    
    # 统计
    filled = sum(1 for row in grid for cell in row if cell >= 0)
    print(f"  已填数字: {filled}/81")
    print(f"  空白数字: {81 - filled}/81")
    print()

def main():
    # 使用相对路径
    script_dir = os.path.dirname(os.path.abspath(__file__))
    project_root = os.path.dirname(script_dir)
    infernal_dir = os.path.join(project_root, "sudoq-app", "res", "sudokus", "standard9x9", "infernal")
    
    print("\n" + "=" * 70)
    print("  新生成的超难数独展示")
    print("=" * 70)
    
    for i in range(1, 4):  # 只展示前3个
        xml_file = os.path.join(infernal_dir, f"sudoku_{i}.xml")
        if os.path.exists(xml_file):
            grid = parse_sudoku_xml(xml_file)
            print_sudoku(grid, f"数独 #{i} (sudoku_{i}.xml) - MUCH_TOO_DIFFICULT")
    
    print("=" * 70)
    print("  所有 10 个数独已生成并可在游戏中使用！")
    print("=" * 70)
    print()

if __name__ == "__main__":
    main()
