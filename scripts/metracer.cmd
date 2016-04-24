::@ECHO OFF
SETLOCAL ENABLEEXTENSIONS 
SETLOCAL ENABLEDELAYEDEXPANSION
SET myself=%~dpnx0
SET argc=0

FOR %%P IN (%*) DO (
 SET /A argc+=1
 IF "%%P%"=="-h" (SET is_help_requested=1)
 IF "%%P%"=="-l" (SET is_list_requested=1)
)

IF "%argc%"=="0" (
${usage_placeholder}
 EXIT /B 1
)

IF DEFINED JAVA_HOME (
 :: Use java.exe from JDK
 SET java_exe=%JAVA_HOME%\bin\java.exe
 SET java_exe_folder=%JAVA_HOME%\bin\
) ELSE (
 :: Resolve java.exe based on PATH
 FOR %%F IN (java.exe) DO (
  SET java_exe=%%~$PATH:F
  SET java_exe_folder=%%~dp$PATH:F
 )
)

IF NOT DEFINED java_exe (
 ECHO Failed to resolve 'java.exe' executable. Please, make sure that Java is installed
 EXIT /B 1
)

IF NOT EXIST "%java_exe%" (
 ECHO Resolved 'java.exe' executable (%java_exe%^) doesn't exist. Please, make sure that Java is installed
 EXIT /B 1
)

IF DEFINED is_help_requested (
 "%java_exe%" -jar "%myself%" %*
 EXIT /B !ERRORLEVEL!
)

:: For all other options we need tools.jar, resolve it
IF EXIST "%java_exe_folder%..\lib\tools.jar" (
 SET tools_jar=%java_exe_folder%..\lib\tools.jar
) ELSE (
  ECHO Failed to resolve tools.jar from a JDK. Please, make sure that JDK is installed and JAVA_HOME environment variable is properly set
  EXIT /B 1
 )
)

IF DEFINED is_list_requested (
 :: For JVM listing no impersonation is required
 "%java_exe%" -Xbootclasspath/a:"%tools_jar%" -jar "%myself%" %*
 EXIT /B !ERRORLEVEL!
)

FOR /F %%I IN ('CALL "%java_exe%" -cp "%myself%" com.develorium.metracer.WinMain %*') DO SET user_name=%%I
ECHO user_name=%user_name%

IF DEFINED user_name (
 SET current_user_name=%USERDOMAIN%\%USERNAME%
 IF NOT "!current_user_name!"=="%user_name%" (
  runas /user:"%user_name%" "\"%java_exe%\" -Xbootclasspath/a:\"%tools_jar%\" -jar \"%myself%\" %*"
  EXIT /B !ERRORLEVEL!
 )
)

"%java_exe%" -Xbootclasspath/a:"%tools_jar%" -jar "%myself%" %*
EXIT /B !ERRORLEVEL!
