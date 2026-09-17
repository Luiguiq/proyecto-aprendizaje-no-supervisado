import java.io.IOException;

/**
 * Entrega del Integrante 4.
 *
 * Un hilo de calculo. Cada instancia abre su PROPIO LectorBloques (que valida
 * la cabecera del archivo en su propio constructor) y acumula su PROPIO
 * Resultado local, sin compartir estado mutable con otros hilos salvo el
 * Repartidor (a traves de su metodo synchronized siguientePar()).
 *
 * El hilo pide pares de bloques (bi, bj) hasta que el repartidor no tiene
 * mas. Por cada par, calcula la distancia al cuadrado entre los puntos
 * correspondientes (raiz cuadrada solo al reportar) y actualiza su
 * Resultado local. La sincronizacion con otros hilos ocurre unicamente al
 * pedir el siguiente par; el doble bucle de distancias es completamente
 * local a cada hilo.
 */
public class TareaDistancia extends Thread {

    private final String archivo;
    private final int N;
    private final int n;
    private final int puntosPorBloque;
    private final Repartidor repartidor;

    /** Resultado local de este hilo. Se combina al final en el hilo principal. */
    public final Resultado resultado = new Resultado();

    /** Cuantos pares (bi, bj) proceso este hilo. Sirve para la prueba obligatoria del bloque forzado. */
    public volatile int paresProcesados = 0;

    private volatile Exception error;

    public TareaDistancia(String archivo, int N, int n, int puntosPorBloque, Repartidor repartidor) {
        super("TareaDistancia");
        this.archivo = archivo;
        this.N = N;
        this.n = n;
        this.puntosPorBloque = puntosPorBloque;
        this.repartidor = repartidor;
    }

    @Override
    public void run() {
        LectorBloques lector = null;
        try {
            lector = new LectorBloques(archivo, n, N);
            int[] par;
            while ((par = repartidor.siguientePar()) != null) {
                procesarPar(lector, par[0], par[1]);
                paresProcesados++;
            }
        } catch (Exception e) {
            error = e;
        } finally {
            if (lector != null) {
                try {
                    lector.cerrar();
                } catch (IOException e) {
                    if (error == null) {
                        error = e;
                    }
                }
            }
        }
    }

    private void procesarPar(LectorBloques lector, int bi, int bj) throws IOException {
        int iniI = bi * puntosPorBloque;
        int cantI = Math.min(puntosPorBloque, N - iniI);
        double[] bloqueI = copiar(lector.leerBloque(iniI, cantI), cantI * n);

        if (bi == bj) {
            // Mismo bloque: solo comparaciones i < j dentro del bloque
            // (la diagonal pesa la mitad que los pares fuera de la diagonal).
            for (int a = 0; a < cantI; a++) {
                for (int b = a + 1; b < cantI; b++) {
                    double d2 = distanciaCuadrado(bloqueI, a, bloqueI, b, n);
                    resultado.actualizar(d2, iniI + a, iniI + b);
                }
            }
        } else {
            int iniJ = bj * puntosPorBloque;
            int cantJ = Math.min(puntosPorBloque, N - iniJ);
            double[] bloqueJ = copiar(lector.leerBloque(iniJ, cantJ), cantJ * n);

            // Bloques distintos: todos contra todos entre los dos bloques.
            for (int a = 0; a < cantI; a++) {
                for (int b = 0; b < cantJ; b++) {
                    double d2 = distanciaCuadrado(bloqueI, a, bloqueJ, b, n);
                    resultado.actualizar(d2, iniI + a, iniJ + b);
                }
            }
        }
    }

    /**
     * Copia el bloque leido a un arreglo propio de este hilo. Es necesaria
     * porque LectorBloques reutiliza un buffer interno entre llamadas a
     * leerBloque: al leer el segundo bloque del par se sobrescribiria el
     * primero si no se copia antes.
     */
    private static double[] copiar(double[] origen, int cantidadDoubles) {
        double[] copia = new double[cantidadDoubles];
        System.arraycopy(origen, 0, copia, 0, cantidadDoubles);
        return copia;
    }

    private static double distanciaCuadrado(double[] p, int idxP, double[] q, int idxQ, int n) {
        double suma = 0.0;
        int baseP = idxP * n;
        int baseQ = idxQ * n;
        for (int k = 0; k < n; k++) {
            double diff = p[baseP + k] - q[baseQ + k];
            suma += diff * diff;
        }
        return suma;
    }

    public Exception getError() {
        return error;
    }
}
