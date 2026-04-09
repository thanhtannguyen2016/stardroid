@echo off
echo Downloading Stellarium constellation artwork (Free Art License by Johan Meuris)...

set PS="C:\Windows\System32\WindowsPowerShell\v1.0\powershell.exe"
set BASE=https://raw.githubusercontent.com/Stellarium/stellarium-skycultures/master/western/illustrations
set DST=F:\build\stardroid\app\src\main\assets\constellation_artwork

echo Downloading Orion...
%PS% -Command "Invoke-WebRequest -Uri '%BASE%/orion.webp' -OutFile '%DST%\orion.webp'"
echo Downloading Scorpius...
%PS% -Command "Invoke-WebRequest -Uri '%BASE%/scorpius.webp' -OutFile '%DST%\scorpius.webp'"
echo Downloading Leo...
%PS% -Command "Invoke-WebRequest -Uri '%BASE%/leo.webp' -OutFile '%DST%\leo.webp'"
echo Downloading Ursa Major...
%PS% -Command "Invoke-WebRequest -Uri '%BASE%/ursa-major.webp' -OutFile '%DST%\ursa_major.webp'"
echo Downloading Ursa Minor...
%PS% -Command "Invoke-WebRequest -Uri '%BASE%/ursa-minor.webp' -OutFile '%DST%\ursa_minor.webp'"

echo.
echo Done! Files saved to %DST%
echo License: Free Art License (Johan Meuris / Stellarium)
pause
