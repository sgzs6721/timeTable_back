#!/bin/bash

# 设置UTF-8编码
export LANG=zh_CN.UTF-8
export LC_ALL=zh_CN.UTF-8

# 设置JVM参数
JAVA_OPTS="-Dfile.encoding=UTF-8 -Dsun.jnu.encoding=UTF-8 -Dconsole.encoding=UTF-8"

# 获取当前脚本所在目录
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# 查找jar文件
JAR_FILE=$(find target -name "timetable-backend*.jar" | head -n 1)

if [ -z "$JAR_FILE" ]; then
    echo "未找到jar文件，请先执行: mvn clean package"
    exit 1
fi

echo "启动应用: $JAR_FILE"
echo "使用JVM参数: $JAVA_OPTS"

# 启动应用
java $JAVA_OPTS -jar "$JAR_FILE"

