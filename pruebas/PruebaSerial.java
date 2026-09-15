import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Pruebas sin dependencias; el oraculo en memoria solo usa 7 puntos sinteticos. */
public class PruebaSerial {
    private static int comprobaciones;

    private static void comprobar(boolean condicion, String mensaje) {
        if (!condicion) throw new AssertionError(mensaje);
        comprobaciones++;
    }

    private static void falla(RunnableIO accion) throws IOException {
        boolean fallo = false;
        try { accion.run(); }
        catch (IOException | IllegalArgumentException | IllegalStateException esperado) { fallo = true; }
        comprobar(fallo, "Debio rechazar la entrada");
    }

    private interface RunnableIO { void run() throws IOException; }

    public static void main(String[] args) throws IOException {
        for (int b : new int[] {1, 7, 29, 30, 50}) {
            Resultado r = Serial.ejecutar("datos/datos_verificacion.bin", 30, 2, b);
            comprobar(r.dMin == Math.sqrt(2) && r.dMax == Math.sqrt(708122), "30 puntos, bloque " + b);
            comprobar(r.minI == 0 && r.minJ == 1 && r.maxI == 0 && r.maxJ == 29, "Indices globales");
        }
        Resultado base = Serial.ejecutar("datos/datos.bin", 1000, 3, 50);
        for (int b : new int[] {1, 37, 1000, 2000}) {
            Resultado r = Serial.ejecutar("datos/datos.bin", 1000, 3, b);
            comprobar(r.dMin == base.dMin && r.dMax == base.dMax, "Invariancia bloque " + b);
        }
        Path archivo = Files.createTempFile("prueba-serial-", ".bin");
        try {
            for (int n : new int[] {1, 2, 3, 10, 100, 1000}) {
                double[][] puntos = new double[7][n];
                for (int i = 0; i < 7; i++) {
                    for (int k = 0; k < n; k++) puntos[i][k] = ((i * 13 + k * 7) % 19) - 9;
                }
                // Duplicados en bloques distintos: minimo cero y empates validos.
                for (int k = 0; k < n; k++) puntos[6][k] = puntos[0][k];
                try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(archivo))) {
                    out.writeInt(7); out.writeInt(n);
                    for (double[] punto : puntos) for (double valor : punto) out.writeDouble(valor);
                }
                double min = Double.POSITIVE_INFINITY, max = 0;
                for (int i = 0; i < 7; i++) for (int j = i + 1; j < 7; j++) {
                    double d = 0;
                    for (int k = 0; k < n; k++) { double v = puntos[i][k] - puntos[j][k]; d += v * v; }
                    min = Math.min(min, d); max = Math.max(max, d);
                }
                for (int b : new int[] {1, 2, 3, 7, 20}) {
                    Resultado r = Serial.ejecutar(archivo.toString(), 7, n, b);
                    comprobar(r.dMin == Math.sqrt(min) && r.dMax == Math.sqrt(max), "Oraculo n=" + n);
                    double d = 0;
                    for (int k = 0; k < n; k++) { double v = puntos[r.maxI][k] - puntos[r.maxJ][k]; d += v * v; }
                    comprobar(Math.sqrt(d) == r.dMax, "Indices maximos del oraculo");
                }
            }
            falla(() -> Serial.ejecutar(archivo.toString(), 8, 1000, 2));
            Files.write(archivo, new byte[] {0, 0, 0});
            falla(() -> Serial.ejecutar(archivo.toString(), 7, 1000, 2));
        } finally { Files.deleteIfExists(archivo); }
        falla(() -> Serial.ejecutar("datos/datos.bin", 1, 3, 50));
        falla(() -> Serial.ejecutar("datos/datos.bin", 1000, 0, 50));
        falla(() -> Serial.ejecutar("datos/datos.bin", 1000, 3, 0));
        Resultado a = new Resultado(), b = new Resultado(), total = new Resultado();
        a.actualizar(9, 0, 1); a.actualizar(25, 0, 2);
        b.actualizar(0, 1, 3); b.actualizar(100, 2, 3);
        total.combinar(new Resultado()); total.combinar(a); total.combinar(b);
        comprobar(total.dMin == 0 && total.dMax == 100 && total.minI == 1 && total.maxI == 2, "Reduccion cuadrados");
        comprobar(a.dMin == 9 && b.dMax == 100, "No modifica fuentes");
        total.finalizar(); total.finalizar();
        comprobar(total.dMin == 0 && total.dMax == 10, "Raices idempotentes");
        falla(() -> total.actualizar(1, 0, 1));
        falla(() -> a.combinar(total));
        falla(() -> a.actualizar(Double.NaN, 0, 1));
        falla(() -> a.actualizar(-1, 0, 1));
        falla(() -> new Resultado().finalizar());
        System.out.println("OK: " + comprobaciones + " comprobaciones; verificacion, oraculo, bloques, reduccion y errores.");
    }
}
