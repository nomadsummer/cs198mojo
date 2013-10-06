REM echo off

REM - Params: [BaseConfigName] [Base Interval] [Max Interval]

for /l %%i in (%2, 1, %3) do (
	java -jar starstream.jar %1%%i.txt
)