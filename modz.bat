@setlocal enableextensions
@cd /d "%~dp0"

".\jre64\bin\java.exe" -Xms2g -Xmx8g -Dprism.maxvram=1g -jar modz.jar
