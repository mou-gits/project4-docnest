@echo off
setlocal

echo ========================================
echo Building DocNest Executable Packages...
echo ========================================

REM Move to script folder
cd /d "%~dp0"

echo.
echo [1/7] Running Maven clean install...
call mvn clean install
if errorlevel 1 goto :fail

echo.
echo [2/7] Copying dependencies...
call mvn dependency:copy-dependencies
if errorlevel 1 goto :fail

echo.
echo [3/7] Removing old output folders...
if exist dist-client rmdir /s /q dist-client
if exist dist-server rmdir /s /q dist-server
if exist installers rmdir /s /q installers

echo.
echo [4/7] Creating fresh folders...
mkdir dist-client
mkdir dist-server
mkdir installers

echo.
echo [5/7] Copying client files...
copy /y "docnest-client\target\docnest-client-1.0-SNAPSHOT.jar" "dist-client\"
copy /y "docnest-client\target\dependency\*.jar" "dist-client\"

echo.
echo [6/7] Copying server files...
copy /y "docnest-server\target\docnest-server-1.0-SNAPSHOT.jar" "dist-server\"
copy /y "docnest-server\target\dependency\*.jar" "dist-server\"

echo.
echo [7/7] Creating executable app folders...

echo Creating DocNestClient...
call jpackage ^
 --type app-image ^
 --name DocNestClient ^
 --input "dist-client" ^
 --main-jar "docnest-client-1.0-SNAPSHOT.jar" ^
 --main-class ca.docnest.ui.MainApp ^
 --module-path "dist-client" ^
 --add-modules javafx.controls,javafx.fxml ^
 --dest installers
if errorlevel 1 goto :fail

echo Creating DocNestServer...
call jpackage ^
 --type app-image ^
 --name DocNestServer ^
 --input "dist-server" ^
 --main-jar "docnest-server-1.0-SNAPSHOT.jar" ^
 --main-class ca.docnest.server.ServerMain ^
 --dest installers ^
 --win-console
if errorlevel 1 goto :fail

echo.
echo ========================================
echo SUCCESS!
echo Executable folders created:
echo installers\DocNestClient
echo installers\DocNestServer
echo ========================================
goto :end

:fail
echo.
echo ========================================
echo BUILD FAILED
echo Check messages above.
echo ========================================
exit /b 1

:end
pause
endlocal