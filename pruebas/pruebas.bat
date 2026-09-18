@echo off
REM Atajo opcional (Integrante 5): compila, genera datasets, corre el driver y las graficas.

echo == 1/4 Compilando ==
javac -encoding UTF-8 -d src src\*.java
if errorlevel 1 goto error

echo == 2/4 Generando los 13 datasets de la matriz podada ==
call pruebas\generar_datasets.bat
if errorlevel 1 goto error

echo == 3/4 Driver de pruebas (resultados.csv + verificacion_igualdad.txt) ==
java -cp src DriverPruebas
if errorlevel 1 goto error

echo == 4/4 Generando graficas PNG ==
java -cp src GeneradorGraficas
if errorlevel 1 goto error

echo [OK] Bateria completa finalizada.
goto :eof

:error
echo [ERROR] La bateria se detuvo. Revise la salida anterior.
exit /b 1
