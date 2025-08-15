#!/bin/bash

echo "========================================"
echo "API Runner - 接口压测服务"
echo "========================================"
echo

echo "正在启动服务..."
echo "服务地址: http://localhost:8080"
echo "监控地址: http://localhost:8080/actuator"
echo

# 检查Java是否安装
if ! command -v java &> /dev/null; then
    echo "错误: 未找到Java环境，请先安装Java 8或更高版本"
    exit 1
fi

# 检查Maven是否安装
if ! command -v mvn &> /dev/null; then
    echo "错误: 未找到Maven环境，请先安装Maven"
    exit 1
fi

echo "正在编译项目..."
mvn clean compile

if [ $? -ne 0 ]; then
    echo "编译失败，请检查代码"
    exit 1
fi

echo "编译成功，正在启动服务..."
echo

mvn spring-boot:run
