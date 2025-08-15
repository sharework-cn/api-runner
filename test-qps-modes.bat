@echo off
chcp 65001 >nul
echo ========================================
echo API Runner QPS模式测试脚本
echo ========================================
echo.

echo 本脚本将测试两种QPS控制模式：
echo 1. DELAY模式 - 通过Thread.sleep()控制QPS
echo 2. CPU模式 - 通过CPU密集型计算控制QPS
echo.

echo 请确保API Runner服务已经启动在 http://localhost:8080
echo 按任意键开始测试...
pause >nul

echo.
echo ========================================
echo 测试1: DELAY模式（默认模式）
echo ========================================
echo.

echo 当前配置：DELAY模式
echo 正在测试 /health 接口获取当前配置...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/health' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 测试DELAY模式下的QPS控制...
echo 发起10个并发请求到 /api/user/123...
for /L %%i in (1,1,10) do (
    echo 请求 %%i...
    start /B powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/user/123' -Method GET; Write-Host '请求%%i完成，状态码:' $response.StatusCode } catch { Write-Host '请求%%i错误:' $_.Exception.Message }"
)
echo.

echo 等待5秒让请求完成...
timeout /t 5 /nobreak >nul

echo 获取QPS统计信息...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/stats/qps?url=/api/user/123' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo ========================================
echo 测试2: CPU模式
echo ========================================
echo.

echo 请按照以下步骤切换到CPU模式：
echo 1. 停止当前服务（Ctrl+C）
echo 2. 修改 application.yml 中的 qps-control-mode 为 CPU
echo 3. 重新启动服务
echo 4. 按任意键继续测试...
pause >nul

echo 正在测试 /health 接口获取当前配置...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/health' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 测试CPU模式下的QPS控制...
echo 发起10个并发请求到 /api/user/123...
for /L %%i in (1,1,10) do (
    echo 请求 %%i...
    start /B powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/user/123' -Method GET; Write-Host '请求%%i完成，状态码:' $response.StatusCode } catch { Write-Host '请求%%i错误:' $_.Exception.Message }"
)
echo.

echo 等待5秒让请求完成...
timeout /t 5 /nobreak >nul

echo 获取QPS统计信息...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/stats/qps?url=/api/user/123' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 对比两种模式：
echo - DELAY模式：响应时间较长，但CPU使用率低
echo - CPU模式：响应时间较短，但CPU使用率高
echo.
echo 提示：
echo - 生产环境推荐使用DELAY模式
echo - 测试环境可以使用CPU模式进行高精度测试
echo.
pause
