#!/usr/bin/env python3
"""
XML字符串文件排序脚本
用于对Android/Compose资源文件中的string元素按name属性进行排序
"""

import xml.etree.ElementTree as ET
import argparse
import os
import sys
from typing import List, Tuple


def parse_xml_file(file_path: str) -> Tuple[ET.Element, str]:
    """
    解析XML文件并返回根元素和原始编码信息
    
    Args:
        file_path: XML文件路径
        
    Returns:
        tuple: (根元素, 编码信息)
    """
    try:
        # 读取文件内容以获取编码信息
        with open(file_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 提取编码信息
        encoding = 'utf-8'  # 默认编码
        if '<?xml' in content:
            xml_declaration = content.split('?>')[0] + '?>'
            if 'encoding=' in xml_declaration:
                encoding_part = xml_declaration.split('encoding=')[1]
                if encoding_part.startswith('"'):
                    encoding = encoding_part.split('"')[1]
                elif encoding_part.startswith("'"):
                    encoding = encoding_part.split("'")[1]
        
        # 解析XML
        tree = ET.parse(file_path)
        root = tree.getroot()
        
        return root, encoding
        
    except ET.ParseError as e:
        print(f"XML解析错误 {file_path}: {e}")
        sys.exit(1)
    except FileNotFoundError:
        print(f"文件未找到: {file_path}")
        sys.exit(1)
    except Exception as e:
        print(f"读取文件时出错 {file_path}: {e}")
        sys.exit(1)


def sort_string_elements(root: ET.Element) -> List[ET.Element]:
    """
    对string元素按name属性进行排序
    
    Args:
        root: XML根元素
        
    Returns:
        排序后的string元素列表
    """
    # 找到所有string元素
    string_elements = root.findall('string')
    
    if not string_elements:
        print("警告: 未找到任何string元素")
        return []
    
    # 按name属性排序
    def get_name_key(element):
        name = element.get('name', '')
        return name.lower()  # 不区分大小写排序
    
    sorted_elements = sorted(string_elements, key=get_name_key)
    
    return sorted_elements


def write_sorted_xml(file_path: str, root: ET.Element, sorted_elements: List[ET.Element], encoding: str):
    """
    将排序后的元素写入XML文件
    
    Args:
        file_path: 输出文件路径
        root: XML根元素
        sorted_elements: 排序后的string元素列表
        encoding: 文件编码
    """
    # 清除原有的string元素
    for elem in root.findall('string'):
        root.remove(elem)
    
    # 添加排序后的元素
    for elem in sorted_elements:
        root.append(elem)
    
    # 创建新的树
    tree = ET.ElementTree(root)
    
    # 写入文件
    try:
        # 先写入临时文件
        temp_file = file_path + '.tmp'
        
        # 写入XML声明和内容
        with open(temp_file, 'w', encoding='utf-8') as f:
            # 写入XML声明
            if encoding.lower() == 'utf-8':
                f.write("<?xml version='1.0' encoding='utf-8'?>\n")
            else:
                f.write(f'<?xml version="1.0" encoding="{encoding}"?>\n')
            
            # 写入根元素开始标签
            f.write('<resources>\n')
            
            # 写入排序后的string元素
            for elem in sorted_elements:
                name = elem.get('name', '')
                text = elem.text or ''
                
                # 转义特殊字符
                text = text.replace('&', '&amp;')
                text = text.replace('<', '&lt;')
                text = text.replace('>', '&gt;')
                text = text.replace('"', '&quot;')
                
                # 恢复一些常见的转义
                text = text.replace('&amp;lt;', '&lt;')
                text = text.replace('&amp;gt;', '&gt;')
                text = text.replace('&amp;quot;', '&quot;')
                
                f.write(f'    <string name="{name}">{text}</string>\n')
            
            f.write('</resources>\n')
        
        # 替换原文件
        os.replace(temp_file, file_path)
        
    except Exception as e:
        print(f"写入文件时出错 {file_path}: {e}")
        # 清理临时文件
        if os.path.exists(temp_file):
            os.remove(temp_file)
        sys.exit(1)


def create_backup(file_path: str):
    """
    创建文件备份
    
    Args:
        file_path: 要备份的文件路径
    """
    backup_path = file_path + '.backup'
    try:
        with open(file_path, 'r', encoding='utf-8') as src:
            with open(backup_path, 'w', encoding='utf-8') as dst:
                dst.write(src.read())
        print(f"已创建备份: {backup_path}")
    except Exception as e:
        print(f"创建备份失败: {e}")


def main():
    """主函数"""
    parser = argparse.ArgumentParser(description='对XML字符串文件中的string元素进行排序')
    parser.add_argument('files', nargs='+', help='要排序的XML文件路径')
    parser.add_argument('--no-backup', action='store_true', help='不创建备份文件')
    parser.add_argument('--dry-run', action='store_true', help='只显示排序结果，不修改文件')
    
    args = parser.parse_args()
    
    for file_path in args.files:
        print(f"\n处理文件: {file_path}")
        
        # 检查文件是否存在
        if not os.path.exists(file_path):
            print(f"错误: 文件不存在 {file_path}")
            continue
        
        # 解析XML文件
        root, encoding = parse_xml_file(file_path)
        
        # 排序string元素
        sorted_elements = sort_string_elements(root)
        
        if not sorted_elements:
            print("跳过: 没有找到string元素")
            continue
        
        print(f"找到 {len(sorted_elements)} 个string元素")
        
        if args.dry_run:
            print("排序后的name属性顺序:")
            for i, elem in enumerate(sorted_elements, 1):
                name = elem.get('name', '')
                print(f"  {i:3d}. {name}")
        else:
            # 创建备份
            if not args.no_backup:
                create_backup(file_path)
            
            # 写入排序后的文件
            write_sorted_xml(file_path, root, sorted_elements, encoding)
            print(f"排序完成: {file_path}")


if __name__ == '__main__':
    main()
