#!/bin/bash

# 设置Java类路径
CLASSPATH="."

# 添加Maven依赖
for jar in $(find ~/.m2/repository -name "*.jar" | head -20); do
    CLASSPATH="$CLASSPATH:$jar"
done

echo "编译Java文件..."

# 创建输出目录
mkdir -p target/classes

# 编译主要的类
javac -encoding UTF-8 -cp "$CLASSPATH" -d target/classes \
    src/main/java/com/timetable/dto/WechatLoginRequest.java \
    src/main/java/com/timetable/dto/WechatUserInfo.java \
    src/main/java/com/timetable/dto/WechatAccessToken.java

if [ $? -eq 0 ]; then
    echo "DTO类编译成功"
else
    echo "DTO类编译失败"
    exit 1
fi

# 编译服务类
javac -encoding UTF-8 -cp "$CLASSPATH:target/classes" -d target/classes \
    src/main/java/com/timetable/service/WechatLoginService.java

if [ $? -eq 0 ]; then
    echo "服务类编译成功"
else
    echo "服务类编译失败"
    exit 1
fi

echo "编译完成！"
