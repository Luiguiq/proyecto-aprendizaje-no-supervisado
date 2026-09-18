
import java.io.IOException;
import java.io.RandomAccessFile;

public class LectorBloques {

    private final RandomAccessFile raf;
    private final int N;
    private final int n;
    private final int puntosPorBloque;

    private byte[] bufferBytes;
    private double[] bufferDoubles;

    // Constructor esperado por Serial y PruebaLector
    public LectorBloques(String ruta, int N, int n, int puntosPorBloque) throws IOException {
        RandomAccessFile archivo = new RandomAccessFile(ruta, "r");
        try {
            int[] cabecera = GestorDataset.leerCabecera(archivo);
            this.N = cabecera[0];
            this.n = cabecera[1];
            GestorDataset.validarArchivo(archivo, this.N, this.n);
            if (this.N != N || this.n != n) {
                throw new IllegalArgumentException(
                        "Parametros no coinciden con la cabecera: esperado N=" + N + " n=" + n
                        + ", real N=" + this.N + " n=" + this.n);
            }
        } catch (IOException e) {
            try { archivo.close(); } catch (IOException ignorada) { }
            throw e;
        } catch (RuntimeException e) {
            try { archivo.close(); } catch (IOException ignorada) { }
            throw e;
        }
        this.raf = archivo;
        this.puntosPorBloque = puntosPorBloque;
    }

    // Constructor de compatibilidad (usado por versiones alternativas de TareaDistancia)
    public LectorBloques(String ruta, int nEsperado, int nTotalEsperado) throws IOException {
        this(ruta, nTotalEsperado, nEsperado, nTotalEsperado);
    }

    public int getN() {
        return N;
    }

    public int getDimensiones() {
        return n;
    }

    public int getPuntosPorBloque() {
        return puntosPorBloque;
    }

    public static int calcularTamanioBuffer(int cantidad, int n) {
        long bytesLong = (long) cantidad * (long) n * (long) GestorDataset.TAM_DOUBLE;
        if (bytesLong > Integer.MAX_VALUE) {
            throw new IllegalArgumentException("Bloque demasiado grande para un buffer de bytes: " + bytesLong);
        }
        return (int) bytesLong;
    }

    public static int calcularBytesBuffer(int cantidad, int n) {
        return calcularTamanioBuffer(cantidad, n);
    }

    public static int puntosPorBloqueSegunMemoria(long memoriaDisponibleBytes, int n) {
        long bytesPorPunto = (long) n * GestorDataset.TAM_DOUBLE;
        long puntos = memoriaDisponibleBytes / bytesPorPunto;
        if (puntos > Integer.MAX_VALUE) {
            puntos = Integer.MAX_VALUE;
        }
        return (int) Math.max(1, puntos);
    }

    public static int calcularPuntosPorBloque(int n) {
        // Buffer por defecto: 4MB para E/S razonable
        long memBytes = 4L * 1024 * 1024;
        return puntosPorBloqueSegunMemoria(memBytes, n);
    }

    public int bloquesTotales() {
        return (int) (((long) N + puntosPorBloque - 1) / puntosPorBloque);
    }

    public int cantidadRealBloque(int numBloque) {
        int inicio = numBloque * puntosPorBloque;
        if (inicio >= N) {
            return 0;
        }
        return Math.min(puntosPorBloque, N - inicio);
    }

    // Firma requerida por Serial.java y PruebaLector.java
    public int leerBloque(int numBloque, int cantidad, int dimensiones, double[] destino) throws IOException {
        long indiceInicio = (long) numBloque * puntosPorBloque;
        long offset = GestorDataset.TAM_CABECERA + indiceInicio * n * GestorDataset.TAM_DOUBLE;
        raf.seek(offset);

        int bytesNecesarios = calcularTamanioBuffer(cantidad, dimensiones);
        if (bufferBytes == null || bufferBytes.length < bytesNecesarios) {
            bufferBytes = new byte[bytesNecesarios];
        }
        raf.readFully(bufferBytes, 0, bytesNecesarios);

        int totalDoubles = cantidad * dimensiones;
        int idxByte = 0;
        for (int i = 0; i < totalDoubles; i++) {
            long bits = 0L;
            for (int b = 0; b < GestorDataset.TAM_DOUBLE; b++) {
                bits = (bits << 8) | (bufferBytes[idxByte++] & 0xFFL);
            }
            destino[i] = Double.longBitsToDouble(bits);
        }
        return cantidad;
    }

    // Firma requerida por TareaDistancia.java original
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
