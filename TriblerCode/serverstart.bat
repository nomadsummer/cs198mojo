REM - Params: OutputFilename
set PYTHONPATH=.
python Tribler\Tools\createlivestream-noauth.py --name %1.mpegts --source http://localhost:8080 --destdir . 1> %1_ServerLog.txt 2>&1