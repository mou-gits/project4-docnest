@echo off
setlocal

echo ========================================
echo Building DocNest release packages...
echo ========================================

REM Move to the folder where this BAT file lives
cd /d "%~dp0"

echo.
echo [1/6] Running Maven clean install...
call mvn clean install
if errorlevel 1 goto :fail

echo.
echo [2/6] Removing old packaging folders...
if exist dist-client rmdir /s /q dist-client
if exist dist-server rmdir /s /q dist-server
if exist installers rmdir /s /q installers

echo.
echo [3/6] Creating fresh packaging folders...
mkdir dist-client
mkdir dist-server

echo.
echo [4/6] Copying main JARs...
copy /y "docnest-client\target\docnest-client-1.0-SNAPSHOT.jar" "dist-client\"
if errorlevel 1 goto :fail

copy /y "docnest-server\target\docnest-server-1.0-SNAPSHOT.jar" "dist-server\"
if errorlevel 1 goto :fail

echo.
echo [5/6] Copying dependency JARs...
copy /y "docnest-client\target\dependency\*.jar" "dist-client\"
if errorlevel 1 goto :fail

copy /y "docnest-server\target\dependency\*.jar" "dist-server\"
if errorlevel 1 goto :fail

echo.
echo [6/6] Creating jpackage app images...

echo Creating DocNestClient...
call jpackage --type app-image --name DocNestClient --input "dist-client" --main-jar "docnest-client-1.0-SNAPSHOT.jar" --main-class ca.docnest.ui.MainApp --module-path "dist-client" --add-modules javafx.controls,javafx.fxml --dest installers
if errorlevel 1 goto :fail

echo Creating DocNestServer...
call jpackage --type app-image --name DocNestServer --input "dist-server" --main-jar "docnest-server-1.0-SNAPSHOT.jar" --main-class ca.docnest.server.ServerMain --dest installers --win-console
if errorlevel 1 goto :fail

echo.
echo ========================================
echo Build completed successfully.
echo Output folders:
echo   installers\DocNestClient
echo   installers\DocNestServer
echo ========================================
goto :end

:fail
echo.
echo ========================================
echo Build failed.
echo Check the error messages above.
echo ========================================
exit /b 1

:end
endlocal
pause