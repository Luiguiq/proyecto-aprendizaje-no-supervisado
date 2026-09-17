@echo off
echo Generando los 13 datasets de la matriz podada...

REM n = 2 (N = 1000, 10000, 100000)
java -cp src GestorDataset generar datos/datos_N1000_n2.bin 1000 2 12345
java -cp src GestorDataset generar datos/datos_N10000_n2.bin 10000 2 12345
java -cp src GestorDataset generar datos/datos_N100000_n2.bin 100000 2 12345

REM n = 3 (N = 1000, 10000, 100000)
java -cp src GestorDataset generar datos/datos_N1000_n3.bin 1000 3 12345
java -cp src GestorDataset generar datos/datos_N10000_n3.bin 10000 3 12345
java -cp src GestorDataset generar datos/datos_N100000_n3.bin 100000 3 12345

REM n = 10 (N = 1000, 10000, 100000)
java -cp src GestorDataset generar datos/datos_N1000_n10.bin 1000 10 12345
java -cp src GestorDataset generar datos/datos_N10000_n10.bin 10000 10 12345
java -cp src GestorDataset generar datos/datos_N100000_n10.bin 100000 10 12345

REM n = 100 (solo N = 1000 y 10000)
java -cp src GestorDataset generar datos/datos_N1000_n100.bin 1000 100 12345
java -cp src GestorDataset generar datos/datos_N10000_n100.bin 10000 100 12345

REM n = 1000 (solo N = 1000 y 10000)
java -cp src GestorDataset generar datos/datos_N1000_n1000.bin 1000 1000 12345
java -cp src GestorDataset generar datos/datos_N10000_n1000.bin 10000 1000 12345

echo [OK] 13 datasets generados exitosamente.