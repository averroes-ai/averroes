@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  Gradle startup script for Windows (using local installation)
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

@rem Use local Gradle installation
set GRADLE_HOME=C:\Gradle\gradle-8.14.3
set GRADLE_EXE=%GRADLE_HOME%\bin\gradle.bat

@rem Check if local Gradle exists
if not exist "%GRADLE_EXE%" (
    echo.
    echo ERROR: Local Gradle not found at %GRADLE_EXE%
    echo Please ensure Gradle is installed at C:\Gradle\gradle-8.14.3
    echo.
    goto fail
)

@rem Execute Gradle using local installation
"%GRADLE_EXE%" %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable GRADLE_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd_ return code if you prefer
if not "" == "%GRADLE_EXIT_CONSOLE%" exit /b %ERRORLEVEL%
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
