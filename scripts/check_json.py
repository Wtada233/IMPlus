import json
import os
import sys

def validate_single_json(file_path):
    """验证单个 JSON 文件的语法。"""
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            json.load(f)
        return True
    except json.JSONDecodeError as e:
        print_json_error(file_path, e)
    except Exception as e:
        print(f"\n⚠️ 读取失败: {file_path}\n   原因: {str(e)}")
    return False

def print_json_error(file_path, e):
    """打印 JSON 格式错误的详细信息。"""
    print(f"\n❌ 语法错误: {file_path}")
    print(f"   原因: {e.msg}")
    print(f"   位置: 第 {e.lineno} 行, 第 {e.colno} 列")
    
    # 打印上下文代码
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
            start = max(0, e.lineno - 3)
            end = min(len(lines), e.lineno + 2)
            for i in range(start, end):
                prefix = ">> " if i == e.lineno - 1 else "   "
                print(f"   {i+1}: {lines[i].rstrip()}")
    except Exception:
        pass

def should_skip_dir(dirname):
    """判断是否需要跳过该目录。"""
    skip_dirs = {'.git', '.gradle', '.gradle_data', 'build'}
    return dirname in skip_dirs

def check_json_files(root_dir):
    """扫描目录下的所有 JSON 文件并验证其合法性。"""
    print(f"开始扫描目录: {os.path.abspath(root_dir)}")
    error_count = 0
    file_count = 0
    
    for root, dirs, files in os.walk(root_dir):
        # 过滤目录
        dirs[:] = [d for d in dirs if not should_skip_dir(d)]
        
        for file in files:
            if not file.endswith('.json'):
                continue
                
            file_count += 1
            if not validate_single_json(os.path.join(root, file)):
                error_count += 1

    print(f"\n扫描完成。共检测 {file_count} 个文件，发现 {error_count} 个错误。")
    return error_count > 0

if __name__ == "__main__":
    # 入口点：解析命令行参数并运行检查
    target = sys.argv[1] if len(sys.argv) > 1 else "."
    has_errors = check_json_files(target)
    sys.exit(1 if has_errors else 0)