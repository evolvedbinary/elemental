@REM
@REM Elemental
@REM Copyright (C) 2024, Evolved Binary Ltd
@REM
@REM admin@evolvedbinary.com
@REM https://www.evolvedbinary.com | https://www.elemental.xyz
@REM
@REM This library is free software; you can redistribute it and/or
@REM modify it under the terms of the GNU Lesser General Public
@REM License as published by the Free Software Foundation; version 2.1.
@REM
@REM This library is distributed in the hope that it will be useful,
@REM but WITHOUT ANY WARRANTY; without even the implied warranty of
@REM MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
@REM Lesser General Public License for more details.
@REM
@REM You should have received a copy of the GNU Lesser General Public
@REM License along with this library; if not, write to the Free Software
@REM Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
@REM

@REM
@REM Simple build Script for Elemental that tries to make it easier to build a few of the usual targets
@REM Author: Adam Retter
@REM

@echo off
setlocal enabledelayedexpansion

set "TARGET=useage"
set "OFFLINE=false"
set "CONCURRENCY=-T2C"

:: Process arguments
:parse_args
if "%~1"=="" goto done_parsing

if /I "%~1"=="--offline" (
    set "OFFLINE=true"
) else if /I "%~1"=="--help" (
    set "TARGET=useage"
) else if /I "%~1"=="-h" (
    set "TARGET=useage"
) else if /I "%~1"=="quick" (
    set "TARGET=quick"
) else if /I "%~1"=="quick-archives" (
    set "TARGET=quick-archives"
) else if /I "%~1"=="quick-docker" (
    set "TARGET=quick-docker"
) else if /I "%~1"=="quick-archives-docker" (
    set "TARGET=quick-archives-docker"
) else if /I "%~1"=="quick-install" (
    set "TARGET=quick-install"
) else if /I "%~1"=="test" (
    set "TARGET=test"
) else if /I "%~1"=="site" (
    set "TARGET=site"
) else if /I "%~1"=="license-check" (
    set "TARGET=license-check"
) else if /I "%~1"=="license-format" (
    set "TARGET=license-format"
) else if /I "%~1"=="dependency-check" (
    set "TARGET=dependency-check"
) else if /I "%~1"=="dependency-security-check" (
    set "TARGET=dependency-security-check"
)
shift
goto parse_args

:done_parsing

if "%OFFLINE%"=="true" (
    set "BASE_CMD=%BASE_CMD% --offline"
)

if "%TARGET%"=="useage" (
    goto show_useage
)

:: Determine script directory
set "SCRIPT_DIR=%~dp0"
set "BASE_CMD=%SCRIPT_DIR%\mvnw.cmd -V"

:: Set CMD based on TARGET
if "%TARGET%"=="quick" (
    set "CMD=%BASE_CMD% %CONCURRENCY% clean package -DskipTests -Ddependency-check.skip=true -Dappbundler.skip=true -Ddocker=false -P !mac-dmg-on-mac,!codesign-mac-app,!codesign-mac-dmg,!mac-dmg-on-unix,!installer,!concurrency-stress-tests,!micro-benchmarks,skip-build-dist-archives"
) else if "%TARGET%"=="quick-archives" (
    set "CMD=%BASE_CMD% %CONCURRENCY% clean package -DskipTests -Ddependency-check.skip=true -Ddocker=true -P installer,!concurrency-stress-tests,!micro-benchmarks"
) else if "%TARGET%"=="quick-docker" (
    set "CMD=%BASE_CMD% %CONCURRENCY% clean package -DskipTests -Ddependency-check.skip=true -Dappbundler.skip=true -Ddocker=true -P docker,!mac-dmg-on-mac,!codesign-mac-app,!codesign-mac-dmg,!mac-dmg-on-unix,!installer,!concurrency-stress-tests,!micro-benchmarks,skip-build-dist-archives"
) else if "%TARGET%"=="quick-archives-docker" (
    set "CMD=%BASE_CMD% %CONCURRENCY% clean package -DskipTests -Ddependency-check.skip=true -Ddocker=true -P installer,-P docker,!concurrency-stress-tests,!micro-benchmarks"
) else if "%TARGET%"=="quick-install" (
    set "CMD=%BASE_CMD% %CONCURRENCY% clean install package -DskipTests -Ddependency-check.skip=true -Dappbundler.skip=true -Ddocker=false -P !mac-dmg-on-mac,!codesign-mac-app,!codesign-mac-dmg,!mac-dmg-on-unix,!installer,!concurrency-stress-tests,!micro-benchmarks,skip-build-dist-archives"
) else if "%TARGET%"=="test" (
    set "CMD=%BASE_CMD% clean test -Ddependency-check.skip=true"
) else if "%TARGET%"=="site" (
    set "CMD=%BASE_CMD% clean test -Ddependency-check.skip=true"
) else if "%TARGET%"=="license-check" (
    set "CMD=%BASE_CMD% license:check"
) else if "%TARGET%"=="license-format" (
    set "CMD=%BASE_CMD% license:format"
) else if "%TARGET%"=="dependency-check" (
    set "CMD=%BASE_CMD% dependency:analyze"
) else if "%TARGET%"=="dependency-security-check" (
    set "CMD=%BASE_CMD% dependency-check:check"
) else (
    echo Invalid target: %TARGET%
    goto show_useage
)

:: Execute the command
call %CMD%
goto end

:show_useage
echo.
echo Usage: build.bat [--offline] ^<target^> ^| --help
echo.
echo Available build targets:
echo    quick                       - Build distribution directory
echo    quick-archives              - Build and archive distribution
echo    quick-docker                - Build distribution + Docker image
echo    quick-archives-docker       - All of the above
echo    quick-install               - Installs Maven artifacts locally
echo    test                        - Run test suite
echo    site                        - Run tests and generate Maven site
echo    license-check               - Check license headers
echo    license-format              - Add missing license headers
echo    dependency-check            - Analyze dependencies
echo    dependency-security-check   - Check for known CVEs

:end
endlocal
