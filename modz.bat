@setlocal enableextensions
@cd /d "%~dp0"

".\jre64\bin\java.exe" -Xms4g -Xmx8g -Dprism.maxvram=4g -jar modz.jar
