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

    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.println("Uso:");
            System.out.println("  generar <ruta> <N> <n> <semilla>");
            System.out.println("  info <ruta>");
            return;
        }
        String comando = args[0];
        if (comando.equals("generar")) {
            String ruta = args[1];
            int N = Integer.parseInt(args[2]);
            int n = Integer.parseInt(args[3]);
            long semilla = Long.parseLong(args[4]);
            generar(ruta, N, n, semilla, 100.0);
            System.out.println("Generado " + ruta + " (N=" + N + ", n=" + n + ", semilla=" + semilla + ")");
        } else if (comando.equals("info")) {
            String ruta = args[1];
            try (RandomAccessFile raf = new RandomAccessFile(ruta, "r")) {
                int[] cab = leerCabecera(raf);
                long real = raf.length();
                long esperado = tamanioEsperado(cab[0], cab[1]);
                System.out.println("N = " + cab[0]);
                System.out.println("n = " + cab[1]);
                System.out.println("tamanio real     = " + real + " bytes");
                System.out.println("tamanio esperado = " + esperado + " bytes");
                if (real == esperado) {
                    System.out.println("Archivo OK");
                } else {
                    System.out.println("Archivo CORRUPTO");
                }
            }
        } else {
            System.out.println("Comando desconocido: " + comando);
        }
    }
}
