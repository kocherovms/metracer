:: ECHO OFF
SETLOCAL ENABLEEXTENSIONS

IF DEFINED JAVA_HOME1 (
 :: Use java.exe from JDK
 SET java_exe=%JAVA_HOME1%\bin\java.exe
 SET java_exe_folder=%JAVA_HOME1%\bin
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
 ECHO %java_exe% doesn't exist. Please, make sure that Java is installed
 EXIT /B 1
)

IF EXIST "%java_exe_folder%..\lib\tools.jar" (
 SET tools_jar="%java_exe_folder%..\lib\tools.jar"
) ELSE (
 IF EXIST "%java_exe_folder%..\..\lib\tools.jar" (
  SET tools_jar="%java_exe_folder%..\..\lib\tools.jar"
  SET java_exe="%java_exe_folder%..\..\bin\java.exe"
 ) ELSE (
  ECHO Failed to resolve tools.jar from a JDK. Please, make sure that JDK is installed and JAVA_HOME environment variable is properly set
  EXIT /B 1
 )
)

IF NOT EXIST "%java_exe%" (
 ECHO %java_exe% doesn't exist. Please, make sure that Java is installed
 EXIT /B 1
)
  
ECHO java_exe=%java_exe%
ECHO tools_jar=%tools_jar%

