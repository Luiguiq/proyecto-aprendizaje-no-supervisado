
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import javax.imageio.ImageIO;

public class GeneradorGraficas {

    static class Registro {

        int N, n, T, puntosPorBloque;
        double tiempoMs;

        Registro(int N, int n, int T, int puntosPorBloque, double tiempoMs) {
            this.N = N;
            this.n = n;
            this.T = T;
            this.puntosPorBloque = puntosPorBloque;
            this.tiempoMs = tiempoMs;
        }
    }

    public static void main(String[] args) {
        new File("graficas").mkdirs();
        Registro[] datos = cargarCSV("pruebas/resultados.csv");
        if (datos == null || datos.length == 0) {
            System.err.println("Error: no hay datos en pruebas/resultados.csv");
            return;
        }

        crearTiempoVsN(datos);
        crearSpeedupVsT(datos);
        crearEficienciaVsT(datos);
        crearTiempoVsDim(datos);
    }

    // 1. tiempo_vs_N.png
    private static void crearTiempoVsN(Registro[] datos) {
        int w = 800, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepararLienzo(img, w, h, "Escalabilidad: Tiempo vs N (n=3)");

        int ox = 100, oy = 420, gw = 640, gh = 330;
        dibujarEjes(g, ox, oy, gw, gh, "Cantidad de puntos (N)", "Tiempo (ms)");

        int[] hilos = {1, 2, 4, 8};
        Color[] cols = {Color.RED, Color.BLUE, new Color(0, 160, 0), Color.MAGENTA};
        int[] ns = {1000, 10000, 100000};

        for (int i = 0; i < ns.length; i++) {
            int px = ox + (int) ((double) i / (ns.length - 1) * gw);
            g.setColor(Color.DARK_GRAY);
            g.drawString("N=" + ns[i], px - 25, oy + 25);
            g.drawLine(px, oy - 4, px, oy + 4);
        }

        for (int hIdx = 0; hIdx < hilos.length; hIdx++) {
            int T = hilos[hIdx];
            g.setColor(cols[hIdx]);
            g.drawString("T=" + T, ox + 60 + hIdx * 90, 65);

            int prevX = -1, prevY = -1;
            for (int i = 0; i < ns.length; i++) {
                double tMs = getTiempo(datos, ns[i], 3, T, false);
                if (tMs > 0) {
                    int px = ox + (int) ((double) i / (ns.length - 1) * gw);
                    double norm = Math.log10(Math.max(1.0, tMs)) / 6.0;
                    if (norm > 1.0) {
                        norm = 1.0;
                    }
                    int py = oy - (int) (norm * gh);

                    g.fillOval(px - 5, py - 5, 10, 10);
                    g.drawString(String.format("%.1f", tMs), px - 18, py - 8);

                    if (prevX != -1) {
                        g.setStroke(new BasicStroke(2));
                        g.drawLine(prevX, prevY, px, py);
                    }
                    prevX = px;
                    prevY = py;
                }
            }
        }
        g.dispose();
        guardar(img, "graficas/tiempo_vs_dimension_n.png");
    }

