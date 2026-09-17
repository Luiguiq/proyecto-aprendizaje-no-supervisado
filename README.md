# Programación Serial y Paralela Multithreading - Aprendizaje No Supervisado

Cálculo exhaustivo de distancias euclidianas mínimas y máximas sobre datasets binarios en disco sin carga total en memoria[cite: 1].

## 1. Restricciones Técnicas
* Sin librerías externas ni colecciones dinámicas (`ArrayList`, `HashMap`, `Stream`, `Random` omitidos en el cómputo central)[cite: 1].
* Lectura por bloques en disco utilizando exclusivamente `RandomAccessFile` a través de `LectorBloques`[cite: 1].
* Medición en milisegundos con 3 decimales (`System.nanoTime()` dividido entre `1_000_000.0`)[cite: 1].
* Entorno de compilación: JDK 17 / JDK 21 LTS[cite: 1].

## 2. Arquitectura de Componentes
* `GestorDataset.java` (Int. 1): Generador binario determinista (LCG, semilla 12345) y validador de cabeceras[cite: 1].
* `LectorBloques.java` (Int. 2): Interfaz de E/S por bloques con validación temprana de cabecera y conversión manual big-endian[cite: 1].
* `Serial.java` / `Resultado.java` (Int. 3): Algoritmo secuencial triangular y contrato de reducción con método `combinar`[cite: 1].
* `Paralelo.java` / `Repartidor.java` / `TareaDistancia.java` (Int. 4): Orquestador multihilo con cola dinámica sincronizada ($T=1, 2, 4, 8$)[cite: 1].
* `DriverPruebas.java` / `GeneradorGraficas.java` (Int. 5): Automatización experimental de matriz podada, bloque forzado a 50 puntos, validación de igualdad y generación de gráficas PNG en Java puro[cite: 1].

## 3. Compilación y Ejecución

```bash
# Compilar todo el código fuente
javac -d src src/*.java

# Ejecución Serial de referencia (N=1000, n=3)
java -cp src Serial datos/datos.bin 1000 3

# Ejecución Paralela con bloque forzado (N=1000, n=3, bloque 50, T=4)
java -cp src Paralelo datos/datos.bin 1000 3 50 4

# Ejecutar la batería completa de pruebas
java -cp src DriverPruebas

# Generar las gráficas PNG
java -cp src GeneradorGraficas