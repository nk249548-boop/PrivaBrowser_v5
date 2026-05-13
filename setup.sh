#!/bin/bash
echo "PrivaBrowser Setup Script"
echo "========================="

JAR_PATH="gradle/wrapper/gradle-wrapper.jar"

if [ -f "$JAR_PATH" ] && [ -s "$JAR_PATH" ]; then
    echo "✅ gradle-wrapper.jar already present"
else
    echo "⬇ Downloading gradle-wrapper.jar..."
    if command -v curl > /dev/null; then
        curl -L "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar" \
             -o "$JAR_PATH"
    elif command -v wget > /dev/null; then
        wget "https://github.com/gradle/gradle/raw/v8.5.0/gradle/wrapper/gradle-wrapper.jar" \
             -O "$JAR_PATH"
    else
        echo "❌ curl/wget not found. Please download manually."
        exit 1
    fi
    echo "✅ gradle-wrapper.jar downloaded"
fi

chmod +x gradlew
echo "✅ gradlew is executable"
echo ""
echo "🚀 Ready! Run: ./gradlew assembleDebug"
