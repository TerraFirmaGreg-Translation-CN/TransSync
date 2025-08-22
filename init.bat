@echo off
:: init.bat - 初始化 TransSync 配置
:: 生成 config.yml 文件于 src/main/resources/

setlocal enabledelayedexpansion

echo.
echo [TransSync] 初始化配置向导
echo ==============================
echo.

:: 设置默认路径
set "CONFIG_FILE=src\main\resources\config.yml"

:: 检查是否已经存在配置文件
if exist "%CONFIG_FILE%" (
    echo 配置文件已存在：%CONFIG_FILE%
    choice /c yn /n /m "是否覆盖？(y/n): "
    if !errorlevel! == 2 goto end
)

:: 收集用户输入
set TOKEN=
set PROJECT_ID=
set REPO_OWNER=
set REPO_NAME=
set BRANCH=
set LOCAL_PATH=

echo.
echo === ParaTranz 设置 ===
set /p TOKEN=请输入 ParaTranz API Token:

set /p PROJECT_ID=请输入项目 ID (ParaTranz 上的项目数字ID):

echo.
echo === GitHub 设置 ===
set /p REPO_OWNER=请输入 GitHub 仓库拥有者（用户名或组织名）:
set /p REPO_NAME=请输入 GitHub 仓库名（如 TransSync）:
set /p BRANCH=请输入目标分支名（默认 main）:
if "%BRANCH%"=="" set BRANCH=main

echo.
echo === 本地设置 ===
echo 语言文件将保存在此目录下（相对于项目根目录）
set /p LOCAL_PATH=请输入本地语言文件路径（如 src/main/resources/lang）:
if "%LOCAL_PATH%"=="" set LOCAL_PATH=src/main/resources/lang

:: 创建 resources 目录（如果不存在）
if not exist "src\main\resources" mkdir "src\main\resources"

:: 生成 config.yml
(
    echo # TransSync 配置文件
    echo # 由 init.bat 自动生成
    echo # 请勿手动修改字段类型
    echo.
    echo paratranz:
    echo   token: "%TOKEN%"
    echo   projectId: %PROJECT_ID%
    echo.
    echo github:
    echo   repoOwner: "%REPO_OWNER%"
    echo   repoName: "%REPO_NAME%"
    echo   branch: "%BRANCH%"
    echo.
    echo local:
    echo   languagePath: "%LOCAL_PATH%"
    echo.
) > "%CONFIG_FILE%"

echo.
echo 成功生成配置文件：%CONFIG_FILE%
echo 提示：现在可以使用 Maven 构建并运行项目。

:end
pause
