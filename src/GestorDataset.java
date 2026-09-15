import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Integrante 1 - Generador y formato del dataset binario.
 *
 * FORMATO DEL ARCHIVO
 *   bytes 0..3   : int N  (cantidad de puntos)
 *   bytes 4..7   : int n  (dimensiones por punto)
 *   bytes 8..    : N*n valores double consecutivos, punto por punto
 *
 * El punto de indice i empieza en el byte:  8 + i*n*8
 * Todos los desplazamientos se calculan en long para no desbordar con N grande.
 *
 * FRONTERA CON LectorBloques (Integrante 2)
 *   Esta clase NO lee bloques para calculo. Solo genera, lee cabecera y valida.
 *   El metodo leerPunto existe solo para depuracion puntual.
 *   La lectura por bloques es responsabilidad exclusiva de LectorBloques.
 *
 * NOMBRES DE ARCHIVO
 *   datos.bin               -> versionado, N=1000, n=3, semilla=12345, demo en vivo
 *   datos_verificacion.bin  -> versionado, 30 puntos, 2D, x=i, y=i*i
 *   datos_N1000_n3.bin      -> generado por el Integrante 5, mismo contenido que
 *                              datos.bin pero con el nombre que el DriverPruebas
 *                              busca (patron datos_N<valor>_n<valor>.bin).
 *                              NO se versiona.
 *
 * REPRODUCIBILIDAD
 *   El generador usa un LCG de 64 bits con semilla fija. Con la misma semilla
 *   y los mismos N, n y rango, todos los integrantes obtienen el mismo archivo.
 */
public class GestorDataset {

    public static final int TAM_CABECERA = 8;
    public static final int TAM_DOUBLE   = 8;
    private static final int TAM_BUFFER_ESCRITURA = 1 << 20; // 1 MB

    // LCG de 64 bits, sin bibliotecas
    private long estado;

    public GestorDataset(long semilla) {
        this.estado = semilla;
    }

    /** Devuelve un double en [0.0, 1.0). Reproducible con la misma semilla. */
    private double siguienteDouble() {
        estado = estado * 6364136223846793005L + 1442695040888963407L;
        long bits = estado >>> 11;          // 53 bits altos
        return bits / 9007199254740992.0;   // 2^53
    }

