# FORMATO_DATASET.md — Formato del dataset binario

Responsable: **Integrante 1** (GestorDataset).

## Formato del archivo

Escritura big-endian, sin librerías externas.

| Bytes       | Tipo   | Contenido                         |
|-------------|--------|-----------------------------------|
| `0..3`      | `int`  | N: cantidad de puntos             |
| `4..7`      | `int`  | n: dimensiones por punto          |
| `8..`       | `double` | N·n valores consecutivos, punto por punto |

El punto de índice `i` empieza en el byte:

```
offset(i) = 8 + i * n * 8
```

Todos los desplazamientos se calculan en `long` para no desbordar con N grande.

## Lectura

`GestorDataset.leerCabecera(RandomAccessFile)` devuelve `[N, n]` leyendo desde el byte 0.
`GestorDataset.validarArchivo(RandomAccessFile)` comprueba que `longitud(archivo) == 8 + N·n·8`.
`GestorDataset.leerPunto(...)` existe solo para depuración puntual; la lectura por bloques
es responsabilidad exclusiva de `LectorBloques` (Integrante 2).

## Reproducibilidad

El generador usa un **LCG de 64 bits** propio (sin `Random` ni librerías):

```
estado = estado * 6364136223846793005L + 1442695040888963407L
```

Con la misma semilla y los mismos `N`, `n` y `rango`, cualquier integrante obtiene el
**mismo archivo byte a byte**.

## Comandos de generación

```
java GestorDataset generar <archivo> <N> <n> <semilla> [rango]   # rango por defecto: 100.0
java GestorDataset verificacion <archivo>                        # 30 puntos, 2D, (i, i*i)
java GestorDataset info <archivo>                                # cabecera + validacion de integridad
java GestorDataset ver <archivo> <desde> [hasta]                 # imprime puntos (depuracion)
```

Ejemplo del entregable:

```
java GestorDataset generar datos/datos.bin 1000 3 12345
java GestorDataset verificacion datos/datos_verificacion.bin
```

## datos.bin vs datos_N1000_n3.bin

`datos.bin` (entregable del Integrante 1, versionado) y `datos_N1000_n3.bin`
(que genera el Integrante 5) contienen **el mismo contenido con distinto nombre**.
El `DriverPruebas` busca por patrón `datos_N<valor>_n<valor>.bin`, por eso el
Integrante 5 debe incluir explícitamente ese archivo además de los versionados.

## Archivos versionados

Solo se versionan `datos/datos.bin` y `datos/datos_verificacion.bin`.
Los datasets grandes se generan localmente y nunca se suben (regla 4.1 del repo).