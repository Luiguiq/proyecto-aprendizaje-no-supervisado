# NOTA_TECNICA_PARALELO.md — Versión paralela (cola dinámica)

Responsable: **Integrante 4** (`Paralelo`, `TareaDistancia`, `Repartidor`).

## Arquitectura

- `Repartidor`: genera **una sola vez** la lista triangular de pares de bloques
  `(bi, bj)` con `bi <= bj` usando dos arreglos primitivos paralelos
  (`listaBi`, `listaBj`), sin `ArrayList`. `siguientePar()` es `synchronized` y
  solo lee/avanza un índice.
- `TareaDistancia` (un `Thread` por hilo): abre su **propio** `LectorBloques`
  (descriptor por hilo) y acumula un `Resultado` local. Pide pares hasta que
  `siguientePar()` devuelve `null`.
- `Paralelo`: orquesta `T` hilos, hace `join()` y reduce con
  `Resultado.combinar` en el hilo principal.

## Sincronización

La única sección crítica es `Repartidor.siguientePar()`. El doble bucle interno
de distancias es totalmente local a cada hilo. La reducción final (`combinar`)
ocurre después del `join()`.

## Carga triangular (la diagonal pesa la mitad)

Para `bi == bj` se comparan solo pares `i < j` dentro del bloque. Para
`bi < bj` se comparan todos contra todos entre los dos bloques. El número total
de pares triangulares es `B(B+1)/2` para `B` bloques.

## Prueba obligatoria con bloque forzado (N=1000, n=3, bloque=50)

`B = 1000 / 50 = 20` bloques → `20·21/2 = 210` pares triangulares, para
`T = 1, 2, 4, 8`. La misma cantidad total de pares permite comparar el reparto
entre hilos.

| T | Pares por hilo | Suma | Coincide | tiempo_ms |
|---|----------------|------|----------|-----------|
| 1 | `[210]` | 210 | sí | 10.270 |
| 2 | `[105, 105]` | 210 | sí | 18.387 |
| 4 | `[57, 51, 52, 50]` | 210 | sí | 25.370 |
| 8 | `[28, 23, 28, 27, 26, 25, 24, 29]` | 210 | sí | 33.791 |

Resultado idéntico para todos los `T`:
`d_min = 0.986066 (i=429, j=632)`, `d_max = 161.084928 (i=300, j=721)`.

Salida cruda: `evidencia/salida_paralelo.txt`.

## Interpretación

Con `N=1000` y bloque 50 el problema es demasiado pequeño frente al costo de
crear hilos y sincronizar la cola, por lo que el paralelo es **más lento** que
el serial a partir de `T=2`. La ganancia aparece en configuraciones grandes
(ver `docs/ANALISIS_SPEEDUP.md` y `docs/COMPARATIVA_SERIAL_PARALELO.md`).

## Uso

```
java -cp src Paralelo <archivo> <N> <n> <puntosPorBloque> <T>
java -cp src Paralelo datos/datos.bin 1000 3 50 4
```
