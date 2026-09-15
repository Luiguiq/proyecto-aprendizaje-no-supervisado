# NOTA_TECNICA_E_S.md — Capa de E/S por bloques (LectorBloques)

Responsable: **Integrante 2** (LectorBloques, PruebaLector).

## Contexto y frontera

El cálculo de distancias (Serial y Paralelo) nunca abre `RandomAccessFile` ni lee
el dataset completo en memoria. La **única puerta de entrada al disco** es
`LectorBloques` (regla 9). `GestorDataset` solo genera, lee cabecera y valida;
no lee bloques para cálculo.

## Formato big-endian y offset de cada bloque

El archivo binario es big-endian (lo que escribe `DataOutputStream.writeDouble`):

| Bytes | Tipo | Contenido |
|-------|------|-----------|
| 0..3  | int  | N |
| 4..7  | int  | n |
| 8..   | double | N·n valores punto por punto |

El bloque `i` contiene los puntos `[i·puntosPorBloque, min((i+1)·puntosPorBloque, N)-1]`.
El punto absoluto `p` empieza en el byte:

```
offset(p) = 8 + p·n·8       (calculado en long para no desbordar)
```

`LectorBloques.leerBloque` hace `raf.seek(offset)` y `raf.readFully(cantidad·n·8)`
una sola vez, y convierte de byte a double **a mano** con `Double.longBitsToDouble`
sobre los 8 bytes en orden big-endian (sin `readDouble`). Eso garantiza que la
lectura no depende de ninguna clase de E/S adicional y que el orden de bytes es
exactamente el que escribió el generador.

## Validación en el constructor (fallo temprano)

`LectorBloques` **no confía en los parámetros** que recibe del llamador:

1. Abre su propio `RandomAccessFile` (un descriptor por hilo).
2. Lee la cabecera con `GestorDataset.leerCabecera` (N y n salen del archivo).
3. Valida la integridad con `GestorDataset.validarArchivo` (tamaño real == esperado).
4. Si los `N`/`n` del llamador no coinciden con la cabecera, lanza `IOException`
   inmediatamente.

Si el constructor falla, cierra el descriptor antes de propagar la excepción.

## Descriptor por hilo

Cada hilo de la versión paralela **abre su propio `LectorBloques`** (y por tanto su
propio `RandomAccessFile`). El puntero de `seek` de un hilo nunca afecta al de otro:
no hay estado compartido de E/S, no se necesita `synchronized` para leer.

## Buffers reutilizados

Cada `LectorBloques` mantiene un único arreglo `byte[]` como buffer de lectura que
se reutiliza en todas las llamadas a `leerBloque` (cero basura). Los arreglos
`double[]` de salida los aporta y reutiliza el llamador (Serial y cada tarea de la
versión paralela tienen los suyos por hilo).

## Tamaños según memoria disponible

- `calcularPuntosPorBloque(n)`: apunta a bloques de ~8 MiB de datos, o 1/4 del heap
  si este es menor. Nunca devuelve 0 y nunca deja que el buffer supere `int`.
- `calcularBytesBuffer(puntosPorBloque, n)`: valida el producto `puntosPorBloque·n·8`
  contra `Integer.MAX_VALUE` y lanza `IllegalArgumentException` si no cabe en un `int`.

## Último bloque incompleto

`cantidadRealBloque(i)` devuelve `min(puntosPorBloque, N - i·puntosPorBloque)`.
`leerBloque` ajusta la `cantidad` pedida al resto disponible del archivo, de modo
que el llamador puede pedir el "tamaño normal" y este se recorta automáticamente en
el último bloque. En la prueba obligatoria de bloque de 50 puntos con N=1000 quedan
20 bloques completos (ver sección 7 del plan).

## Verificación de la evidencia

- **Lectura idéntica byte a byte**: se compararon los 1000 puntos leídos por
  `LectorBloques` contra los impresos por `GestorDataset ver` (fc.exe, sin diferencias).
- **Dataset de verificación**: `Punto[i] = (i, i·i)` para i = 0..29 (30 puntos, 2D).
- **Fallo temprano**: archivo truncado a 100 bytes y cabecera con N/n descuadrados
  → `IOException` en el constructor.
- **Prueba obligatoria**: bloque de 50 puntos → 20 bloques, último con puntos 950..999.

## Uso

```
java PruebaLector datos/datos.bin 0 400        # lee bloque 0 con 400 puntos/bloque
java PruebaLector datos/datos.bin 19 50        # prueba obligatoria: 20 bloques de 50
java PruebaLector datos/datos_verificacion.bin 0 30
```