@echo off

REM - Params: [Num Iter] [Interval] [MpegFilename] [OutputFilename]

@set /a c = %2% + 1

set PYTHONPATH=.
for /l %%i in (1, 1, %1) do (
	echo Starting Player #%%i
	start cmd /c singleplayer.bat %3 %4 %%i
	
	REM --Wait [interval] seconds before starting next.
	ping 127.0.0.1 -n %c% > nul
)