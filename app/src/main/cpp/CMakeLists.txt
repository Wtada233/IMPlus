cmake_minimum_required(VERSION 3.22.1)

project("pinyin")

# Include directories
include_directories(googlepinyin/include)

# Define source files
set(PINYIN_SOURCES
    googlepinyin/share/dictbuilder.cpp
    googlepinyin/share/dictlist.cpp
    googlepinyin/share/dicttrie.cpp
    googlepinyin/share/lpicache.cpp
    googlepinyin/share/matrixsearch.cpp
    googlepinyin/share/mystdlib.cpp
    googlepinyin/share/ngram.cpp
    googlepinyin/share/pinyinime.cpp
    googlepinyin/share/searchutility.cpp
    googlepinyin/share/spellingtable.cpp
    googlepinyin/share/spellingtrie.cpp
    googlepinyin/share/splparser.cpp
    googlepinyin/share/sync.cpp
    googlepinyin/share/userdict.cpp
    googlepinyin/share/utf16char.cpp
    googlepinyin/share/utf16reader.cpp
    pinyin_jni.cpp
)

# Build shared library
add_library(pinyin SHARED ${PINYIN_SOURCES})

# Links
find_library(log-lib log)
target_link_libraries(pinyin ${log-lib})
