import java.io.IOException;
import java.util.Locale;

/** Referencia serial: cada par global i < j se visita una sola vez, por bloques. */
public class Serial {
    public static Resultado ejecutar(String ruta, int N, int n) throws IOException {
        return ejecutar(ruta, N, n, LectorBloques.calcularPuntosPorBloque(n));
    }

    /** Incluye apertura, validacion, buffers, lecturas, calculo, cierre y raices. */
    public static Resultado ejecutar(String ruta, int N, int n, int puntosPorBloque)
            throws IOException {
        long inicio = System.nanoTime();
        if (N < 2 || n < 1 || puntosPorBloque < 1) {
            throw new IllegalArgumentException("Se requiere N >= 2, n >= 1 y bloque >= 1");
        }
        int bloque = Math.min(N, puntosPorBloque);
        int elementos = LectorBloques.calcularBytesBuffer(bloque, n) / GestorDataset.TAM_DOUBLE;
        Resultado resultado = new Resultado();
        LectorBloques lector = new LectorBloques(ruta, N, n, bloque);
        try {
            double[] a = new double[elementos];
            double[] b = new double[elementos];
            int bloques = (int) (((long) N + bloque - 1) / bloque);
            for (int bi = 0; bi < bloques; bi++) {
                int cantidadA = lector.cantidadRealBloque(bi);
                lector.leerBloque(bi, cantidadA, n, a);
                int inicioA = (int) ((long) bi * bloque);
                for (int bj = bi; bj < bloques; bj++) {
                    int cantidadB = lector.cantidadRealBloque(bj);
                    double[] segundo = a;
                    if (bi != bj) {
                        lector.leerBloque(bj, cantidadB, n, b);
                        segundo = b;
                    }
                    int inicioB = (int) ((long) bj * bloque);
                    for (int i = 0; i < cantidadA; i++) {
                        int desdeJ = bi == bj ? i + 1 : 0;
                        for (int j = desdeJ; j < cantidadB; j++) {
                            double suma = 0;
                            for (int k = 0; k < n; k++) {
                                double diferencia = a[i * n + k] - segundo[j * n + k];
                                suma += diferencia * diferencia;
                            }
                            resultado.actualizar(suma, inicioA + i, inicioB + j);
                        }
                    }
                }
            }
        } finally {
            lector.cerrar();
        }
        resultado.finalizar();
        resultado.tiempoMs = (System.nanoTime() - inicio) / 1_000_000.0;
        return resultado;
    }

    public static void main(String[] args) {
        if (args.length != 3 && args.length != 4) {
            System.err.println("Uso: java -cp src Serial <archivo> <N> <n> [puntosPorBloque]");
            System.exit(1);
        }
        try {
            int N = Integer.parseInt(args[1]);
            int n = Integer.parseInt(args[2]);
            int bloque = args.length == 4 ? Integer.parseInt(args[3])
                    : LectorBloques.calcularPuntosPorBloque(n);
            Resultado r = ejecutar(args[0], N, n, bloque);
            System.out.println("archivo=" + args[0]);
            System.out.println("N=" + N + " n=" + n + " puntosPorBloque=" + Math.min(N, bloque));
            System.out.println("pares=" + (long) N * (N - 1) / 2);
            System.out.println("d_min=" + r.dMin + " indices=(" + r.minI + "," + r.minJ + ")");
            System.out.println("d_max=" + r.dMax + " indices=(" + r.maxI + "," + r.maxJ + ")");
            System.out.printf(Locale.ROOT, "tiempo_ms=%.3f%n", r.tiempoMs);
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("Error serial: " + e.getMessage());
            System.exit(1);
        }
    }
}
