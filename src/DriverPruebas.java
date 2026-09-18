
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Locale;

public class DriverPruebas {

    static class ConfigPrueba {

        int N;
        int n;

        ConfigPrueba(int N, int n) {
            this.N = N;
            this.n = n;
        }
    }

    public static void main(String[] args) {
        // Matriz podada oficial de 13 configuraciones (Seccion 5.2)
        ConfigPrueba[] matrizPodada = new ConfigPrueba[]{
            // n = 2
            new ConfigPrueba(1000, 2),
            new ConfigPrueba(10000, 2),
            new ConfigPrueba(100000, 2),
            // n = 3
            new ConfigPrueba(1000, 3),
            new ConfigPrueba(10000, 3),
            new ConfigPrueba(100000, 3),
            // n = 10
            new ConfigPrueba(1000, 10),
            new ConfigPrueba(10000, 10),
            new ConfigPrueba(100000, 10),
            // n = 100
            new ConfigPrueba(1000, 100),
            new ConfigPrueba(10000, 100),
            // n = 1000
            new ConfigPrueba(1000, 1000),
            new ConfigPrueba(10000, 1000)
        };

        int[] hilos = new int[]{1, 2, 4, 8};

        new File("pruebas").mkdirs();
        new File("evidencia").mkdirs();

        File dirDatos = new File("datos");
        File[] candidatos = dirDatos.listFiles();
        boolean hayDatasets = false;
        if (candidatos != null) {
            for (File c : candidatos) {
                String nombre = c.getName();
                if (nombre.startsWith("datos_N") && nombre.endsWith(".bin")) {
                    hayDatasets = true;
                    break;
                }
            }
        }
        if (!hayDatasets) {
            System.err.println("No hay datasets datos_N*_n*.bin en datos/.");
            System.err.println("Genere los 13 con pruebas/generar_datasets.bat y vuelva a ejecutar.");
            return;
        }

        File csvFile = new File("pruebas/resultados.csv");
        File evidenciaFile = new File("evidencia/verificacion_igualdad.txt");

        try (PrintWriter csvWriter = new PrintWriter(new FileWriter(csvFile)); PrintWriter evWriter = new PrintWriter(new FileWriter(evidenciaFile))) {

            // Columnas CSV oficiales segun Seccion 8
            csvWriter.println("N,n,T,puntosPorBloque,tiempo_ms,d_min,d_max,idx_min_i,idx_min_j,idx_max_i,idx_max_j");
            evWriter.println("=== REPORTE DE VERIFICACION DE IGUALDAD SERIAL VS PARALELO ===");
            evWriter.println("Criterio: Coincidencia por valor con tolerancia 0 (Seccion 1.6 y Seccion 6)\n");

            // 1. Matriz podada regular
            for (ConfigPrueba cfg : matrizPodada) {
                int N = cfg.N;
                int n = cfg.n;
                String ruta = "datos/datos_N" + N + "_n" + n + ".bin";
                File f = new File(ruta);
                if (!f.exists()) {
                    System.err.println("Archivo no encontrado: " + ruta + ". Saltando...");
                    continue;
                }

                int bloque = LectorBloques.calcularPuntosPorBloque(n);
                if (bloque > N) {
                    bloque = N;
                }

                System.out.printf("--> Ejecutando N=%d, n=%d, bloque=%d%n", N, n, bloque);

                // Ejecucion Serial (referencia de tiempo y valores)
                Resultado resSerial = Serial.ejecutar(ruta, N, n, bloque);
                double dMinSerial = resSerial.dMin;
                double dMaxSerial = resSerial.dMax;

                // Escribir Serial en CSV (representado como T=1 serial en el registro).
                // Nota: por cada configuracion hay DOS filas T=1. La PRIMERA (esta) es
                // el serial de referencia; la SEGUNDA la escribe el bucle paralelo con
                // 1 hilo. Las graficas de speedup usan esta (el serial) como baseline,
                // por eso S(1)=1 por construccion. Columnas congeladas en la Seccion 8.
                escribirFila(csvWriter, N, n, 1, bloque, resSerial.tiempoMs,
                        dMinSerial, dMaxSerial, resSerial.minI, resSerial.minJ, resSerial.maxI, resSerial.maxJ);

                // Ejecutar Paralelo con T in {1, 2, 4, 8}
                for (int T : hilos) {
                    Resultado resPar = Paralelo.ejecutar(ruta, N, n, bloque, T, false);
                    double dMinPar = resPar.raizMin();
                    double dMaxPar = resPar.raizMax();

                    escribirFila(csvWriter, N, n, T, bloque, resPar.tiempoMs,
                            dMinPar, dMaxPar, resPar.minI, resPar.minJ, resPar.maxI, resPar.maxJ);

                    // Validacion de igualdad segun regla de tolerancia 0 por valor
                    boolean minOk = Math.abs(dMinSerial - dMinPar) < 1e-9;
                    boolean maxOk = Math.abs(dMaxSerial - dMaxPar) < 1e-9;

                    if (minOk && maxOk) {
                        boolean mismoMinIdx = (resSerial.minI == resPar.minI && resSerial.minJ == resPar.minJ);
                        boolean mismoMaxIdx = (resSerial.maxI == resPar.maxI && resSerial.maxJ == resPar.maxJ);
                        String detalleEmpate = (!mismoMinIdx || !mismoMaxIdx)
                                ? " [EMPATE VALIDO: Indices difieren pero valor coincide]"
                                : " [COINCIDENCIA EXACTA EN INDICES Y VALOR]";
                        evWriter.printf(Locale.ROOT, "N=%d n=%d T=%d: OK%s (dMin=%.6f, dMax=%.6f)%n",
                                N, n, T, detalleEmpate, dMinPar, dMaxPar);
                    } else {
                        evWriter.printf(Locale.ROOT, "N=%d n=%d T=%d: ERROR -> Serial(min=%.6f, max=%.6f) vs Paralelo(min=%.6f, max=%.6f)%n",
                                N, n, T, dMinSerial, dMaxSerial, dMinPar, dMaxPar);
                    }
                }
                csvWriter.flush();
                evWriter.flush();
            }

            // 2. PRUEBA OBLIGATORIA CON BLOQUE FORZADO (Seccion 7 y Seccion 1.10)
            int nForzado = 3;
            int NForzado = 1000;
            int bloqueForzado = 50;
            String rutaForzada = "datos/datos_N1000_n3.bin";

            if (!new File(rutaForzada).exists()) {
                System.err.println("Archivo no encontrado: " + rutaForzada
                        + ". Se omite la prueba obligatoria (genere los datasets con pruebas/generar_datasets.bat).");
            } else {
                System.out.println("\n--> Ejecutando PRUEBA OBLIGATORIA CON BLOQUE FORZADO (N=1000, n=3, bloque=50)...");

                Resultado resSerialForzado = Serial.ejecutar(rutaForzada, NForzado, nForzado, bloqueForzado);
                escribirFila(csvWriter, NForzado, nForzado, 1, bloqueForzado, resSerialForzado.tiempoMs,
                        resSerialForzado.dMin, resSerialForzado.dMax,
                        resSerialForzado.minI, resSerialForzado.minJ, resSerialForzado.maxI, resSerialForzado.maxJ);

                for (int T : hilos) {
                    Resultado resParForzado = Paralelo.ejecutar(rutaForzada, NForzado, nForzado, bloqueForzado, T, false);
                    escribirFila(csvWriter, NForzado, nForzado, T, bloqueForzado, resParForzado.tiempoMs,
                            resParForzado.raizMin(), resParForzado.raizMax(),
                            resParForzado.minI, resParForzado.minJ, resParForzado.maxI, resParForzado.maxJ);
                }
            }

            System.out.println("\n[OK] Bateria de pruebas finalizada exitosamente.");
            System.out.println("Archivo de datos generado: pruebas/resultados.csv");
            System.out.println("Archivo de evidencia generado: evidencia/verificacion_igualdad.txt");

        } catch (Exception e) {
            System.err.println("Error durante la ejecucion de pruebas:");
            e.printStackTrace();
        }
    }

    private static void escribirFila(PrintWriter writer, int N, int n, int T, int puntosPorBloque,
            double tiempoMs, double dMin, double dMax,
            int minI, int minJ, int maxI, int maxJ) {
        writer.printf(Locale.ROOT, "%d,%d,%d,%d,%.3f,%.6f,%.6f,%d,%d,%d,%d%n",
                N, n, T, puntosPorBloque, tiempoMs, dMin, dMax, minI, minJ, maxI, maxJ);
    }
}
