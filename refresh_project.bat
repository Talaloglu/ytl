@echo off
echo ========================================
echo   Movie App Dependency Refresh Script
echo ========================================
echo.

echo Step 1: Cleaning build files...
if exist ".gradle" rmdir /s /q ".gradle"
if exist "app\build" rmdir /s /q "app\build"
if exist "build" rmdir /s /q "build"
echo Build files cleaned.
echo.

echo Step 2: Running Gradle clean...
call gradlew clean
echo.

echo Step 3: Running Gradle build...
call gradlew build
echo.

echo Step 4: Syncing Gradle wrapper...
call gradlew wrapper --gradle-version=8.2
echo.

echo ========================================
echo   Dependency refresh completed!
echo ========================================
echo.
echo Your categorized home screen should now work properly with streaming URL matching.
echo Only movies with valid Supabase video URLs will be displayed in all sections.
echo.
pause