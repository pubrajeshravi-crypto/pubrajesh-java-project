@echo off
setlocal

echo ========================================================
echo   Starting Student Management System
echo ========================================================

:: 1. Check if MySQL/MariaDB is running on port 3306
netstat -ano | findstr :3306 | findstr LISTENING >nul
if %errorlevel% neq 0 (
    echo Starting database server on localhost:3306...
    start "" /b "C:\Program Files\MariaDB 13.0\bin\mysqld.exe" --defaults-file="C:\Program Files\MariaDB 13.0\data\my.ini"
    timeout /t 3 /nobreak >nul
) else (
    echo Database server is already running on localhost:3306.
)

:: 2. Launch Student Management System
echo Launching Application...
start "" "C:\JAVA\jdk-21.0.12.1\bin\javaw.exe" -jar "%~dp0target\student-management-system-1.0.0-shaded.jar"

echo Application launched successfully!
endlocal
