/**
 * Entrega del Integrante 4.
 *
 * Orquestador de la version paralela: crea T hilos TareaDistancia, cada uno
 * con su propio LectorBloques y su propio Resultado local, que compiten por
 * una cola dinamica de pares de bloques (Repartidor). Al terminar, se hace
 * join de todos los hilos y se reduce (combina) sus resultados locales en un
 * unico Resultado global.
 *
 * Uso:
 *   java -cp src Paralelo <archivo> <N> <n> <puntosPorBloque> <T>
 *
 * Ejemplo (prueba obligatoria con bloque forzado, seccion 7 del plan):
 *   java -cp src Paralelo datos/datos.bin 1000 3 50 4
 */
import java.util.Locale;

public class Paralelo {

    public static void main(String[] args) throws Exception {
        if (args.length < 5) {
            System.out.println("Uso: java Paralelo <archivo> <N> <n> <puntosPorBloque> <T>");
            return;
        }
        String archivo = args[0];
        int N = Integer.parseInt(args[1]);
        int n = Integer.parseInt(args[2]);
        int puntosPorBloque = Integer.parseInt(args[3]);
        int T = Integer.parseInt(args[4]);

        Resultado global = ejecutar(archivo, N, n, puntosPorBloque, T, true);

        System.out.printf(Locale.ROOT, "d_min=%.6f (i=%d, j=%d)%n", global.raizMin(), global.minI, global.minJ);
        System.out.printf(Locale.ROOT, "d_max=%.6f (i=%d, j=%d)%n", global.raizMax(), global.maxI, global.maxJ);
        System.out.printf(Locale.ROOT, "tiempo_ms=%.3f%n", global.tiempoMs);
    }

    /**
     * Corre la version paralela completa y devuelve el Resultado global
     * combinado, con tiempoMs ya cargado. Si imprimirDetalle es true,
     * tambien imprime la cabecera y cuantos pares proceso cada hilo (para la
     * prueba obligatoria de bloque forzado, seccion 7 y docs/NOTA_TECNICA_PARALELO.md).
     */
    public static Resultado ejecutar(String archivo, int N, int n, int puntosPorBloque, int T,
            boolean imprimirDetalle) throws Exception {
        int totalBloques = (N + puntosPorBloque - 1) / puntosPorBloque;
        Repartidor repartidor = new Repartidor(totalBloques);

        TareaDistancia[] hilos = new TareaDistancia[T];

        long inicio = System.nanoTime();
        for (int t = 0; t < T; t++) {
            hilos[t] = new TareaDistancia(archivo, N, n, puntosPorBloque, repartidor);
            hilos[t].start();
        }

        Resultado global = new Resultado();
        for (int t = 0; t < T; t++) {
            hilos[t].join();
            if (hilos[t].getError() != null) {
                throw new RuntimeException("Fallo en hilo " + t, hilos[t].getError());
            }
            global.combinar(hilos[t].resultado);
        }
        long fin = System.nanoTime();
        global.tiempoMs = (fin - inicio) / 1_000_000.0;

        if (imprimirDetalle) {
            System.out.println("=== Paralelo ===");
            System.out.println("archivo=" + archivo + " N=" + N + " n=" + n
                    + " puntosPorBloque=" + puntosPorBloque + " T=" + T);
            System.out.println("totalBloques=" + totalBloques + " totalPares=" + repartidor.getTotalPares());
            int suma = 0;
            for (int t = 0; t < T; t++) {
                System.out.println("hilo[" + t + "].paresProcesados=" + hilos[t].paresProcesados);
                suma += hilos[t].paresProcesados;
            }
            System.out.println("sumaParesProcesados=" + suma
                    + (suma == repartidor.getTotalPares() ? " (coincide con totalPares)" : " (NO coincide)"));
        }

        return global;
    }
}
