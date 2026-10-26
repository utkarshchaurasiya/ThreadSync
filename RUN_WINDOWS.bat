@echo off
title ThreadSync — Java Multithreading Simulator
echo.
echo  ╔══════════════════════════════════════════════════╗
echo  ║   ThreadSync — Thread Synchronization Simulator ║
echo  ╚══════════════════════════════════════════════════╝
echo.

REM Check Java
java -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] Java not found. Please install JDK 17+
    echo  Download: https://adoptium.net
    pause
    exit /b 1
)

REM Check javac
javac -version >nul 2>&1
IF %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] javac not found. Please install JDK (not just JRE).
    echo  Download: https://adoptium.net
    pause
    exit /b 1
)

echo  [1/2] Compiling ThreadSyncApp.java...
if not exist out mkdir out
javac src\ThreadSyncApp.java -d out

IF %ERRORLEVEL% NEQ 0 (
    echo  [ERROR] Compilation failed. Check src\ThreadSyncApp.java
    pause
    exit /b 1
)

echo  [2/2] Launching ThreadSync...
echo.
java -cp out ThreadSyncApp

pause
