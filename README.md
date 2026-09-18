# Programación Serial y Paralela Multithreading - Aprendizaje No Supervisado

Cálculo exhaustivo de distancias euclidianas mínimas y máximas sobre datasets binarios en disco sin carga total en memoria.

## 1. Restricciones Técnicas
* Sin librerías externas ni colecciones dinámicas (`ArrayList`, `HashMap`, `Stream`, `Random` omitidos en el cómputo central).
* Lectura por bloques en disco utilizando exclusivamente `RandomAccessFile` a través de `LectorBloques`.
* Medición en milisegundos con 3 decimales (`System.nanoTime()` dividido entre `1_000_000.0`).
* Entorno de compilación: JDK 17 / JDK 21 LTS.

## 2. Arquitectura de Componentes
* `GestorDataset.java` (Int. 1): Generador binario determinista (LCG, semilla 12345) y validador de cabeceras.
* `LectorBloques.java` (Int. 2): Interfaz de E/S por bloques con validación temprana de cabecera y conversión manual big-endian.
* `Serial.java` / `Resultado.java` (Int. 3): Algoritmo secuencial triangular y contrato de reducción con método `combinar`.
* `Paralelo.java` / `Repartidor.java` / `TareaDistancia.java` (Int. 4): Orquestador multihilo con cola dinámica sincronizada (T = 1, 2, 4, 8).
* `DriverPruebas.java` / `GeneradorGraficas.java` (Int. 5): Automatización experimental de matriz podada, bloque forzado a 50 puntos, validación de igualdad y generación de gráficas PNG en Java puro.

## 3. Frontera de Clases
* `GestorDataset` **no lee bloques** (solo `info`, `leerCabecera`, `validarArchivo`; `leerPunto` solo para depuración).
* `LectorBloques` es la **única puerta** de lectura por bloques al disco.
* `Serial` y `Paralelo`/`TareaDistancia` **no abren `RandomAccessFile`**: cada hilo usa su propio `LectorBloques`.

## 4. Contratos
* `Resultado`: campos `dMin, dMax, minI, minJ, maxI, maxJ, tiempoMs`; métodos `actualizar(d², i, j)` y `combinar(otro)`. El serial no usa `combinar`; el paralelo sí.
* `LectorBloques`: valida la cabecera en su propio constructor y falla temprano si el archivo está corrupto o los parámetros no coinciden.

## 5. Unidad de Tiempo y Verificación
* Todo se mide con `System.nanoTime()` y se reporta en ms con 3 decimales; columna oficial `tiempo_ms` en `pruebas/resultados.csv`.
* La verificación serial vs paralelo compara el **valor** de la distancia (tolerancia 0), no el índice. Si el índice difiere pero el valor coincide, es un **empate válido**.
* Prueba obligatoria: bloque forzado a **50 puntos** para N=1000, n=3, T = 1, 2, 4, 8 (20 bloques → 210 pares triangulares).
* En `resultados.csv` hay **dos filas `T=1`** por configuración: la **primera** es el serial de referencia y la **segunda** es el paralelo con 1 hilo. Las gráficas de speedup usan el serial como baseline, por eso `S(1)=1` por construcción (las columnas del CSV están congeladas en la Sección 8, sin columna `modo`).

## 6. Reglas de Datos
* Solo se versionan `datos/datos.bin` y `datos/datos_verificacion.bin`.
* Los datasets grandes de la matriz podada se generan localmente y nunca se suben (`.gitignore`).

## 7. Compilación y Ejecución

```bash
# 1) Compilar todo el código fuente
javac -d src src/*.java

# 2) Generar los 13 datasets de la matriz podada (paso previo obligatorio del Int. 5)
#    Windows:
pruebas\generar_datasets.bat
#    Linux/Mac:
bash pruebas/generar_datasets.sh

# 3) Ejecución Serial de referencia (N=1000, n=3)
java -cp src Serial datos/datos.bin 1000 3

# 4) Ejecución Paralela con bloque forzado (N=1000, n=3, bloque 50, T=4)
java -cp src Paralelo datos/datos.bin 1000 3 50 4

# 5) Batería completa de pruebas (requiere el paso 2)
java -cp src DriverPruebas

# 6) Generar las gráficas PNG
java -cp src GeneradorGraficas
```

Atajo opcional (hace los pasos 1, 2, 5 y 6 de una vez):

```bash
# Windows:
pruebas\pruebas.bat
# Linux/Mac:
bash pruebas/pruebas.sh
```

## 8. Utilidades del Generador

```bash
java -cp src GestorDataset generar <archivo> <N> <n> <semilla> [rango]
java -cp src GestorDataset verificacion <archivo>
java -cp src GestorDataset info <archivo>
java -cp src GestorDataset ver <archivo> <desde> [hasta]
```

`datos.bin` (entregable del Int. 1) y `datos_N1000_n3.bin` (que genera el Int. 5) tienen el **mismo contenido** con distinto nombre; el driver busca el segundo.
