import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * NOTA: Esta clase NO es entrega del Integrante 4. Es responsabilidad del
 * Integrante 1 (src/GestorDataset.java real del equipo). Se incluye aquí una
 * version minima y compatible con el contrato de la seccion 1.1 y 1.9 del
 * plan, solo para poder compilar, probar y generar evidencia de Paralelo.java,
 * TareaDistancia.java y Repartidor.java de forma independiente.
 *
 * El datos.bin generado por esta version NO tiene por que coincidir en hash
 * MD5 con el datos.bin oficial del equipo (el LCG exacto es responsabilidad
 * del Integrante 1). Para la entrega real se debe reemplazar este archivo por
 * el oficial del Integrante 1 sin tocar Paralelo.java, TareaDistancia.java ni
 * Repartidor.java: el contrato (leerCabecera, tamanioEsperado, validarArchivo)
 * es el mismo.
 */
public class GestorDataset {

    public static final int TAM_CABECERA = 8;   // 4 bytes N + 4 bytes n
    public static final int TAM_DOUBLE = 8;

    public static long tamanioEsperado(int N, int n) {
        return TAM_CABECERA + (long) N * (long) n * TAM_DOUBLE;
    }

    public static int[] leerCabecera(RandomAccessFile raf) throws IOException {
        raf.seek(0);
        int N = raf.readInt();
        int n = raf.readInt();
        return new int[] { N, n };
    }

    // Abre y cierra el archivo internamente, para no exponer RandomAccessFile
    // a otros consumidores (frontera de clases de la Seccion 1.2 y regla 1 de la Seccion 9).
    public static int[] leerCabecera(String ruta) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(ruta, "r")) {
            return leerCabecera(raf);
        }
    }

    public static void validarArchivo(RandomAccessFile raf, int N, int n) throws IOException {
        long esperado = tamanioEsperado(N, n);
        long real = raf.length();
        if (real != esperado) {
            throw new IOException("Archivo corrupto o incompleto: tamanio real=" + real
                    + " bytes, tamanio esperado=" + esperado + " bytes (N=" + N + ", n=" + n + ")");
        }
    }

    // LCG de 64 bits (constantes tipo PCG/Knuth). Sin java.util.Random.
    private static long lcgSiguiente(long estado) {
        return estado * 6364136223846793005L + 1442695040888963407L;
    }

    private static double siguienteDouble(long[] estado, double rango) {
        estado[0] = lcgSiguiente(estado[0]);
        long valorPositivo = estado[0] >>> 11; // 53 bits utiles
        double frac = valorPositivo / (double) (1L << 53);
        return frac * rango;
    }

    public static void generar(String ruta, int N, int n, long semilla, double rango) throws IOException {
        File archivo = new File(ruta);
        File dir = archivo.getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(archivo)))) {
            out.writeInt(N);
            out.writeInt(n);
            long[] estado = { semilla };
            for (int i = 0; i < N; i++) {
                for (int k = 0; k < n; k++) {
                    out.writeDouble(siguienteDouble(estado, rango));
                }
            }
        }
    }

    /** 30 puntos en 2D con x = i, y = i*i, para verificacion a mano. */
    public static void generarVerificacion(String ruta, int cantidad) throws IOException {
        File archivo = new File(ruta);
        File dir = archivo.getParentFile();
        if (dir != null) {
            dir.mkdirs();
        }
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(archivo)))) {
            out.writeInt(cantidad);
            out.writeInt(2);
            for (int i = 0; i < cantidad; i++) {
                out.writeDouble(i);
                out.writeDouble((double) i * (double) i);
            }
        }
    }

    /** Lectura de UN punto: solo para depuracion puntual (NO usar en el calculo). */
    public static void leerPunto(RandomAccessFile raf, long indicePunto, int n, double[] destino)
            throws IOException {
        long posicion = TAM_CABECERA + indicePunto * n * TAM_DOUBLE;
        raf.seek(posicion);
        for (int i = 0; i < n; i++) {
            destino[i] = raf.readDouble();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0) { ayuda(); return; }
        String comando = args[0];
        if (comando.equals("generar")) {
            if (args.length < 5) { ayuda(); return; }
            String ruta = args[1];
            int N = Integer.parseInt(args[2]);
            int n = Integer.parseInt(args[3]);
            long semilla = Long.parseLong(args[4]);
            double rango = (args.length >= 6) ? Double.parseDouble(args[5]) : 100.0;
            generar(ruta, N, n, semilla, rango);
            System.out.println("Generado " + ruta + " (N=" + N + ", n=" + n
                    + ", semilla=" + semilla + ", rango=[0," + rango + "))");
        } else if (comando.equals("verificacion")) {
            if (args.length < 2) { ayuda(); return; }
            generarVerificacion(args[1], 30);
            System.out.println("Generado " + args[1] + " (30 puntos, 2D, Punto[i]=(i, i*i))");
        } else if (comando.equals("info")) {
            if (args.length < 2) { ayuda(); return; }
            String ruta = args[1];
            try (RandomAccessFile raf = new RandomAccessFile(ruta, "r")) {
                int[] cab = leerCabecera(raf);
                long real = raf.length();
                long esperado = tamanioEsperado(cab[0], cab[1]);
                System.out.println("N = " + cab[0] + "   n = " + cab[1]);
                System.out.println("tamanio real     = " + real + " bytes");
                System.out.println("tamanio esperado = " + esperado + " bytes");
                if (real == esperado) {
                    System.out.println("Archivo OK");
                } else {
                    System.out.println("Archivo CORRUPTO");
                }
            }
        } else if (comando.equals("ver")) {
            if (args.length < 3) { ayuda(); return; }
            try (RandomAccessFile raf = new RandomAccessFile(args[1], "r")) {
                int[] cab = leerCabecera(raf);
                int dims = cab[1];
                long desde = Long.parseLong(args[2]);
                long hasta = (args.length >= 4) ? Long.parseLong(args[3]) : desde;
                double[] punto = new double[dims];
                for (long i = desde; i <= hasta && i < cab[0]; i++) {
                    leerPunto(raf, i, dims, punto);
                    StringBuilder sb = new StringBuilder("Punto[").append(i).append("] = ");
                    for (int j = 0; j < dims; j++) {
                        if (j > 0) sb.append(", ");
                        sb.append(punto[j]);
                    }
                    System.out.println(sb.toString());
                }
            }
        } else {
            System.out.println("Comando desconocido: " + comando);
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
