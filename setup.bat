@echo off
setlocal enabledelayedexpansion
title Ola S1 Quick Setup Tool

echo ========================================================================
echo                 Ola S1 Quick Mod and Navigation Installer
echo ========================================================================
echo.

:: 1. Check Python
set "PY_CMD="
py -3 --version >nul 2>&1
if !errorlevel! equ 0 (
    set "PY_CMD=py -3"
) else (
    python --version >nul 2>&1
    if !errorlevel! equ 0 (
        set "PY_CMD=python"
    )
)

if "!PY_CMD!"=="" (
    echo [ERROR] Python 3 was not found on your system!
    echo Please install Python 3 from https://www.python.org/downloads/
    echo IMPORTANT: Make sure to tick the checkbox "Add Python to PATH" during installation.
    echo.
    pause
    exit /b 1
)

:: 2. Check ADB
where adb >nul 2>&1
if !errorlevel! neq 0 (
    if exist "%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe" (
        set "PATH=%LOCALAPPDATA%\Android\Sdk\platform-tools;!PATH!"
    ) else if exist "C:\platform-tools\adb.exe" (
        set "PATH=C:\platform-tools;!PATH!"
    ) else if exist "C:\Program Files\platform-tools\adb.exe" (
        set "PATH=C:\Program Files\platform-tools;!PATH!"
    ) else (
        echo [ERROR] 'adb' command was not found!
        echo Please download Android Platform-Tools:
        echo https://developer.android.com/tools/releases/platform-tools
        echo Extract the zip and add its folder to your system PATH.
        echo.
        pause
        exit /b 1
    )
)

:: 3. Change directory to script folder
cd /d "%~dp0"

:: 4. Install required dependencies
echo [*] Checking dependencies...
!PY_CMD! -m pip install -q --disable-pip-version-check -r requirements.txt
if !errorlevel! neq 0 (
    echo [!] Warning: pip install encountered an issue, trying to proceed...
)

:: 5. Launch Setup
echo [*] Starting installer...
echo.
!PY_CMD! -u setup.py %*

echo.
pause

