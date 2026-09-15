import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Integrante 2 - Capa de E/S por bloques.
 *
 * Unica puerta de entrada a la lectura de disco para el calculo de distancias.
 * Serial (Integrante 3) y Paralelo/TareaDistancia (Integrante 4) NO abren
 * RandomAccessFile: usan esta clase.
 *
 * DESCRIPTOR POR HILO
 *   Cada hilo crea su propio LectorBloques, que abre su propio RandomAccessFile.
 *   Asi los seek() de un hilo nunca afectan al puntero de otro.
 *
 * VALIDACION EN EL CONSTRUCTOR
 *   Esta clase NO confia en los N y n que le pasa quien la instancia:
 *     1) abre el RandomAccessFile,
 *     2) lee la cabecera con GestorDataset.leerCabecera (N y n salen del archivo),
 *     3) valida la integridad con GestorDataset.validarArchivo (falla temprano
 *        si el archivo esta corrupto o incompleto),
 *     4) si los parametros del llamador no coinciden con la cabecera, lanza
 *        excepcion inmediatamente.
 *
 * LECTURA DE BLOQUES
 *   El bloque i contiene los puntos [i*puntosPorBloque, min((i+1)*puntosPorBloque, N)-1].
 *   El ultimo bloque puede ser incompleto: el llamador pasa la cantidad real y
 *   este metodo la ajusta al resto disponible del archivo.
 *   La conversion de byte a double se hace a mano, en big-endian (regla 1.2 / 9).
 *   El buffer de bytes se reutiliza en cada llamada (cero basura).
 */
public class LectorBloques {

    private final RandomAccessFile raf;
    private final int N;
    private final int n;
    private final int puntosPorBloque;
    private final byte[] buffer;

    /**
     * @param ruta            ruta del archivo binario (datos.bin o generados).
     * @param NEsperado       N que el llamador cree que hay en el archivo.
     * @param nEsperado       n que el llamador cree que hay en el archivo.
     * @param puntosPorBloque cantidad maxima de puntos por bloque (>= 1).
     */
    public LectorBloques(String ruta, int NEsperado, int nEsperado, int puntosPorBloque)
            throws IOException {
        if (puntosPorBloque <= 0) {
            throw new IllegalArgumentException("puntosPorBloque debe ser >= 1");
        }
        RandomAccessFile f = null;
        try {
            f = new RandomAccessFile(ruta, "r");
            int[] cab = GestorDataset.leerCabecera(f);
            GestorDataset.validarArchivo(f);
            if (cab[0] != NEsperado || cab[1] != nEsperado) {
                throw new IOException("La cabecera del archivo no coincide con los parametros"
                        + " del llamador: archivo N=" + cab[0] + " n=" + cab[1]
                        + " vs esperado N=" + NEsperado + " n=" + nEsperado);
            }
            this.raf = f;
            this.N = cab[0];
            this.n = cab[1];
            this.puntosPorBloque = puntosPorBloque;
            this.buffer = new byte[calcularBytesBuffer(puntosPorBloque, n)];
        } catch (IOException | RuntimeException e) {
            if (f != null) {
                try { f.close(); } catch (IOException ignorada) { /* ya estamos fallando */ }
            }
            throw e;
        }
    }

    // ---------------------------------------------------------------
    // Consulta
    // ---------------------------------------------------------------
    public int getN() { return N; }

    public int getn() { return n; }

    public int getPuntosPorBloque() { return puntosPorBloque; }

    /** Cantidad de bloques (el ultimo puede ser incompleto). */
    public int bloquesTotales() {
        return (N + puntosPorBloque - 1) / puntosPorBloque;
    }

    /** Cantidad real de puntos que tiene el bloque iBloque (ajusta el ultimo). */
    public int cantidadRealBloque(int iBloque) {
        long primer = indicePrimerPuntoDelBloque(iBloque);
        if (primer >= N) {
            throw new IllegalArgumentException("Bloque " + iBloque
                    + " fuera de rango: hay " + bloquesTotales() + " bloques");
        }
        long resto = N - primer;
        return (int) Math.min(puntosPorBloque, resto);
    }

