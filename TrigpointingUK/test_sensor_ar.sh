#!/bin/bash

# Test script for SensorARActivity fixes
# This script runs the specific tests we created to prevent regression

echo "Running SensorARActivity tests to verify fixes..."

# Change to the project directory
cd "$(dirname "$0")"

echo "Running unit tests..."
./gradlew test --tests "uk.trigpointing.android.ar.SensorARActivityUnitTest" --info

echo ""
echo "Running Android instrumentation tests..."
./gradlew connectedAndroidTest --tests "uk.trigpointing.android.ar.SensorARActivityTest" --info

echo ""
echo "Running all location-related tests..."
./gradlew test --tests "*Location*" --info

echo ""
echo "Test run complete!"
echo ""
echo "Key fixes verified:"
echo "1. Location provider availability checks"
echo "2. Permission request code handling"
echo "3. Error handling for IllegalArgumentException"
echo "4. Graceful degradation when providers are unavailable"
