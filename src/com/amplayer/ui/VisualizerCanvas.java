package com.amplayer.ui;

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Full-screen animated spectrum visualizer.
 *
 * Renders 20 fake animated bars using integer fixed-point math (height * 1000).
 * Each bar has an independent "energy" value that rises with random impulses and
 * decays each frame.  Peak indicators hang above each bar and drop slowly.
 *
 * The animation runs at ~50 ms per frame (≈20 fps) while the Canvas is visible,
 * driven by showNotify / hideNotify.
 */
public class VisualizerCanvas extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_HDR      = 0x111111;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_TEXT1    = 0xFFFFFF;
    private static final int COLOR_TEXT2    = 0x8E8E93;
    private static final int COLOR_BAR_LO   = 0x30D158; // green  — low energy
    private static final int COLOR_BAR_MID  = 0xFFD60A; // yellow — mid energy
    private static final int COLOR_BAR_HI   = 0xFA2D48; // red    — high energy
    private static final int COLOR_PEAK     = 0xFFFFFF;

    // -------------------------------------------------------------------------
    // Layout
    // -------------------------------------------------------------------------

    private static final int PAD   = 6;
    private static final int BARS  = 20;
    private static final Font HDR_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUB_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    // -------------------------------------------------------------------------
    // Bar state — integer fixed-point (* 1000)
    // -------------------------------------------------------------------------

    // Current bar height fraction in [0, 1000]
    private final int[] barHeight  = new int[BARS];
    // Peak height fraction in [0, 1000] — drops slowly
    private final int[] peakHeight = new int[BARS];
    // Per-bar phase for pseudo-random pulsing
    private final int[] phase      = new int[BARS];
    // Impulse counter (random extra burst for each bar)
    private final int[] impulse    = new int[BARS];

    // Simple LCG pseudo-random state
    private int rng = 0x1234ABCD;

    // -------------------------------------------------------------------------
    // Animation
    // -------------------------------------------------------------------------

    private static final int FRAME_MS = 50;
    private volatile boolean running  = false;

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK = new Command("Back", Command.BACK, 1);

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final Display    display;
    private final Displayable backScreen;
    private String trackName   = "";
    private String trackArtist = "";

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public VisualizerCanvas(Display display, Displayable backScreen) {
        this.display    = display;
        this.backScreen = backScreen;
        setFullScreenMode(true);
        // Stagger phases so bars don't all pulse together
        for (int i = 0; i < BARS; i++) {
            phase[i] = i * (1000 / BARS);
        }
    }

    // -------------------------------------------------------------------------
    // Track info (optional — shown at top)
    // -------------------------------------------------------------------------

    public void setTrackInfo(String name, String artist) {
        trackName   = name   != null ? name   : "";
        trackArtist = artist != null ? artist : "";
        repaint();
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    protected void showNotify() { startAnimation(); }
    protected void hideNotify() { stopAnimation();  }

    private void startAnimation() {
        if (running) return;
        running = true;
        new Thread(new Runnable() {
            public void run() {
                while (running) {
                    try { Thread.sleep(FRAME_MS); } catch (InterruptedException e) { break; }
                    tick();
                    repaint();
                }
            }
        }).start();
    }

    private void stopAnimation() {
        running = false;
    }

    // -------------------------------------------------------------------------
    // Animation tick
    // -------------------------------------------------------------------------

    private void tick() {
        for (int i = 0; i < BARS; i++) {
            // Advance phase
            phase[i] = (phase[i] + 15 + (i % 3) * 5) % 1000;

            // Occasionally fire an impulse
            if ((nextRng() & 0x1F) == 0) {
                impulse[i] = 300 + (nextRng() & 0xFF);
            }
            if (impulse[i] > 0) {
                barHeight[i] += impulse[i] / 4;
                impulse[i]   -= 40;
                if (impulse[i] < 0) impulse[i] = 0;
            }

            // Sine-like wave contribution (using phase approximation)
            int wave = sineApprox(phase[i]);   // 0..500
            barHeight[i] += wave / 8;

            // Decay
            barHeight[i] -= 35;

            // Clamp
            if (barHeight[i] < 0)    barHeight[i] = 0;
            if (barHeight[i] > 1000) barHeight[i] = 1000;

            // Peak
            if (barHeight[i] > peakHeight[i]) {
                peakHeight[i] = barHeight[i];
            } else {
                peakHeight[i] -= 8;
                if (peakHeight[i] < barHeight[i]) peakHeight[i] = barHeight[i];
                if (peakHeight[i] < 0)            peakHeight[i] = 0;
            }
        }
    }

    /** Approximate sin(phase/1000 * 2*PI) scaled to [0, 500]. */
    private static int sineApprox(int phase) {
        // phase in [0, 1000) → maps to [0, 1000) = full cycle
        // Use a triangle-wave approximation: rises 0→500 for phase 0→250,
        // falls 500→0 for phase 250→500, stays 0 for phase 500→1000.
        if (phase < 250)       return phase * 2;
        else if (phase < 500)  return (500 - phase) * 2;
        else                   return 0;
    }

    // -------------------------------------------------------------------------
    // LCG random
    // -------------------------------------------------------------------------

    private int nextRng() {
        rng = rng * 1664525 + 1013904223;
        return rng & 0x7FFFFFFF;
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Header
        int hdrH = HDR_FONT.getHeight() + PAD * 2;
        if (trackName.length() > 0 || trackArtist.length() > 0) {
            g.setColor(COLOR_HDR);
            g.fillRect(0, 0, w, hdrH);
            g.setColor(COLOR_DIVIDER);
            g.drawLine(0, hdrH, w, hdrH);

            g.setFont(HDR_FONT);
            g.setColor(COLOR_TEXT1);
            g.drawString(clip(trackName, HDR_FONT, w - PAD * 2),
                         w / 2, PAD, Graphics.HCENTER | Graphics.TOP);
            g.setFont(SUB_FONT);
            g.setColor(COLOR_TEXT2);
            g.drawString(clip(trackArtist, SUB_FONT, w - PAD * 2),
                         w / 2, PAD + HDR_FONT.getHeight(),
                         Graphics.HCENTER | Graphics.TOP);
        } else {
            hdrH = 0;
        }

        int skH = SUB_FONT.getHeight() + PAD * 2;

        // Bar area
        int areaH = h - hdrH - skH - PAD;
        int totalBarW = w - PAD * 2;
        int barW  = totalBarW / BARS;
        if (barW < 2) barW = 2;
        int gap   = 1;

        for (int i = 0; i < BARS; i++) {
            int bx = PAD + i * barW;
            int bh = barHeight[i] * areaH / 1000;
            int by = hdrH + areaH - bh;

            // Bar color based on height fraction
            if (barHeight[i] > 750)       g.setColor(COLOR_BAR_HI);
            else if (barHeight[i] > 400)  g.setColor(COLOR_BAR_MID);
            else                          g.setColor(COLOR_BAR_LO);

            if (bh > 0) {
                g.fillRect(bx, by, barW - gap, bh);
            }

            // Peak indicator (2px line)
            int ph = peakHeight[i] * areaH / 1000;
            int py = hdrH + areaH - ph - 2;
            if (ph > 0 && py >= hdrH) {
                g.setColor(COLOR_PEAK);
                g.fillRect(bx, py, barW - gap, 2);
            }
        }

        // Subtle accent line at bottom
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, h - skH - 2, w, 2);

        drawSoftKeyBar(g, w, h, skH);
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HDR);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUB_FONT);
        int labelY = barY + (skH - SUB_FONT.getHeight()) / 2;
        g.setColor(COLOR_TEXT1);
        g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_TEXT2);
        g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
    }

    // -------------------------------------------------------------------------
    // Key handling
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        if (keyCode == -6 || keyCode == -7) {
            goBack();
            return;
        }
        int action = getGameAction(keyCode);
        if (action == FIRE) {
            goBack();
        }
    }

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) goBack();
    }

    private void goBack() {
        stopAnimation();
        display.setCurrent(backScreen);
    }

    // -------------------------------------------------------------------------
    // Touch Events
    // -------------------------------------------------------------------------

    private long pressTime_T = 0;

    protected void pointerPressed(int x, int y) {
        pressTime_T = System.currentTimeMillis();
    }

    protected void pointerReleased(int x, int y) {
        if ((System.currentTimeMillis() - pressTime_T) < 500) {
            int h = getHeight();
            int w = getWidth();
            int skH = SUB_FONT.getHeight() + PAD * 2;
            if (y > h - skH) {
                goBack();
            } else {
                goBack(); // Any click exits visualizer
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
