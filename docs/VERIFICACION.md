# VERIFICACION.md — Verificación con el dataset de 30 puntos

Responsable: **Integrante 1** (GestorDataset).

## Dataset de verificación

`datos/datos_verificacion.bin` — 30 puntos en 2 dimensiones:

```
Punto[i] = (x_i, y_i) = (i, i*i)
```

Generado con:

```
java GestorDataset verificacion datos/datos_verificacion.bin
```

## Valores esperados (cálculo a mano)

- **Distancia mínima**: entre P[0] y P[1]:
  `d = sqrt((0-1)^2 + (0-1)^2) = sqrt(2) = 1.4142135623730951`
- **Distancia máxima**: entre P[0] y P[29]:
  ```
  d^2 = (0-29)^2 + (0-841)^2 = 29^2 + 841^2 = 841 + 707281 = 708122
  d   = sqrt(708122) = 841.4998514557207
  ```
  Justificación de que es el máximo: factorizando,
  `d^2(i,j) = (i-j)^2 * [1 + (i+j)^2]`. Los factores `|i-j|` y `|i+j|`
  se maximizan simultáneamente solo en los extremos `i=0, j=29`, por lo
  que ningún otro par puede superar este valor.

## Cómo usarlo para verificar un cálculo

1. Correr serial/paralelo contra `datos/datos_verificacion.bin`.
2. Comparar las distancias mínima y máxima contra los valores esperados
   (con tolerancia 0).
3. Comprobar a mano que la distancia entre los índices reportados coincide
   con el valor reportado.

## Regla de empate por valor

El criterio de verificación compara el **valor** de la distancia, no los índices.
Si el índice difiere pero el valor coincide, es un **empate válido**:
se verifica a mano que la distancia entre los índices reportados sea la misma.
Solo si el valor difiere es un fallo real (regla 1.6 / sección 6 del enunciado).