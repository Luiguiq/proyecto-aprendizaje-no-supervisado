# Límites Computacionales y Justificación de Poda

## 1. Análisis del Caso Extremo: $N = 100\,000\,000$ y $n = 1000$

El problema plantea la comparación de todos los pares de un dataset con $N = 10^8$ puntos en $n = 1000$ dimensiones[cite: 1].

### A. Requerimiento de Almacenamiento en Disco
El formato almacena una cabecera de 8 bytes seguida de $N \times n$ valores `double` de 8 bytes en formato big-endian[cite: 1]:
$$\text{Tamaño total} = 8 + (100\,000\,000 \times 1\,000 \times 8)\text{ bytes} \approx 800\,000\,000\,008\text{ bytes} \approx 800\text{ GB}$$
Un archivo de este volumen supera la capacidad de almacenamiento disponible en estaciones de trabajo convencionales e imposibilita su procesamiento directo sin cómputo distribuido[cite: 1].

### B. Complejidad Algorítmica y Operaciones
El cálculo exhaustivo exige recorrer la mitad triangular superior[cite: 1]:
$$\text{Total de pares} = \frac{N(N - 1)}{2} = \frac{10^8(10^8 - 1)}{2} \approx 5 \times 10^{15}\text{ pares}$$
Para cada par, se calculan $n = 1000$ restas al cuadrado y sumas acumuladas[cite: 1]:
$$\text{Operaciones de punto flotante (FLOPs)} \approx 5 \times 10^{15} \times 1000 = 5 \times 10^{18}\text{ operaciones}$$

### C. Extrapolación de Tiempo
A una tasa sostenida de $10^8$ comparaciones de puntos por segundo, el cálculo requeriría:
$$t = \frac{5 \times 10^{15}}{10^8\text{ seg}} = 5 \times 10^7\text{ segundos} \approx 578\text{ días} \approx 1.58\text{ años}$$
Al incorporar la latencia real por accesos aleatorios a disco mecánico o SSD con `RandomAccessFile`, el tiempo total superaría varias décadas de cómputo ininterrumpido[cite: 1].

---

## 2. Justificación de la Poda de la Matriz de Pruebas

Debido a la complejidad $O(N^2 \cdot n)$, resulta inviable evaluar dimensiones altas junto con tamaños de muestra masivos mediante fuerza bruta[cite: 1]:
* Para $n \in \{2, 3, 10\}$, se evaluaron $N \in \{1\,000, 10\,000, 100\,000\}$[cite: 1].
* Para $n \in \{100, 1000\}$, el cómputo se limitó a $N \in \{1\,000, 10\,000\}$[cite: 1].

Esta poda permitió registrar la variación empírica del costo frente al incremento cuadrático de $N$ y lineal de $n$ en tiempos razonables de ejecución[cite: 1].

## 3. Conclusión
El enfoque por fuerza bruta exacta $O(N^2 \cdot n)$ no escala a problemas de gran volumen[cite: 1]. Para conjuntos masivos ($N \ge 10^7$), se requiere emplear:
1. Indexación espacial (KD-Trees, Ball-Trees).
2. Algoritmos de aproximación probabilística de vecinos cercanos (LSH, HNSW).
3. Muestreo estadístico previo al cálculo exhaustivo[cite: 1].