/**
 * Acumulador local, sin sincronizacion. dMin y dMax guardan cuadrados hasta
 * finalizar(); despues contienen distancias euclidianas listas para reportar.
 * Combinar todos los acumuladores ANTES de finalizar la reduccion paralela.
 */
public class Resultado {
    public double dMin = Double.POSITIVE_INFINITY;
    public double dMax = Double.NEGATIVE_INFINITY;
    public int minI = -1, minJ = -1, maxI = -1, maxJ = -1;
    public double tiempoMs;
    private boolean finalizado;

    public void actualizar(double distanciaCuadrada, int i, int j) {
        exigirAcumulador();
        if (!Double.isFinite(distanciaCuadrada) || distanciaCuadrada < 0 || i < 0 || j <= i) {
            throw new IllegalArgumentException("Se requiere distancia finita >= 0 e indices 0 <= i < j");
        }
        if (distanciaCuadrada < dMin) {
            dMin = distanciaCuadrada;
            minI = i;
            minJ = j;
        }
        if (distanciaCuadrada > dMax) {
            dMax = distanciaCuadrada;
            maxI = i;
            maxJ = j;
        }
    }

    /** Reduce cuadrados e indices; no suma tiempos. Un hilo sin pares es neutro. */
    public void combinar(Resultado otro) {
        exigirAcumulador();
        if (otro == null) throw new IllegalArgumentException("Resultado nulo");
        otro.exigirAcumulador();
        if (otro.minI < 0) return;
        if (otro.dMin < dMin) {
            dMin = otro.dMin;
            minI = otro.minI;
            minJ = otro.minJ;
        }
        if (otro.dMax > dMax) {
            dMax = otro.dMax;
            maxI = otro.maxI;
            maxJ = otro.maxJ;
        }
    }

    /** Aplica solo dos raices. Repetir esta llamada no cambia el resultado. */
    public void finalizar() {
        if (finalizado) return;
        if (minI < 0) throw new IllegalStateException("No hay pares de puntos");
        dMin = Math.sqrt(dMin);
        dMax = Math.sqrt(dMax);
        finalizado = true;
    }

    private void exigirAcumulador() {
        if (finalizado) throw new IllegalStateException("El resultado ya contiene raices");
    }
}
