@echo off
echo ========================================
echo API Runner - 接口压测服务
echo ========================================
echo.

echo 正在启动服务...
echo 服务地址: http://localhost:8080
echo 监控地址: http://localhost:8080/actuator
echo.

REM 检查Java是否安装
java -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Java环境，请先安装Java 8或更高版本
    pause
    exit /b 1
)

REM 检查Maven是否安装
mvn -version >nul 2>&1
if errorlevel 1 (
    echo 错误: 未找到Maven环境，请先安装Maven
    pause
    exit /b 1
)

echo 正在编译项目...
call mvn clean compile

if errorlevel 1 (
    echo 编译失败，请检查代码
    pause
    exit /b 1
)

echo 编译成功，正在启动服务...
echo.

call mvn spring-boot:run

pause
