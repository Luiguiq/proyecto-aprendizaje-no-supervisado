#!/bin/bash
mkdir -p datos

# n = 2
java -cp src GestorDataset generar datos/datos_N1000_n2.bin 1000 2 12345
java -cp src GestorDataset generar datos/datos_N10000_n2.bin 10000 2 12345
java -cp src GestorDataset generar datos/datos_N100000_n2.bin 100000 2 12345

# n = 3
java -cp src GestorDataset generar datos/datos_N1000_n3.bin 1000 3 12345
java -cp src GestorDataset generar datos/datos_N10000_n3.bin 10000 3 12345
java -cp src GestorDataset generar datos/datos_N100000_n3.bin 100000 3 12345

# n = 10
java -cp src GestorDataset generar datos/datos_N1000_n10.bin 1000 10 12345
java -cp src GestorDataset generar datos/datos_N10000_n10.bin 10000 10 12345
java -cp src GestorDataset generar datos/datos_N100000_n10.bin 100000 10 12345

# n = 100
java -cp src GestorDataset generar datos/datos_N1000_n100.bin 1000 100 12345
java -cp src GestorDataset generar datos/datos_N10000_n100.bin 10000 100 12345

# n = 1000
java -cp src GestorDataset generar datos/datos_N1000_n1000.bin 1000 1000 12345
java -cp src GestorDataset generar datos/datos_N10000_n1000.bin 10000 1000 12345

echo "[OK] Datasets generados exitosamente."