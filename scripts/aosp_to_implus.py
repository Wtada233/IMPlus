import sys
import struct

def convert_aosp_to_implus(input_file, output_file):
    """
    一个简化的 AOSP 二进制词典导出工具。
    由于 AOSP 格式（V2/V4）是 Trie 结构，完全解析需要复杂的树遍历。
    本工具尝试从中提取可读字符串和频率。
    """
    print(f"Reading AOSP dictionary: {input_file}...")
    
    try:
        with open(input_file, 'rb') as f:
            data = f.read()
            
        # 检查 AOSP Magic Number (0x7855b1ed)
        magic = struct.unpack('>I', data[:4])[0]
        if magic != 0x7855b1ed:
            print("Warning: Magic number mismatch. This might not be a standard AOSP dictionary.")

        # 简单的字符串提取策略 (针对二进制字典中的原始单词流)
        # 实际生产中建议使用 AOSP 的 dicttool 导出 .combined 格式
        words = []
        current_word = ""
        
        for i in range(len(data)):
            byte = data[i]
            if 32 <= byte <= 126: # 可打印 ASCII
                current_word += chr(byte)
            else:
                if len(current_word) > 1:
                    # 尝试寻找频率字节 (通常在单词结束后的某个偏移量)
                    # 这里的逻辑是简化的，真实场景建议直接使用 text 版词库
                    freq = data[i+1] if i+1 < len(data) else 200
                    words.append((current_word, freq))
                current_word = ""
        
        # 排序并去重
        words = sorted(list(set(words)), key=lambda x: x[1], reverse=True)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            for word, freq in words:
                f.write(f"{word}\t{freq}\n")
                
        print(f"Success! Exported {len(words)} words to {output_file}")
        print("Note: If the output looks messy, please use AOSP 'dicttool' to export to '.combined' format first.")

    except Exception as e:
        print(f"Error: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python aosp_to_implus.py <input.dict> <output.txt>")
    else:
        convert_aosp_to_implus(sys.argv[1], sys.argv[2])
