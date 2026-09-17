/**
 * Entrega del Integrante 4.
 *
 * Cola dinamica de pares triangulares de bloques (bi, bj) con bi <= bj.
 * Cada hilo pide el siguiente par apenas termina el suyo mediante
 * siguientePar(), un metodo synchronized. No usa ArrayList: la lista
 * triangular se genera una sola vez en el constructor con dos arreglos
 * primitivos paralelos.
 *
 * La sincronizacion ocurre unicamente al pedir el par (una operacion muy
 * corta: leer y avanzar un indice), nunca dentro del bucle interno de
 * calculo de distancias que hace cada hilo.
 */
public class Repartidor {

    private final int totalBloques;
    private final int totalPares;
    private final int[] listaBi;
    private final int[] listaBj;
    private int siguienteIndice;

    public Repartidor(int totalBloques) {
        if (totalBloques <= 0) {
            throw new IllegalArgumentException("totalBloques debe ser mayor que 0");
        }
        this.totalBloques = totalBloques;
        this.totalPares = totalBloques * (totalBloques + 1) / 2;
        this.listaBi = new int[totalPares];
        this.listaBj = new int[totalPares];

        int k = 0;
        for (int bi = 0; bi < totalBloques; bi++) {
            for (int bj = bi; bj < totalBloques; bj++) {
                listaBi[k] = bi;
                listaBj[k] = bj;
                k++;
            }
        }
        this.siguienteIndice = 0;
    }

    /**
     * Devuelve el siguiente par {bi, bj} pendiente, o null si ya no quedan.
     * Unico punto de sincronizacion de todo el reparto de trabajo.
     */
    public synchronized int[] siguientePar() {
        if (siguienteIndice >= totalPares) {
            return null;
        }
        int[] par = { listaBi[siguienteIndice], listaBj[siguienteIndice] };
        siguienteIndice++;
        return par;
    }

    public int getTotalBloques() {
        return totalBloques;
    }

    public int getTotalPares() {
        return totalPares;
    }
}