    // ---------------------------------------------------------------
    // Generacion del dataset principal
    // ---------------------------------------------------------------
    public void generarDataset(String ruta, int N, int n, double rango) throws IOException {
        if (N <= 0 || n <= 0) {
            throw new IllegalArgumentException("N y n deben ser mayores que cero");
        }

        long inicio = System.currentTimeMillis();

        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(ruta), TAM_BUFFER_ESCRITURA))) {
            dos.writeInt(N);
            dos.writeInt(n);
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < n; j++) {
                    dos.writeDouble(siguienteDouble() * rango);
                }
            }
        }

        long fin = System.currentTimeMillis();
        long tamanio = tamanioEsperado(N, n);

        System.out.println("Dataset generado en: " + ruta);
        System.out.println("  N = " + N + "   n = " + n + "   rango = [0, " + rango + ")");
        System.out.println("  tamanio esperado = " + tamanio + " bytes  (" + (tamanio / 1048576.0) + " MB)");
        System.out.println("  tiempo = " + (fin - inicio) + " ms");
    }

    public static long tamanioEsperado(long N, long n) {
        return TAM_CABECERA + N * n * TAM_DOUBLE;
    }

    // ---------------------------------------------------------------
    // Lectura de cabecera y validacion
    // ---------------------------------------------------------------
    /** Devuelve un arreglo de 2 posiciones: [0]=N, [1]=n */
    public static int[] leerCabecera(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int[] cab = new int[2];
        cab[0] = raf.readInt();
        cab[1] = raf.readInt();
        return cab;
    }

    /** Verifica que el tamanio real del archivo coincida con lo que dice la cabecera. */
    public static void validarArchivo(RandomAccessFile raf) throws IOException {
        int[] cab = leerCabecera(raf);
        long esperado = tamanioEsperado(cab[0], cab[1]);
        long real = raf.length();
        if (esperado != real) {
            throw new IOException("Archivo corrupto o incompleto: esperado " + esperado
                    + " bytes, real " + real + " bytes");
        }
    }

    // ---------------------------------------------------------------
    // Generador chico de verificacion: 30 puntos en 2D
    //   x_i = i
    //   y_i = i*i
    // Sirve para que el resto verifique a mano minimos y maximos.
    // ---------------------------------------------------------------
    public static void generarDatasetVerificacion(String ruta) throws IOException {
        int N = 30, n = 2;
        try (DataOutputStream dos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(ruta)))) {
            dos.writeInt(N);
            dos.writeInt(n);
            for (int i = 0; i < N; i++) {
                dos.writeDouble((double) i);
                dos.writeDouble((double) (i * i));
            }
        }
        System.out.println("Dataset de verificacion (30 puntos, 2D) generado en: " + ruta);
        System.out.println("  Punto[i] = (i, i*i)");
        System.out.println("  Distancia minima esperada: P[0] a P[1] = sqrt(2) = 1.4142135623730951");
        System.out.println("  Distancia maxima esperada: P[0] a P[29]");
    }

    // ---------------------------------------------------------------
    // Lectura de UN punto: solo para depuracion.
    // NO usar dentro del calculo de distancias.
    // ---------------------------------------------------------------
    public static void leerPunto(RandomAccessFile raf, long indicePunto, int n, double[] destino)
            throws IOException {
        long posicion = TAM_CABECERA + indicePunto * n * TAM_DOUBLE;
        raf.seek(posicion);
        for (int i = 0; i < n; i++) {
            destino[i] = raf.readDouble();
        }
    }

    // ---------------------------------------------------------------
    // Main con modos de uso
    // ---------------------------------------------------------------
    public static void main(String[] args) throws IOException {
        if (args.length == 0) { ayuda(); return; }

        switch (args[0]) {
            case "generar":
                if (args.length < 5) { ayuda(); return; }
                String ruta  = args[1];
                int N        = Integer.parseInt(args[2]);
                int n        = Integer.parseInt(args[3]);
                long semilla = Long.parseLong(args[4]);
                double rango = (args.length >= 6) ? Double.parseDouble(args[5]) : 100.0;
                new GestorDataset(semilla).generarDataset(ruta, N, n, rango);
                break;

            case "verificacion":
                if (args.length < 2) { ayuda(); return; }
                generarDatasetVerificacion(args[1]);
                break;

            case "info":
                if (args.length < 2) { ayuda(); return; }
                try (RandomAccessFile raf = new RandomAccessFile(args[1], "r")) {
                    int[] cab = leerCabecera(raf);
                    System.out.println("N = " + cab[0] + "   n = " + cab[1]);
                    System.out.println("tamanio real     = " + raf.length() + " bytes");
                    System.out.println("tamanio esperado = " + tamanioEsperado(cab[0], cab[1]) + " bytes");
                    validarArchivo(raf);
                    System.out.println("Archivo OK");
                }
                break;

            case "ver":
                if (args.length < 3) { ayuda(); return; }
                try (RandomAccessFile raf = new RandomAccessFile(args[1], "r")) {
                    int[] cab = leerCabecera(raf);
                    int dims = cab[1];
                    long desde = Long.parseLong(args[2]);
                    long hasta = (args.length >= 4) ? Long.parseLong(args[3]) : desde;
                    double[] punto = new double[dims];
                    for (long i = desde; i <= hasta && i < cab[0]; i++) {
                        leerPunto(raf, i, dims, punto);
                        StringBuilder sb = new StringBuilder();
                        sb.append("Punto[").append(i).append("] = ");
for (int j = 0; j < dims; j++) {
                            if (j > 0) sb.append(", ");
                            sb.append(punto[j]);
                        }
                        System.out.println(sb.toString());
                    }
                }
                break;

            default:
                ayuda();
        }
    }

    private static void ayuda() {
        System.out.println("Uso:");
        System.out.println("  java GestorDataset generar <archivo> <N> <n> <semilla> [rango]");
        System.out.println("  java GestorDataset verificacion <archivo>");
        System.out.println("  java GestorDataset info <archivo>");
        System.out.println("  java GestorDataset ver <archivo> <desde> [hasta]");
    }
}