import json
import os
import sys

def check_json_files(root_dir):
    print(f"开始扫描目录: {os.path.abspath(root_dir)}")
    error_count = 0
    file_count = 0
    
    for root, dirs, files in os.walk(root_dir):
        # 排除隐藏目录和 build 目录以提高速度
        if '.git' in dirs: dirs.remove('.git')
        if '.gradle' in dirs: dirs.remove('.gradle')
        if '.gradle_data' in dirs: dirs.remove('.gradle_data')
        if 'build' in dirs: dirs.remove('build')
        
        for file in files:
            if file.endswith('.json'):
                file_count += 1
                file_path = os.path.join(root, file)
                try:
                    with open(file_path, 'r', encoding='utf-8') as f:
                        json.load(f)
                except json.JSONDecodeError as e:
                    print(f"\n❌ 语法错误: {file_path}")
                    print(f"   原因: {e.msg}")
                    print(f"   位置: 第 {e.lineno} 行, 第 {e.colno} 列")
                    # 打印错误行附近的内容
                    try:
                        with open(file_path, 'r', encoding='utf-8') as f_lines:
                            lines = f_lines.readlines()
                            start = max(0, e.lineno - 3)
                            end = min(len(lines), e.lineno + 2)
                            for i in range(start, end):
                                prefix = ">> " if i == e.lineno - 1 else "   "
                                print(f"   {i+1}: {lines[i].rstrip()}")
                    except:
                        pass
                    error_count += 1
                except Exception as e:
                    print(f"\n⚠️ 读取失败: {file_path}")
                    print(f"   原因: {str(e)}")
                    error_count += 1

    print(f"\n扫描完成。共检测 {file_count} 个文件，发现 {error_count} 个错误。")
    return error_count > 0

if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else "."
    if check_json_files(target):
        sys.exit(1)
    else:
        sys.exit(0)
