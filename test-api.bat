@echo off
chcp 65001 >nul
echo ========================================
echo API Runner 测试脚本
echo ========================================
echo.

echo 1. 测试健康检查接口
echo 正在测试 /health 接口...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/health' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 2. 测试URL白名单功能
echo 正在测试配置的URL /api/user/123...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/user/123' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 正在测试未配置的URL /api/unknown...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/unknown' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 3. 测试QPS统计接口
echo 正在测试 /api/stats/qps 接口...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/stats/qps?url=/api/user/123' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 4. 测试并发统计接口
echo 正在测试 /api/stats/concurrent 接口...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/stats/concurrent' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo 5. 测试URL配置接口
echo 正在测试 /api/config/urls 接口...
powershell -Command "try { $response = Invoke-WebRequest -Uri 'http://localhost:8080/api/config/urls' -Method GET; Write-Host '状态码:' $response.StatusCode; Write-Host '响应内容:' $response.Content } catch { Write-Host '错误:' $_.Exception.Message }"
echo.

echo ========================================
echo 测试完成！
echo ========================================
echo.
echo 提示：
echo - 如果看到错误，请确保服务已启动
echo - 使用 start.bat 启动服务
echo - 使用 Ctrl+C 停止服务
echo.
pause