    // ---------------------------------------------------------------
    // Lectura de un bloque
    // ---------------------------------------------------------------
    /**
     * Lee hasta @param cantidad puntos del bloque iBloque en Big-endian manual.
     *
     * @param iBloque indice del bloque a leer.
     * @param cantidad cantidad de puntos pedida (el llamador pasa la real para el
     *                 ultimo bloque; aqui se ajusta al resto disponible del archivo).
     * @param dims     dimensiones esperadas (debe coincidir con la cabecera).
     * @param destino  arreglo del llamador de al menos cantidad*dims doubles;
     *                 se reutiliza entre llamadas (cero basura).
     * @return la cantidad de puntos realmente leidos.
     */
    public int leerBloque(int iBloque, int cantidad, int dims, double[] destino)
            throws IOException {
        if (dims != n) {
            throw new IllegalArgumentException("dimensiones " + dims
                    + " no coinciden con la cabecera (n=" + n + ")");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("cantidad debe ser >= 1");
        }

        long primer = indicePrimerPuntoDelBloque(iBloque);
        if (primer >= N) {
            throw new IllegalArgumentException("Bloque " + iBloque
                    + " fuera de rango: hay " + bloquesTotales() + " bloques");
        }
        long resto = N - primer;
        if (cantidad > resto) {
            cantidad = (int) resto;
        }

        long posicion = GestorDataset.TAM_CABECERA
                + primer * (long) n * GestorDataset.TAM_DOUBLE;
        int bytes = cantidad * n * GestorDataset.TAM_DOUBLE;

        raf.seek(posicion);
        raf.readFully(buffer, 0, bytes);

        int k = 0;
        for (int p = 0; p < cantidad; p++) {
            for (int d = 0; d < n; d++) {
                destino[k] = deBytesABigEndian(buffer, k * 8);
                k++;
            }
        }
        return cantidad;
    }

    private long indicePrimerPuntoDelBloque(int iBloque) {
        if (iBloque < 0) {
            throw new IllegalArgumentException("bloque debe ser >= 0");
        }
        return (long) iBloque * puntosPorBloque;
    }

    public void cerrar() throws IOException {
        raf.close();
    }

    // ---------------------------------------------------------------
    // Calculo de tamanos (seguridad contra memoria y desbordes)
    // ---------------------------------------------------------------
    /**
     * Cantidad maxima de puntos por bloque segun la memoria disponible.
     * Se apunta a bloques de ~8 MiB de datos (o 1/4 del heap si es menor)
     * para no saturar de memoria y no generar basura con bloques gigantes.
     */
    public static int calcularPuntosPorBloque(int dims) {
        if (dims <= 0) {
            throw new IllegalArgumentException("dims debe ser >= 1");
        }
        long maxMem = Runtime.getRuntime().maxMemory();
        long presupuesto = Math.min(8L * 1024 * 1024, maxMem / 4);
        long puntoEnBytes = (long) dims * GestorDataset.TAM_DOUBLE;
        long puntos = Math.max(1L, presupuesto / puntoEnBytes);

        long limiteInt = ((long) Integer.MAX_VALUE) / puntoEnBytes;
        if (puntos > limiteInt) {
            puntos = limiteInt;
        }
        return (int) puntos;
    }

    /** Tamanio en bytes del buffer de un bloque, validando contra Integer.MAX_VALUE. */
    public static int calcularBytesBuffer(int puntosPorBloque, int dims) {
        if (puntosPorBloque <= 0 || dims <= 0) {
            throw new IllegalArgumentException("puntosPorBloque y dims deben ser >= 1");
        }
        long bytes = (long) puntosPorBloque * dims * GestorDataset.TAM_DOUBLE;
        if (bytes > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Buffer de bloque demasiado grande: "
                    + bytes + " bytes supera Integer.MAX_VALUE. Reducir puntosPorBloque.");
        }
        return (int) bytes;
    }

    /** Convierte 8 bytes big-endian a double a mano (regla: sin readDouble). */
    private static double deBytesABigEndian(byte[] b, int off) {
        long bits = ((long) (b[off] & 0xFF) << 56)
                  | ((long) (b[off + 1] & 0xFF) << 48)
                  | ((long) (b[off + 2] & 0xFF) << 40)
                  | ((long) (b[off + 3] & 0xFF) << 32)
                  | ((long) (b[off + 4] & 0xFF) << 24)
                  | ((long) (b[off + 5] & 0xFF) << 16)
                  | ((long) (b[off + 6] & 0xFF) << 8)
                  | (long) (b[off + 7] & 0xFF);
        return Double.longBitsToDouble(bits);
    }
}