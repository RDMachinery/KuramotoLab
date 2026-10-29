/*
 * KuramotoLab.java - an interactive laboratory for the Kuramoto model of
 * coupled oscillators: watch synchrony emerge from disorder.
 *
 * Run:   java KuramotoLab.java        (Java 11+, no dependencies)
 *
 * THE MODEL
 *   Ring view (all-to-all coupling):
 *       dθi/dt = ωi + K·r·sin(ψ − θi) + noise
 *   Lattice view (each cell coupled only to its 4 neighbours):
 *       dθi/dt = ωi + (K/4)·Σ sin(θj − θi) + noise
 *
 *   r·e^{iψ} = (1/N)·Σ e^{iθj} is the order parameter:
 *   r ≈ 0 means incoherent, r = 1 means perfectly phase-locked.
 *
 *   Natural frequencies ωi are drawn from a Lorentzian of half-width γ.
 *   For that case Kuramoto's exact result is a critical coupling
 *   Kc = 2γ (2(γ+D) with noise strength D), and for D = 0,
 *   r = sqrt(1 − Kc/K) above threshold. "Sweep K" checks this numerically.
 *
 * THINGS TO TRY
 *   Ring:    start with K below 1 and slowly drag it past Kc = 1. A locked
 *            spoke condenses out of the smear and the arrow grows.
 *            Add noise and watch the threshold move right.
 *   Lattice: spread γ ≈ 0, press Scramble. Random phases coarsen into
 *            domains joined by vortices (spiral defects) that drift and
 *            annihilate in pairs. Raise γ to watch frequency clusters
 *            fight for territory.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleSupplier;

public class KuramotoLab extends JFrame {

    static final double TWO_PI = 2 * Math.PI;
    static final double DT = 0.05;

    static final Color BG     = new Color(0x12, 0x14, 0x1a);
    static final Color PANEL  = new Color(0x1b, 0x1e, 0x26);
    static final Color GRID   = new Color(0x34, 0x39, 0x46);
    static final Color TEXT   = new Color(0xc9, 0xcf, 0xdb);
    static final Color ACCENT = new Color(0xff, 0xa6, 0x3d);

    // =====================================================================
    //  Models
    // =====================================================================

    interface Model {
        void step(double K, double gamma, double D, double dt);
        double order();
        void scramble();
    }

    static double wrap(double t) {
        if (t >= TWO_PI || t < 0) t -= TWO_PI * Math.floor(t / TWO_PI);
        return t;
    }

    /** All-to-all coupling. The mean-field trick makes each step O(N). */
    static final class RingModel implements Model {
        final int n;
        final double[] theta, unitOmega;   // unitOmega sorted ascending
        final Random rng;
        double r, psi;

        RingModel(int n, long seed) {
            this.n = n;
            rng = new Random(seed);
            theta = new double[n];
            unitOmega = new double[n];
            // Evenly spaced Lorentzian quantiles: far less finite-size noise
            // than random samples, so the population tracks the theory closely.
            for (int i = 0; i < n; i++) {
                double u = (i + 0.5) / n;
                unitOmega[i] = clamp(Math.tan(Math.PI * (u - 0.5)), 60);
            }
            scramble();
        }

        public void scramble() {
            for (int i = 0; i < n; i++) theta[i] = rng.nextDouble() * TWO_PI;
            measure();
        }

        void measure() {
            double c = 0, s = 0;
            for (double t : theta) { c += Math.cos(t); s += Math.sin(t); }
            c /= n; s /= n;
            r = Math.hypot(c, s);
            psi = Math.atan2(s, c);
        }

        public void step(double K, double gamma, double D, double dt) {
            double kr = K * r, amp = Math.sqrt(2 * D * dt);
            for (int i = 0; i < n; i++) {
                double t = theta[i] + dt * (gamma * unitOmega[i] + kr * Math.sin(psi - theta[i]));
                if (D > 0) t += amp * rng.nextGaussian();
                theta[i] = wrap(t);
            }
            measure();
        }

        public double order() { return r; }
    }

    /** L×L grid, periodic boundaries, nearest-neighbour coupling. */
    static final class LatticeModel implements Model {
        final int L;
        double[] theta, next;
        final double[] unitOmega;
        final Random rng;
        double r;

        LatticeModel(int L, long seed) {
            this.L = L;
            rng = new Random(seed);
            theta = new double[L * L];
            next = new double[L * L];
            unitOmega = new double[L * L];
            for (int i = 0; i < unitOmega.length; i++)
                unitOmega[i] = clamp(Math.tan(Math.PI * (rng.nextDouble() - 0.5)), 10);
            scramble();
        }

        public void scramble() {
            for (int i = 0; i < theta.length; i++) theta[i] = rng.nextDouble() * TWO_PI;
            measure();
        }

        void measure() {
            double c = 0, s = 0;
            for (double t : theta) { c += Math.cos(t); s += Math.sin(t); }
            r = Math.hypot(c, s) / theta.length;
        }

        public void step(double K, double gamma, double D, double dt) {
            double k4 = K * 0.25, amp = Math.sqrt(2 * D * dt);
            for (int y = 0; y < L; y++) {
                int row = y * L;
                int up = ((y + L - 1) % L) * L;
                int dn = ((y + 1) % L) * L;
                for (int x = 0; x < L; x++) {
                    int lf = x == 0 ? L - 1 : x - 1;
                    int rt = x == L - 1 ? 0 : x + 1;
                    int i = row + x;
                    double t = theta[i];
                    double s = Math.sin(theta[row + lf] - t) + Math.sin(theta[row + rt] - t)
                             + Math.sin(theta[up + x] - t) + Math.sin(theta[dn + x] - t);
                    double v = t + dt * (gamma * unitOmega[i] + k4 * s);
                    if (D > 0) v += amp * rng.nextGaussian();
                    next[i] = wrap(v);
                }
            }
            double[] tmp = theta; theta = next; next = tmp;
            measure();
        }

        public double order() { return r; }
    }

    static double clamp(double v, double lim) { return Math.max(-lim, Math.min(lim, v)); }

    // =====================================================================
    //  Views
    // =====================================================================

    static void smooth(Graphics2D g) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
    }

    /** Each oscillator is a dot at its phase angle; radius ranks its natural frequency. */
    static final class RingView extends JPanel {
        final RingModel m;
        final Color[] colors;

        RingView(RingModel m) {
            this.m = m;
            setBackground(BG);
            setPreferredSize(new Dimension(680, 680));
            colors = new Color[m.n];
            for (int i = 0; i < m.n; i++) {
                float f = i / (float) (m.n - 1);
                colors[i] = Color.getHSBColor(0.66f * (1 - f), 0.7f, 1f);
            }
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            smooth(g);
            int w = getWidth(), h = getHeight();
            double cx = w / 2.0, cy = h / 2.0, R = 0.44 * Math.min(w, h);

            g.setColor(GRID);
            g.setStroke(new BasicStroke(1f));
            for (double f : new double[]{0.35, 1.0})
                g.draw(new Ellipse2D.Double(cx - f * R, cy - f * R, 2 * f * R, 2 * f * R));
            g.draw(new Line2D.Double(cx - R, cy, cx + R, cy));
            g.draw(new Line2D.Double(cx, cy - R, cx, cy + R));

            double d = Math.max(2.5, R / 110);
            for (int i = 0; i < m.n; i++) {
                double rad = R * (0.35 + 0.65 * i / (double) (m.n - 1));
                double x = cx + rad * Math.cos(m.theta[i]);
                double y = cy - rad * Math.sin(m.theta[i]);
                g.setColor(colors[i]);
                g.fill(new Ellipse2D.Double(x - d / 2, y - d / 2, d, d));
            }

            // Order parameter r·e^{iψ}
            double ax = cx + m.r * R * Math.cos(m.psi), ay = cy - m.r * R * Math.sin(m.psi);
            g.setColor(Color.WHITE);
            g.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(new Line2D.Double(cx, cy, ax, ay));
            g.fill(new Ellipse2D.Double(ax - 5, ay - 5, 10, 10));

            g.setColor(TEXT);
            g.setFont(getFont().deriveFont(12f));
            g.drawString("angle = phase θ    inner = slow (blue), outer = fast (red)    arrow = order parameter",
                    12, h - 12);
        }
    }

    /** Each cell coloured by phase: hue goes once round the colour wheel per 2π. */
    static final class LatticeView extends JPanel {
        final LatticeModel m;
        final BufferedImage img;
        final int[] palette = new int[360];
        final int[] pixels;

        LatticeView(LatticeModel m) {
            this.m = m;
            setBackground(BG);
            setPreferredSize(new Dimension(680, 680));
            img = new BufferedImage(m.L, m.L, BufferedImage.TYPE_INT_RGB);
            pixels = new int[m.L * m.L];
            for (int i = 0; i < 360; i++) palette[i] = Color.HSBtoRGB(i / 360f, 0.8f, 0.95f);
        }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            for (int i = 0; i < pixels.length; i++) {
                int k = (int) (m.theta[i] / TWO_PI * 360);
                pixels[i] = palette[Math.min(359, Math.max(0, k))];
            }
            img.setRGB(0, 0, m.L, m.L, pixels, 0, m.L);
            int w = getWidth(), h = getHeight();
            int side = Math.min(w, h) - 48;
            int x0 = (w - side) / 2, y0 = (h - side) / 2 - 8;
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            g.drawImage(img, x0, y0, side, side, null);

            smooth(g);
            g.setColor(TEXT);
            g.setFont(getFont().deriveFont(12f));
            g.drawString("colour = phase θ    points where all colours meet are vortices (phase singularities)",
                    12, h - 12);
        }
    }

    /** Rolling plot of r over time. */
    static final class TimePlot extends JPanel {
        final double[] hist = new double[600];
        int pos, count;

        TimePlot() {
            setBackground(PANEL);
            setPreferredSize(new Dimension(380, 200));
        }

        void push(double v) { hist[pos] = v; pos = (pos + 1) % hist.length; count = Math.min(count + 1, hist.length); }
        void clear() { pos = 0; count = 0; }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            smooth(g);
            int w = getWidth(), h = getHeight();
            int L = 36, T = 24, Rm = 10, B = 22;
            int pw = w - L - Rm, ph = h - T - B;
            axes(g, "order parameter  r(t)", L, T, pw, ph, w);
            if (count < 2) return;
            Path2D.Double p = new Path2D.Double();
            for (int k = 0; k < count; k++) {
                int idx = (pos - count + k + hist.length) % hist.length;
                double x = L + pw * k / (double) (hist.length - 1);
                double y = T + ph * (1 - hist[idx]);
                if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
            }
            g.setColor(ACCENT);
            g.setStroke(new BasicStroke(1.8f));
            g.draw(p);
        }
    }

    static void axes(Graphics2D g, String title, int L, int T, int pw, int ph, int w) {
        g.setFont(g.getFont().deriveFont(11f));
        for (int k = 0; k <= 4; k++) {
            int y = T + ph * k / 4;
            g.setColor(GRID);
            g.drawLine(L, y, L + pw, y);
            g.setColor(TEXT);
            g.drawString(String.format("%.2f", 1 - k / 4.0), 4, y + 4);
        }
        g.setColor(GRID);
        g.drawRect(L, T, pw, ph);
        g.setColor(TEXT);
        g.setFont(g.getFont().deriveFont(Font.BOLD, 12f));
        g.drawString(title, L, T - 8);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 11f));
    }

    /** Steady-state r against K, with Kuramoto's exact prediction overlaid. */
    static final class SweepPlot extends JPanel {
        final List<double[]> pts = new ArrayList<>();
        final DoubleSupplier currentK;
        double kmax = 3, kc = 1, D = 0;
        boolean started;

        SweepPlot(DoubleSupplier currentK) {
            this.currentK = currentK;
            setBackground(PANEL);
            setPreferredSize(new Dimension(380, 240));
        }

        void begin(double kmax, double kc, double D) {
            this.kmax = kmax; this.kc = kc; this.D = D;
            pts.clear();
            started = true;
            repaint();
        }

        void add(List<double[]> more) { pts.addAll(more); repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            smooth(g);
            int w = getWidth(), h = getHeight();
            int L = 36, T = 24, Rm = 10, B = 26;
            int pw = w - L - Rm, ph = h - T - B;
            axes(g, "sweep: steady-state r vs K", L, T, pw, ph, w);
            if (!started) {
                g.setColor(TEXT);
                g.drawString("Press \"Sweep K\" to map the transition", L + 20, T + ph / 2);
                return;
            }
            for (int k = 0; k <= 4; k++) {
                double K = kmax * k / 4;
                g.drawString(String.format("%.1f", K), L + (int) (pw * k / 4.0) - 8, T + ph + 16);
            }

            // Critical coupling
            double xc = L + pw * kc / kmax;
            g.setColor(GRID.brighter());
            g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4, 4}, 0));
            g.draw(new Line2D.Double(xc, T, xc, T + ph));
            g.setColor(TEXT);
            g.drawString(String.format("Kc=%.2f", kc), (int) xc + 4, T + 14);

            // Exact theory (noise-free Lorentzian): r = sqrt(1 - Kc/K)
            if (D == 0) {
                Path2D.Double p = new Path2D.Double();
                for (int k = 0; k <= 300; k++) {
                    double K = kmax * k / 300.0;
                    double r = K > kc ? Math.sqrt(1 - kc / K) : 0;
                    double x = L + pw * K / kmax, y = T + ph * (1 - r);
                    if (k == 0) p.moveTo(x, y); else p.lineTo(x, y);
                }
                g.setColor(new Color(0x6c, 0xa8, 0xff));
                g.setStroke(new BasicStroke(1.6f));
                g.draw(p);
                g.drawString("theory  √(1−Kc/K)", L + pw - 120, T + ph - 8);
            }

            g.setColor(Color.WHITE);
            for (double[] q : pts) {
                double x = L + pw * q[0] / kmax, y = T + ph * (1 - q[1]);
                g.fill(new Ellipse2D.Double(x - 3, y - 3, 6, 6));
            }

            double K = currentK.getAsDouble();
            if (K <= kmax) {
                double x = L + pw * K / kmax;
                g.setColor(ACCENT);
                g.setStroke(new BasicStroke(1.5f));
                g.draw(new Line2D.Double(x, T, x, T + ph));
            }
        }
    }

    /** A slider with a live numeric readout. */
    static final class Knob extends JPanel {
        final JSlider s;
        final double scale;

        Knob(String name, int min, int max, int val, double scale, String fmt) {
            super(new BorderLayout(6, 0));
            this.scale = scale;
            setOpaque(false);
            JLabel title = new JLabel(name);
            title.setForeground(TEXT);
            title.setPreferredSize(new Dimension(110, 20));
            JLabel readout = new JLabel();
            readout.setForeground(ACCENT);
            readout.setPreferredSize(new Dimension(44, 20));
            s = new JSlider(min, max, val);
            s.setOpaque(false);
            Runnable upd = () -> readout.setText(String.format(fmt, value()));
            s.addChangeListener(e -> upd.run());
            upd.run();
            add(title, BorderLayout.WEST);
            add(s, BorderLayout.CENTER);
            add(readout, BorderLayout.EAST);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        }

        double value() { return s.getValue() / scale; }
        void set(double v) { s.setValue((int) Math.round(v * scale)); }
    }

    // =====================================================================
    //  Application
    // =====================================================================

    final RingModel ring = new RingModel(1200, 1);
    final LatticeModel lattice = new LatticeModel(160, 2);
    final RingView ringView = new RingView(ring);
    final LatticeView latticeView = new LatticeView(lattice);
    final CardLayout cards = new CardLayout();
    final JPanel viewHolder = new JPanel(cards);

    final Knob kKnob     = new Knob("Coupling K",       0, 400, 50, 100, "%.2f");
    final Knob gammaKnob = new Knob("Spread γ",         0, 200, 50, 100, "%.2f");
    final Knob noiseKnob = new Knob("Noise D",          0, 100,  0, 100, "%.2f");
    final Knob speedKnob = new Knob("Steps / frame",    1,  20,  4,   1, "%.0f");

    final TimePlot timePlot = new TimePlot();
    final SweepPlot sweepPlot = new SweepPlot(kKnob::value);
    final JLabel status = new JLabel(" ");
    final JButton sweepBtn = new JButton("Sweep K");
    final JButton pauseBtn = new JButton("Pause");

    boolean latticeMode, paused;

    KuramotoLab() {
        super("Kuramoto Lab - emergent synchrony");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        viewHolder.add(ringView, "ring");
        viewHolder.add(latticeView, "lattice");
        add(viewHolder, BorderLayout.CENTER);

        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBackground(BG);
        side.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        side.setPreferredSize(new Dimension(400, 680));

        JComboBox<String> mode = new JComboBox<>(new String[]{
                "Ring - all-to-all coupling", "Lattice - nearest neighbours"});
        mode.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        mode.addActionListener(e -> setMode(mode.getSelectedIndex() == 1));

        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 0));
        buttons.setOpaque(false);
        buttons.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        JButton scramble = new JButton("Scramble");
        scramble.addActionListener(e -> { current().scramble(); timePlot.clear(); });
        pauseBtn.addActionListener(e -> { paused = !paused; pauseBtn.setText(paused ? "Run" : "Pause"); });
        sweepBtn.addActionListener(e -> runSweep());
        buttons.add(scramble);
        buttons.add(pauseBtn);
        buttons.add(sweepBtn);

        status.setForeground(TEXT);
        status.setAlignmentX(LEFT_ALIGNMENT);
        status.setFont(status.getFont().deriveFont(13f));

        for (JComponent c : new JComponent[]{mode, buttons, kKnob, gammaKnob, noiseKnob, speedKnob,
                status, timePlot, sweepPlot})
            c.setAlignmentX(LEFT_ALIGNMENT);

        side.add(mode);
        side.add(Box.createVerticalStrut(8));
        side.add(buttons);
        side.add(Box.createVerticalStrut(10));
        side.add(kKnob);
        side.add(gammaKnob);
        side.add(noiseKnob);
        side.add(speedKnob);
        side.add(Box.createVerticalStrut(8));
        side.add(status);
        side.add(Box.createVerticalStrut(8));
        side.add(timePlot);
        side.add(Box.createVerticalStrut(8));
        side.add(sweepPlot);
        add(side, BorderLayout.EAST);

        new Timer(16, e -> tick()).start();
        pack();
        setLocationRelativeTo(null);
    }

    Model current() { return latticeMode ? lattice : ring; }

    void setMode(boolean lat) {
        latticeMode = lat;
        cards.show(viewHolder, lat ? "lattice" : "ring");
        sweepBtn.setEnabled(!lat);
        if (lat) { kKnob.set(2.0); gammaKnob.set(0.02); noiseKnob.set(0); speedKnob.set(6); }
        else     { kKnob.set(0.5); gammaKnob.set(0.5);  noiseKnob.set(0); speedKnob.set(4); }
        current().scramble();
        timePlot.clear();
    }

    void tick() {
        double K = kKnob.value(), g = gammaKnob.value(), D = noiseKnob.value();
        if (!paused) {
            Model m = current();
            int steps = (int) speedKnob.value();
            for (int s = 0; s < steps; s++) m.step(K, g, D, DT);
            timePlot.push(m.order());
        }
        double r = current().order();
        if (latticeMode) {
            status.setText(String.format("r = %.3f    local coupling: watch domains and vortices", r));
        } else {
            double kc = 2 * (g + D);
            String phase = K < kc ? "incoherent" : "partially locked";
            status.setText(String.format("r = %.3f    Kc = 2(γ+D) = %.2f    K/Kc = %s  (%s)",
                    r, kc, kc > 0 ? String.format("%.2f", K / kc) : "∞", phase));
        }
        viewHolder.repaint();
        timePlot.repaint();
        sweepPlot.repaint();
    }

    void runSweep() {
        final double gamma = gammaKnob.value(), D = noiseKnob.value();
        final double kc = 2 * (gamma + D);
        final double kmax = Math.max(1.0, 2.5 * kc);
        sweepPlot.begin(kmax, kc, D);
        sweepBtn.setEnabled(false);
        new SwingWorker<Void, double[]>() {
            @Override protected Void doInBackground() {
                RingModel m = new RingModel(2000, 7);
                int n = 40;
                for (int j = 0; j <= n; j++) {
                    double K = kmax * j / n;   // adiabatic: each K starts from the last state
                    for (int s = 0; s < 1000; s++) m.step(K, gamma, D, DT);
                    double acc = 0;
                    for (int s = 0; s < 500; s++) { m.step(K, gamma, D, DT); acc += m.r; }
                    publish(new double[]{K, acc / 500});
                }
                return null;
            }
            @Override protected void process(List<double[]> chunk) { sweepPlot.add(chunk); }
            @Override protected void done() { sweepBtn.setEnabled(!latticeMode); }
        }.execute();
    }

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "Kuramoto Lab");
        SwingUtilities.invokeLater(() -> {
            try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
            catch (Exception ignored) { }
            new KuramotoLab().setVisible(true);
        });
    }
}
