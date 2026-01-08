#!/bin/bash
# 设置 GRADLE_USER_HOME 为当前目录下的 .gradle_data
# 确保所有依赖和 gradle 发行版都包含在项目文件夹内
export GRADLE_USER_HOME="$(pwd)/.gradle_data"

echo "Using GRADLE_USER_HOME: $GRADLE_USER_HOME"

if [ -x "./gradlew" ]; then
    ./gradlew "$@"
else
    echo "Error: ./gradlew not found or not executable."
    exit 1
fi