    // 2. speedup_vs_T.png
    private static void crearSpeedupVsT(Registro[] datos) {
        int w = 800, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepararLienzo(img, w, h, "Speedup S(T) (N=10000, n=3)");

        int ox = 100, oy = 420, gw = 640, gh = 330;
        dibujarEjes(g, ox, oy, gw, gh, "Numero de Hilos (T)", "Speedup S(T)");

        double tSerial = getTiempo(datos, 10000, 3, 1, false);
        int[] hilos = {1, 2, 4, 8};

        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        g.drawLine(ox, oy - (int) (1.0 / 8.0 * gh), ox + gw, oy - gh);
        g.drawString("Ideal S(T) = T", ox + gw - 110, oy - gh + 15);

        g.setColor(Color.BLUE);
        g.setStroke(new BasicStroke(2));
        int prevX = -1, prevY = -1;
        for (int i = 0; i < hilos.length; i++) {
            int T = hilos[i];
            double tPar = getTiempo(datos, 10000, 3, T, false);
            double speedup = (tPar > 0) ? (tSerial / tPar) : 1.0;

            int px = ox + (int) ((double) i / (hilos.length - 1) * gw);
            int py = oy - (int) (Math.min(8.0, speedup) / 8.0 * gh);

            g.setColor(Color.BLACK);
            g.drawString("T=" + T, px - 10, oy + 25);
            g.setColor(Color.BLUE);
            g.fillOval(px - 5, py - 5, 10, 10);
            g.drawString(String.format("%.2f", speedup), px - 10, py - 10);

            if (prevX != -1) {
                g.drawLine(prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
        }
        g.dispose();
        guardar(img, "graficas/speedup_vs_T.png");
    }

    // 3. eficiencia_vs_T.png
    private static void crearEficienciaVsT(Registro[] datos) {
        int w = 800, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepararLienzo(img, w, h, "Eficiencia E(T) (N=10000, n=3)");

        int ox = 100, oy = 420, gw = 640, gh = 330;
        dibujarEjes(g, ox, oy, gw, gh, "Numero de Hilos (T)", "Eficiencia E(T)");

        double tSerial = getTiempo(datos, 10000, 3, 1, false);
        int[] hilos = {1, 2, 4, 8};

        g.setColor(Color.LIGHT_GRAY);
        g.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0));
        int pyIdeal = oy - (int) ((1.0 / 1.2) * gh);
        g.drawLine(ox, pyIdeal, ox + gw, pyIdeal);
        g.drawString("Ideal E = 1.0", ox + gw - 100, pyIdeal - 5);

        g.setColor(new Color(0, 150, 0));
        g.setStroke(new BasicStroke(2));
        int prevX = -1, prevY = -1;
        for (int i = 0; i < hilos.length; i++) {
            int T = hilos[i];
            double tPar = getTiempo(datos, 10000, 3, T, false);
            double speedup = (tPar > 0) ? (tSerial / tPar) : 1.0;
            double ef = speedup / T;

            int px = ox + (int) ((double) i / (hilos.length - 1) * gw);
            int py = oy - (int) ((Math.min(1.2, ef) / 1.2) * gh);

            g.setColor(Color.BLACK);
            g.drawString("T=" + T, px - 10, oy + 25);
            g.setColor(new Color(0, 150, 0));
            g.fillRect(px - 5, py - 5, 10, 10);
            g.drawString(String.format("%.2f", ef), px - 10, py - 10);

            if (prevX != -1) {
                g.drawLine(prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
        }
        g.dispose();
        guardar(img, "graficas/eficiencia_vs_T.png");
    }

    // 4. tiempo_vs_n.png
    private static void crearTiempoVsDim(Registro[] datos) {
        int w = 800, h = 500;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = prepararLienzo(img, w, h, "Tiempo vs Dimension n (N=10000, T=4)");

        int ox = 100, oy = 420, gw = 640, gh = 330;
        dibujarEjes(g, ox, oy, gw, gh, "Dimension (n)", "Tiempo (ms)");

        int[] dims = {2, 3, 10, 100, 1000};
        g.setColor(Color.MAGENTA);
        g.setStroke(new BasicStroke(2));

        int prevX = -1, prevY = -1;
        for (int i = 0; i < dims.length; i++) {
            int n = dims[i];
            double t = getTiempo(datos, 10000, n, 4, false);
            int px = ox + (int) ((double) i / (dims.length - 1) * gw);
            double norm = Math.log10(Math.max(1.0, t)) / 6.0;
            int py = oy - (int) (norm * gh);

            g.setColor(Color.BLACK);
            g.drawString("n=" + n, px - 12, oy + 25);
            g.setColor(Color.MAGENTA);
            g.fillOval(px - 5, py - 5, 10, 10);
            g.drawString(String.format("%.1f", t), px - 15, py - 8);

            if (prevX != -1) {
                g.drawLine(prevX, prevY, px, py);
            }
            prevX = px;
            prevY = py;
        }
        g.dispose();
        guardar(img, "graficas/tiempo_vs_n.png");
    }

    private static double getTiempo(Registro[] datos, int N, int n, int T, boolean forzado) {
        for (int i = 0; i < datos.length; i++) {
            Registro r = datos[i];
            if (r.N == N && r.n == n && r.T == T) {
                if (forzado && r.puntosPorBloque == 50) {
                    return r.tiempoMs;
                }
                if (!forzado && r.puntosPorBloque != 50) {
                    return r.tiempoMs;
                }
            }
        }
        return -1.0;
    }

    private static Graphics2D prepararLienzo(BufferedImage img, int w, int h, String titulo) {
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);
        g.setColor(Color.BLACK);
        g.setFont(new Font("SansSerif", Font.BOLD, 16));
        g.drawString(titulo, 40, 35);
        g.setFont(new Font("SansSerif", Font.PLAIN, 12));
        return g;
    }

    private static void dibujarEjes(Graphics2D g, int ox, int oy, int gw, int gh, String ejeX, String ejeY) {
        g.setColor(Color.GRAY);
        g.drawLine(ox, oy, ox + gw, oy);
        g.drawLine(ox, oy, ox, oy - gh);
        g.drawString(ejeX, ox + gw / 2 - 40, oy + 45);
        g.drawString(ejeY, ox - 80, oy - gh / 2);
    }

    private static Registro[] cargarCSV(String ruta) {
        try {
            int lineas = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
                while (br.readLine() != null) {
                    lineas++;
                }
            }
            if (lineas <= 1) {
                return new Registro[0];
            }

            Registro[] arr = new Registro[lineas - 1];
            int idx = 0;
            try (BufferedReader br = new BufferedReader(new FileReader(ruta))) {
                br.readLine(); // saltar cabecera
                String l;
                while ((l = br.readLine()) != null) {
                    String[] p = l.split(",");
                    if (p.length >= 5) {
                        arr[idx++] = new Registro(
                                Integer.parseInt(p[0].trim()),
                                Integer.parseInt(p[1].trim()),
                                Integer.parseInt(p[2].trim()),
                                Integer.parseInt(p[3].trim()),
                                Double.parseDouble(p[4].trim())
                        );
                    }
                }
            }
            return arr;
        } catch (Exception e) {
            return new Registro[0];
        }
    }

    private static void guardar(BufferedImage img, String ruta) {
        try {
            ImageIO.write(img, "png", new File(ruta));
            System.out.println("OK: " + ruta);
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}
