@echo off
@setlocal enableextensions
@cd /d "%~dp0"

rem Max quality: -Xms4g -Xmx8g -Dprism.maxvram=4g -jar modz.jar tileSize=248
rem Mid quality: -Xms3g -Xmx6g -Dprism.maxvram=2g -jar modz.jar tileSize=186
rem Min quality: -Xms1g -Xmx2g -Dprism.maxvram=1g -jar modz.jar tileSize=100
rem Get webApiKey: https://steamcommunity.com/dev/apikey

".\jre64\bin\java.exe" -Xms1g -Xmx2g -Dprism.maxvram=1g -jar modz.jar tileSize=100 webApiKey=
