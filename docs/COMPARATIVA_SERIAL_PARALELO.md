# COMPARATIVA_SERIAL_PARALELO.md — Criterio de verificación y resultados T=1,2,4,8

Responsable: **Integrante 4** (evidencia de igualdad de la versión paralela).

## Criterio de verificación (por valor, tolerancia 0)

Se compara el **valor** de la distancia mínima y máxima entre serial y paralelo
con tolerancia 0. El índice es referencia, no criterio:

- Si el índice difiere pero el **valor** coincide, se verifica a mano que la
  distancia entre los índices reportados sea la misma → **empate válido**.
- Solo si el **valor** difiere es un fallo real.

El reporte automático está en `evidencia/verificacion_igualdad.txt` y el CSV
oficial en `pruebas/resultados.csv`.

> **Nota sobre las filas `T=1` del CSV.** Por cada configuración se escribe el
> serial de referencia y luego el paralelo con `T = 1, 2, 4, 8`. Como las
> columnas están congeladas (Sección 8, sin columna `modo`), hay **dos filas
> `T=1`**: la **primera** es el serial y la **segunda** el paralelo de 1 hilo.
> Las gráficas de speedup usan el serial (la primera) como baseline, de modo que
> `S(1) = 1` por construcción.

## Prueba obligatoria con bloque forzado (N=1000, n=3, bloque=50)

| T | tiempo_ms | S(T) = serial/T | E(T) | d_min (i,j) | d_max (i,j) |
|---|-----------|-----------------|------|-------------|-------------|
| 1 (serial) | 2.354 | 1.000 | 1.000 | 0.986066 (429,632) | 161.084928 (300,721) |
| 1 (paralelo) | 3.465 | 0.679 | 0.679 | 0.986066 (429,632) | 161.084928 (300,721) |
| 2 | 2.053 | 1.147 | 0.573 | 0.986066 (429,632) | 161.084928 (300,721) |
| 4 | 1.323 | 1.779 | 0.445 | 0.986066 (429,632) | 161.084928 (300,721) |
| 8 | 1.148 | 2.050 | 0.256 | 0.986066 (429,632) | 161.084928 (300,721) |

(En esta configuración tan pequeña el paralelo solo supera al serial en `T=4`
y `T=8`.)

## Carga grande (N=10000, n=1000, bloque=524)

| T | tiempo_ms | S(T) | E(T) |
|---|-----------|------|------|
| 1 (serial) | 22 319.774 | 1.000 | 1.000 |
| 2 | 12 495.200 | 1.786 | 0.893 |
| 4 | 6 867.352 | 3.250 | 0.813 |
| 8 | 3 926.618 | 5.684 | 0.710 |

Aquí sí se aprecia la aceleración: los bloques pequeños (524 puntos) generan
`20` bloques y `210` pares, repartidos dinámicamente entre los hilos.

## Conclusión

- Los valores mínimos y máximos coinciden con tolerancia 0 en todas las
  configuraciones (0 filas `ERROR` en `verificacion_igualdad.txt`).
- No se observaron empates con índices distintos en las corridas registradas:
  el serial y el paralelo reportaron los mismos índices y valores.
- La eficiencia cae al crecer `T` por el costo de E/S concurrente y de la cola
  dinámica (ver `docs/ANALISIS_SPEEDUP.md`).
