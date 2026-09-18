#!/bin/bash
# Atajo opcional (Integrante 5): compila, genera datasets, corre el driver y las graficas.
set -e

echo "== 1/4 Compilando =="
javac -encoding UTF-8 -d src src/*.java

echo "== 2/4 Generando los 13 datasets de la matriz podada =="
bash pruebas/generar_datasets.sh

echo "== 3/4 Driver de pruebas (resultados.csv + verificacion_igualdad.txt) =="
java -cp src DriverPruebas

echo "== 4/4 Generando graficas PNG =="
java -cp src GeneradorGraficas

echo "[OK] Bateria completa finalizada."
