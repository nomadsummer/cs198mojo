@echo off
REM - Params: [MpegFilename] [OutputFilename] [Index]

set PYTHONPATH=.
python Tribler\Player\swarmplayer.py %1 1> %2%3.txt 2>&1
