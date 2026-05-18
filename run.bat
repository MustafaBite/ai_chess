@echo off
echo Derleniyor...
if not exist "out" mkdir out
dir /s /b src\main\java\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
del sources.txt

echo.
echo Baslatiliyor...
echo ----------------------------------------
java -cp out com.aichess.engine.Main
pause
