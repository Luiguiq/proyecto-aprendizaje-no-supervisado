# Participante 3: referencia serial y contrato de Resultado

## Compilacion y ejecucion

Desde la raiz del repositorio, con JDK 17 o superior:

```sh
javac --release 17 -d src src/*.java
java -cp src Serial datos/datos_verificacion.bin 30 2 7
java -cp src Serial datos/datos.bin 1000 3 50
javac --release 17 -cp src -d src pruebas/PruebaSerial.java
java -cp src PruebaSerial
```

Uso: `Serial <archivo> <N> <n> [puntosPorBloque]`. Sin el ultimo argumento se
usa `LectorBloques.calcularPuntosPorBloque(n)`. Se limita el bloque a N.
El constructor de LectorBloques verifica N y n contra la cabecera y valida
el tamanio del archivo. Serial no abre archivos directamente.

## Algoritmo y memoria

Se recorren bloques `(bi,bj)` con `bi <= bj`. En la diagonal solo se comparan
los puntos locales `i < j`; fuera de ella se compara el producto cartesiano.
El indice global es `indiceBloque * puntosPorBloque + indiceLocal` (base cero).
Esto cubre una vez cada par: `N*(N-1)/2`. El ultimo bloque usa su cantidad real.
Se suman cuadrados para cualquier n; solo al terminar se calculan dos raices.

Se reutilizan dos arreglos de doubles y el buffer de bytes del lector:
aproximadamente `24 * puntosPorBloque * n` bytes, mas objetos y cabecera.
La memoria depende del bloque, no del tamanio total del dataset. Los archivos
pequenos pueden caber en un bloque; no existe una carga completa adicional.
El costo aritmetico es O(N^2*n). Un bloque mayor puede reducir lecturas.
Se cierra el lector en `finally`, incluso ante errores.

## Contrato para participantes 4 y 5

```java
Resultado referencia = Serial.ejecutar(ruta, N, n, puntosPorBloque);
// referencia.dMin y referencia.dMax ya son distancias euclidianas.

Resultado total = new Resultado();
// Despues de join(), para cada acumulador local SIN finalizar:
total.combinar(local);
// Solo despues de combinar TODOS los hilos:
total.finalizar();
total.tiempoMs = (System.nanoTime() - inicio) / 1_000_000.0;
```

Campos publicos: `dMin`, `dMax`, `minI`, `minJ`, `maxI`, `maxJ`, `tiempoMs`.
Durante la acumulacion dMin y dMax son **distancias al cuadrado**.
`actualizar(d2,i,j)` requiere un valor finito no negativo e indices `0 <= i < j`.
`combinar(otro)` conserva los extremos y sus indices, no modifica la fuente ni
suma tiempos. Los acumuladores vacios (indices -1) son neutros, por ejemplo
si un hilo no recibio trabajo. Un resultado vacio no puede finalizarse.
`finalizar()` transforma los extremos en raices una sola vez. Actualizar o
combinar resultados finalizados se rechaza para evitar mezclar unidades.
No se deben modificar directamente los campos durante una reduccion.
La clase no es sincronizada: cada hilo tiene su acumulador; el orquestador
combina despues de los joins. En empates se conserva el primer par encontrado.
La comparacion serial/paralelo debe usar valores exactos, no exigir indices iguales.

## Resultados medidos

Fecha: 2026-09-15. Windows, OpenJDK Temurin 17.0.17, compilacion Java 17.
Cada fila corresponde a una ejecucion en una JVM nueva, sin calentamiento.
`tiempo_ms` incluye apertura, validacion, reservas de buffers, lecturas,
distancias, cierre y raices; excluye la impresion y la generacion del dataset.
Son mediciones de referencia funcional, no promedios estadisticos ni speedups.
La salida completa esta en `evidencia/salida_serial.txt`.

| Dataset | N | n | Bloque | tiempo_ms | d_min | d_max |
|---|---:|---:|---:|---:|---:|---:|
| Verificacion | 30 | 2 | 1 | 3.049 | 1.4142135623730951 | 841.4998514557207 |
| Verificacion | 30 | 2 | 7 | 2.109 | 1.4142135623730951 | 841.4998514557207 |
| Verificacion | 30 | 2 | 30 | 2.727 | 1.4142135623730951 | 841.4998514557207 |
| Base | 1000 | 3 | 37 | 15.747 | 0.986065719184471 | 161.08492797311416 |
| Base | 1000 | 3 | 50 | 12.956 | 0.986065719184471 | 161.08492797311416 |
| Base | 1000 | 3 | 1000 | 11.585 | 0.986065719184471 | 161.08492797311416 |

Verificacion: minimo `(0,1)`, maximo `(0,29)`, 435 pares.
Para `(i,i*i)`, el minimo es sqrt(2). Las diferencias absolutas de ambas
coordenadas estan acotadas por 29 y 841, alcanzadas juntas entre 0 y 29;
por ello el maximo es sqrt(29^2 + 841^2) = sqrt(708122).
Base: minimo `(429,632)`, maximo `(300,721)`, 499500 pares.
Con bloque 50 hay 20 bloques y 210 pares triangulares de bloques.
Estos son los valores de referencia para las corridas paralelas T=1,2,4,8.

## Verificacion reproducible

Se regenero localmente la base con `GestorDataset generar
datos/datos_int3_base.bin 1000 3 12345`: 24008 bytes, integridad OK y MD5
`d1871098306f974ceabc3467527428bb`, igual al archivo versionado y al plan.
El archivo regenerado esta ignorado por Git.

`PruebaSerial` paso 87 comprobaciones: valores analiticos de 30 puntos,
invariancia al cambiar bloques, bloque incompleto, bloque unitario, bloque
mayor que N, indices globales, duplicados y dimensiones 1,2,3,10,100,1000.
El oraculo independiente recorre solo siete puntos sinteticos en memoria;
el algoritmo de produccion siempre usa LectorBloques.
Tambien verifica reduccion con acumuladores vacios, fuentes intactas,
raices idempotentes, rechazo de unidades mezcladas, NaN, distancia negativa,
parametros invalidos, cabecera discordante y archivo truncado.

La matriz de rendimiento completa y la comparacion paralela corresponden
a la integracion de los participantes 4 y 5; no se atribuyen aqui resultados
de esas implementaciones.
