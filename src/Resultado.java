/**
 * NOTA: Esta clase NO es entrega del Integrante 4. Es responsabilidad del
 * Integrante 3 (contrato congelado, seccion 1.8 del plan). Se incluye aqui
 * una version minima y compatible con ese contrato exacto (campos, actualizar
 * y combinar) solo para poder compilar y probar de forma independiente el
 * trabajo del Integrante 4. Para la entrega real, reemplazar por el
 * Resultado.java oficial del Integrante 3 sin tocar TareaDistancia.java ni
 * Paralelo.java: usan exactamente estos mismos metodos.
 */
public class Resultado {

    public double dMin = Double.MAX_VALUE;
    public double dMax = -1.0;
    public int minI = -1;
    public int minJ = -1;
    public int maxI = -1;
    public int maxJ = -1;
    public double tiempoMs;

    /** dCuadrado es la distancia AL CUADRADO entre los puntos i y j. */
    public void actualizar(double dCuadrado, int i, int j) {
        if (dCuadrado < dMin) {
            dMin = dCuadrado;
            minI = i;
            minJ = j;
        }
        if (dCuadrado > dMax) {
            dMax = dCuadrado;
            maxI = i;
            maxJ = j;
        }
    }

    /** Reduce dos resultados en uno: minimo de los minimos, maximo de los maximos. */
    public void combinar(Resultado otro) {
        if (otro.dMin < this.dMin) {
            this.dMin = otro.dMin;
            this.minI = otro.minI;
            this.minJ = otro.minJ;
        }
        if (otro.dMax > this.dMax) {
            this.dMax = otro.dMax;
            this.maxI = otro.maxI;
            this.maxJ = otro.maxJ;
        }
    }

    public double raizMin() {
        return Math.sqrt(dMin);
    }

    public double raizMax() {
        return Math.sqrt(dMax);
    }
}
