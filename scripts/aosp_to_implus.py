import sys
import os

def convert_aosp_combined_to_implus(input_file, output_file):
    """
    解析 AOSP .combined 格式并转换为 IMPlus 格式。
    格式示例:  word=...,f=...,flags=,originalFreq=...
    """
    print(f"Parsing AOSP combined dictionary: {input_file}...")
    
    words = []
    try:
        with open(input_file, 'r', encoding='utf-8', errors='ignore') as f:
            for line in f:
                line = line.strip()
                if not line.startswith("word="):
                    continue
                
                # 提取 word 和 f
                parts = line.split(',')
                word = ""
                freq = 0
                for p in parts:
                    if p.startswith("word="):
                        word = p[5:]
                    elif p.startswith("f="):
                        try:
                            freq = int(p[2:])
                        except:
                            freq = 0
                
                if word:
                    words.append((word, freq))
        
        # 排序并保存
        words.sort(key=lambda x: x[1], reverse=True)
        
        with open(output_file, 'w', encoding='utf-8') as f:
            for word, freq in words:
                f.write(f"{word}\t{freq}\n")
                
        print(f"Success! Exported {len(words)} words to {output_file}")
    except Exception as e:
        print(f"Error during combined parsing: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python aosp_to_implus.py <input.combined> <output.txt>")
    else:
        convert_aosp_combined_to_implus(sys.argv[1], sys.argv[2])
