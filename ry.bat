@echo off
setlocal EnableDelayedExpansion

rem 文件用途：管理 yr-admin 打包产物的启动、停止、重启与状态检查
rem 创建时间：历史脚本，2026-03-10 调整为兼容 Java 17 的启动参数与顺序
set "AppName=yr-admin.jar"

rem Java 17 兼容的 JVM 参数，移除 PermSize 与旧 GC 日志参数
set "JVM_OPTS=-Dname=%AppName% -Duser.timezone=Asia/Shanghai -Xms512M -Xmx512M -XX:+HeapDumpOnOutOfMemoryError -XX:NewRatio=1 -XX:SurvivorRatio=30 -XX:+UseParallelGC"

echo.
echo.  [1] 启动 %AppName%
echo.  [2] 停止 %AppName%
echo.  [3] 重启 %AppName%
echo.  [4] 查看状态 %AppName%
echo.  [5] 退出
echo.

echo.请输入选项编号:
set /p ID=
if "%ID%"=="1" goto start
if "%ID%"=="2" goto stop
if "%ID%"=="3" goto restart
if "%ID%"=="4" goto status
if "%ID%"=="5" goto end
pause
goto end

:start
set "pid="
set "image_name="
for /f "tokens=1,2" %%a in ('jps -l ^| findstr /i "%AppName%"') do (
    set "pid=%%a"
    set "image_name=%%b"
)
if defined pid (
    echo %AppName% is running
    pause
    goto end
)

rem 先注入 JVM 参数，再指定 -jar，避免参数被误当成应用参数
start "" javaw %JVM_OPTS% -jar %AppName%
echo Start %AppName% success...
goto end

:stop
set "pid="
set "image_name="
for /f "tokens=1,2" %%a in ('jps -l ^| findstr /i "%AppName%"') do (
    set "pid=%%a"
    set "image_name=%%b"
)
if not defined pid (
    echo process %AppName% does not exist
) else (
    echo prepare to kill %image_name%
    echo start kill %pid% ...
    taskkill /f /pid %pid%
)
goto end

:restart
call :stop
call :start
goto end

:status
set "pid="
set "image_name="
for /f "tokens=1,2" %%a in ('jps -l ^| findstr /i "%AppName%"') do (
    set "pid=%%a"
    set "image_name=%%b"
)
if not defined pid (
    echo process %AppName% is dead
) else (
    echo %image_name% is running
)
goto end

:end
endlocal
