# Análisis de Aceleración (Speedup) y Eficiencia

## 1. Definición de Métricas
* **Speedup ($S(T)$):** Relación entre el tiempo de ejecución serial y el tiempo paralelo con $T$ hilos:
  $$S(T) = \frac{\text{tiempo\_ms\_serial}}{\text{tiempo\_ms\_paralelo}(T)}$$
* **Eficiencia ($E(T)$):** Fracción del cómputo aprovechada por cada hilo:
  $$E(T) = \frac{S(T)}{T}$$

## 2. Comportamiento Observado
En los experimentos ejecutados con la cola dinámica triangular (210 pares para $N=1000$ y particiones proporcionales en bloques grandes), se observa un incremento en el speedup al pasar de $T=1$ a $T=2$ y $T=4$. Sin embargo, la aceleración no es lineal ($S(T) < T$), produciéndose una degradación de la eficiencia cuando $T=8$.

## 3. Factores de Pérdida de Eficiencia

1. **Cuello de Botella en Disco (E/S Concurrente):**
   * Cada hilo gestiona su propio descriptor `RandomAccessFile`.
   * Con $T=4$ y $T=8$, múltiples hilos intentan realizar lecturas concurrentes (`seek` + `readFully`) sobre el mismo archivo físico. Esto satura el canal de almacenamiento y causa contención en el sistema de archivos.
2. **Sincronización en la Cola Dinámica:**
   * Aunque el acceso crítico se limitó a `siguientePar()` en la clase `Repartidor`, a medida que aumenta la cantidad de hilos, el monitor `synchronized` genera microbloqueos al solicitar pares de bloques.
3. **Fracción Serial Intrínseca (Ley de Amdahl):**
   * El ciclo de vida de los hilos (`Thread.start()`, sincronización por `join()`), la reducción final mediante `Resultado.combinar()` y la extracción final de raíces cuadradas son fases seriales.
4. **Desbalance Triangular de Carga:**
   * Los pares en la diagonal ($bi == bj$) comparan únicamente la mitad triangular $i < j$, procesando la mitad de pares que un bloque fuera de la diagonal ($bi \ne bj$)[cite: 1]. Aunque la cola dinámica amortigua este efecto, los últimos pares en terminar provocan que algunos hilos queden ociosos esperando el `join()` final[cite: 1].