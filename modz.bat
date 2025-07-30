@setlocal enableextensions
@cd /d "%~dp0"

rem Max quality: -Xms4g -Xmx8g -Dprism.maxvram=4g -jar modz.jar tileSize=248
rem Mid quality: -Xms2g -Xmx4g -Dprism.maxvram=2g -jar modz.jar tileSize=200
rem Min quality: -Xms1g -Xmx2g -Dprism.maxvram=1g -jar modz.jar tileSize=100
rem Get webApiKey: https://steamcommunity.com/dev/apikey

".\jre64\bin\java.exe" -Xms1g -Xmx2g -Dprism.maxvram=1g -jar modz.jar tileSize=100 webApiKey=
