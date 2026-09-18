import java.io.IOException;

/**
 * Integrante 2 - Main de depuracion de LectorBloques.
 *
 * Uso:
 *   java PruebaLector <archivo> [numBloque] [puntosPorBloque]
 *
 * Lee un bloque y lo imprime con el MISMO formato que "GestorDataset ver"
 * (Punto[i] = v0, v1, ...), para poder comparar la salida byte a byte
 * con el generador oficial y demostrar que LectorBloques lee identico.
 *
 * Tambien funciona como harness de fallo: si el archivo esta corrupto o la
 * cabecera no coincide con los parametros, el constructor de LectorBloques
 * lanza excepcion temprana (tambien se documenta en NOTA_TECNICA_E_S.md).
 */
public class PruebaLector {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) { ayuda(); return; }

        String archivo  = args[0];
        int numBloque   = (args.length >= 2) ? Integer.parseInt(args[1]) : 0;
        int puntosMax   = (args.length >= 3) ? Integer.parseInt(args[2]) : 0;

        int[] cab = GestorDataset.leerCabecera(archivo);
        int N = cab[0];
        int n = cab[1];
        if (puntosMax <= 0) {
            puntosMax = LectorBloques.calcularPuntosPorBloque(n);
        }

        LectorBloques lector = new LectorBloques(archivo, N, n, puntosMax);
        try {
            int totalBloques = lector.bloquesTotales();
            if (numBloque < 0 || numBloque >= totalBloques) {
                System.out.println("Error: bloque " + numBloque + " fuera de rango"
                        + " (bloques = 0.." + (totalBloques - 1) + ")");
                return;
            }

            int cantidad = lector.cantidadRealBloque(numBloque);
            double[] bloque = new double[cantidad * n];
            int leidos = lector.leerBloque(numBloque, cantidad, n, bloque);

            System.out.println("=== LectorBloques: " + archivo + " ===");
            System.out.println("N = " + N + "  n = " + n + "  puntosPorBloque = " + puntosMax);
            System.out.println("bloques totales = " + totalBloques
                    + "  bloque pedido = " + numBloque
                    + "  puntos leidos = " + leidos);

            for (int i = 0; i < leidos; i++) {
                int indiceAbs = numBloque * puntosMax + i;
                StringBuilder sb = new StringBuilder("Punto[" + indiceAbs + "] = ");
                for (int d = 0; d < n; d++) {
                    if (d > 0) sb.append(", ");
                    sb.append(bloque[i * n + d]);
                }
                System.out.println(sb.toString());
            }
            System.out.println("=== fin bloque " + numBloque + " ===");
        } finally {
            lector.cerrar();
        }
    }

    private static void ayuda() {
        System.out.println("Uso: java PruebaLector <archivo> [numBloque] [puntosPorBloque]");
    }
}