#!/bin/bash

echo ""
echo " ╔══════════════════════════════════════════════════╗"
echo " ║   ThreadSync — Thread Synchronization Simulator ║"
echo " ╚══════════════════════════════════════════════════╝"
echo ""

# Check java
if ! command -v java &>/dev/null; then
    echo " [ERROR] java not found. Install JDK 17+: https://adoptium.net"
    exit 1
fi

# Check javac
if ! command -v javac &>/dev/null; then
    echo " [ERROR] javac not found. Please install full JDK (not just JRE)."
    echo " Ubuntu/Debian: sudo apt install openjdk-21-jdk"
    echo " macOS:         brew install openjdk@21"
    exit 1
fi

echo " [1/2] Compiling..."
mkdir -p out
javac src/ThreadSyncApp.java -d out

if [ $? -ne 0 ]; then
    echo " [ERROR] Compilation failed."
    exit 1
fi

echo " [2/2] Launching ThreadSync..."
echo ""
java -cp out ThreadSyncApp
