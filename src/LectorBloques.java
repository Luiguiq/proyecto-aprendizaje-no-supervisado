import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * NOTA: Esta clase NO es entrega del Integrante 4. Es responsabilidad del
 * Integrante 2 (src/LectorBloques.java real del equipo). Se incluye aqui una
 * version minima y compatible con el contrato de la seccion 1.9 del plan
 * (valida cabecera en su propio constructor, falla temprano, unica puerta a
 * RandomAccessFile), solo para poder compilar y probar de forma independiente
 * Paralelo.java, TareaDistancia.java y Repartidor.java.
 *
 * Para la entrega real, este archivo debe reemplazarse por el oficial del
 * Integrante 2 sin tocar el resto de las clases del Integrante 4: el
 * contrato publico (constructor, leerBloque, cerrar) es el mismo.
 */
public class LectorBloques {

    private final RandomAccessFile raf;
    private final int N;
    private final int n;

    // Buffers reutilizados entre llamadas a leerBloque para no generar basura.
    private byte[] bufferBytes;
    private double[] bufferDoubles;

    public LectorBloques(String ruta, int nEsperado, int nTotalEsperado) throws IOException {
        this.raf = new RandomAccessFile(ruta, "r");
        int[] cabecera = GestorDataset.leerCabecera(raf);
        this.N = cabecera[0];
        this.n = cabecera[1];
        GestorDataset.validarArchivo(raf, N, n);
        if (this.n != nEsperado || this.N != nTotalEsperado) {
            raf.close();
            throw new IllegalArgumentException(
                    "Parametros del llamador no coinciden con la cabecera del archivo: "
                            + "esperado N=" + nTotalEsperado + " n=" + nEsperado
                            + ", real N=" + this.N + " n=" + this.n);
        }
    }

    public int getN() {
        return N;
    }

    public int getDimensiones() {
        return n;
    }

    public static int calcularTamanioBuffer(int cantidad, int n) {
        long bytesLong = (long) cantidad * (long) n * (long) GestorDataset.TAM_DOUBLE;
        if (bytesLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Bloque demasiado grande para un buffer de bytes: " + bytesLong);
        }
        return (int) bytesLong;
    }

    public static int puntosPorBloqueSegunMemoria(long memoriaDisponibleBytes, int n) {
        long bytesPorPunto = (long) n * GestorDataset.TAM_DOUBLE;
        long puntos = memoriaDisponibleBytes / bytesPorPunto;
        if (puntos > Integer.MAX_VALUE) {
            puntos = Integer.MAX_VALUE;
        }
        return (int) Math.max(1, puntos);
    }

    /**
     * Lee "cantidad" puntos a partir del indice global indiceInicio.
     * Devuelve un arreglo de doubles de tamanio cantidad*n (coordenada k del
     * punto local p esta en la posicion p*n+k). El arreglo devuelto es un
     * buffer interno reutilizado: el llamador debe copiarlo si necesita
     * conservarlo mientras se hace otra lectura con el mismo LectorBloques.
     */
    public double[] leerBloque(int indiceInicio, int cantidad) throws IOException {
        long offset = GestorDataset.TAM_CABECERA + (long) indiceInicio * n * GestorDataset.TAM_DOUBLE;
        raf.seek(offset);

        int bytesNecesarios = calcularTamanioBuffer(cantidad, n);
        if (bufferBytes == null || bufferBytes.length < bytesNecesarios) {
            bufferBytes = new byte[bytesNecesarios];
        }
        raf.readFully(bufferBytes, 0, bytesNecesarios);

        int totalDoubles = cantidad * n;
        if (bufferDoubles == null || bufferDoubles.length < totalDoubles) {
            bufferDoubles = new double[totalDoubles];
        }

        int idxByte = 0;
        for (int i = 0; i < totalDoubles; i++) {
            long bits = 0L;
            for (int b = 0; b < GestorDataset.TAM_DOUBLE; b++) {
                bits = (bits << 8) | (bufferBytes[idxByte++] & 0xFFL);
            }
            bufferDoubles[i] = Double.longBitsToDouble(bits);
        }
        return bufferDoubles;
    }

    public void cerrar() throws IOException {
        raf.close();
    }
}
