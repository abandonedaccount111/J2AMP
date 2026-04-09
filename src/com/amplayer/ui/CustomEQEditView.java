package com.amplayer.ui;

import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.Settings;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Visual Equalizer Editor for JSR-234.
 * Displays interactive bars for each frequency band.
 */
public class CustomEQEditView extends Canvas implements CommandListener {

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_SELECTED = 0x1C1C1E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_NAME     = 0xFFFFFF;
    private static final int COLOR_SUBNAME  = 0x8E8E93;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;
    private static final int COLOR_HDR_BG   = 0x111111;

    private static final Font TITLE_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font NAME_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUB_FONT   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD = 8;
    private static final int TITLE_BAR_H = TITLE_FONT.getHeight() + PAD * 2;

    private final AppleMusicMIDlet midlet;
    private final Display          display;
    private final Displayable      backScreen;
    private final PlaybackManager  pm;

    private int bandCount = 0;
    private int minLevel  = -1500;
    private int maxLevel  = 1500;
    private int[] frequencies;
    private int focusedBand = 0;

    public CustomEQEditView(AppleMusicMIDlet midlet, Display display, Displayable backScreen, PlaybackManager pm) {
        super();
        this.midlet     = midlet;
        this.display    = display;
        this.backScreen = backScreen;
        this.pm         = pm;
        setFullScreenMode(true);
        initBands();
    }

    private void initBands() {
        bandCount = pm.getEqualizerNumberOfBands();
        if (bandCount <= 0) {
            // Fallback for UI if no player active
            bandCount = 5;
        } else {
            minLevel = pm.getEqualizerMinLevel();
            maxLevel = pm.getEqualizerMaxLevel();
            frequencies = new int[bandCount];
            for (int i = 0; i < bandCount; i++) {
                frequencies[i] = pm.getEqualizerCenterFreq(i);
            }
        }

        if (Settings.eqCustomLevels == null || Settings.eqCustomLevels.length != bandCount) {
            Settings.eqCustomLevels = new int[bandCount];
            // If we have a player, try to fetch current levels
            for (int i = 0; i < bandCount; i++) {
                Settings.eqCustomLevels[i] = pm.getEqualizerBandLevel(i);
            }
        }
    }

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Title bar
        g.setColor(COLOR_HDR_BG);
        g.fillRect(0, 0, w, TITLE_BAR_H);
        g.setFont(TITLE_FONT);
        g.setColor(COLOR_NAME);
        g.drawString("Manual EQ", PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, TITLE_BAR_H - 2, w, 2);

        int skH      = SUB_FONT.getHeight() + PAD * 2;
        int areaY    = TITLE_BAR_H + PAD;
        int areaH    = h - TITLE_BAR_H - skH - PAD * 4 - NAME_FONT.getHeight() - SUB_FONT.getHeight();
        int areaW    = w - PAD * 2;
        int barSpace = areaW / Math.max(1, bandCount);
        int barW     = Math.max(4, barSpace - 4);

        // Zero line
        int zeroY = areaY + areaH / 2;
        g.setColor(COLOR_DIVIDER);
        g.drawLine(PAD, zeroY, w - PAD, zeroY);

        for (int i = 0; i < bandCount; i++) {
            int bx = PAD + i * barSpace + (barSpace - barW) / 2;
            int level = Settings.eqCustomLevels[i];
            
            // Map level to Y
            // Level 0 is at zeroY. maxLevel is at areaY. minLevel is at areaY+areaH.
            int bh = (int)((long)level * (areaH / 2) / (maxLevel > 0 ? maxLevel : 1500));
            int by = zeroY - bh;

            // Highlight focused band
            if (i == focusedBand) {
                g.setColor(0x333333);
                g.fillRect(bx - 2, areaY, barW + 4, areaH);
                g.setColor(COLOR_ACCENT);
            } else {
                g.setColor(COLOR_SUBNAME);
            }

            // Draw bar
            if (bh >= 0) {
                g.fillRect(bx, by, barW, bh);
            } else {
                g.fillRect(bx, zeroY, barW, -bh);
            }
            
            // Draw knob/handle
            g.setColor(i == focusedBand ? COLOR_NAME : COLOR_ACCENT);
            g.fillRect(bx - 1, by - 2, barW + 2, 4);
        }

        // Band Details
        int detailY = areaY + areaH + PAD;
        g.setFont(NAME_FONT);
        g.setColor(COLOR_NAME);
        String freqStr = formatFreq(frequencies != null ? frequencies[focusedBand] : 0);
        g.drawString(freqStr, w / 2, detailY, Graphics.HCENTER | Graphics.TOP);
        
        g.setFont(SUB_FONT);
        g.setColor(COLOR_ACCENT);
        String gainStr = (Settings.eqCustomLevels[focusedBand] / 100) + " dB";
        g.drawString(gainStr, w / 2, detailY + NAME_FONT.getHeight(), Graphics.HCENTER | Graphics.TOP);

        drawSoftKeyBar(g, w, h, skH);
    }

    private String formatFreq(int mHz) {
        if (mHz <= 0) return "Band " + (focusedBand + 1);
        int hz = mHz / 1000;
        if (hz >= 1000) return (hz / 1000) + " kHz";
        return hz + " Hz";
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HDR_BG);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUB_FONT);
        int labelY = barY + (skH - SUB_FONT.getHeight()) / 2;
        g.setColor(COLOR_NAME);
        g.drawString("Save", PAD, labelY, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_SUBNAME);
        g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
    }

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (keyCode == -6 || action == FIRE || keyCode == -5) {
            saveAndExit();
        } else if (keyCode == -7) {
            display.setCurrent(backScreen);
        } else if (action == LEFT) {
            if (focusedBand > 0) { focusedBand--; repaint(); }
        } else if (action == RIGHT) {
            if (focusedBand < bandCount - 1) { focusedBand++; repaint(); }
        } else if (action == UP) {
            adjustLevel(100);
        } else if (action == DOWN) {
            adjustLevel(-100);
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    private void adjustLevel(int delta) {
        int val = Settings.eqCustomLevels[focusedBand] + delta;
        if (val < minLevel) val = minLevel;
        if (val > maxLevel) val = maxLevel;
        Settings.eqCustomLevels[focusedBand] = val;
        
        // Immediate apply if music is playing
        pm.setEqualizerBandLevel(focusedBand, val);
        pm.applyEqualizer();
        repaint();
    }

    private void saveAndExit() {
        Settings.eqPreset = -1; // Use custom
        Settings.eqEnabled = true;
        Settings.save();
        pm.applyEqualizer();
        display.setCurrent(backScreen);
    }

    // ── Touch ──────────────────────────────────────────────────────────────
    private long pressTime_T = 0;

    protected void pointerPressed(int x, int y) {
        pressTime_T = System.currentTimeMillis();
        handleTouch(x, y);
    }

    protected void pointerDragged(int x, int y) {
        handleTouch(x, y);
    }

    private void handleTouch(int x, int y) {
        int w = getWidth();
        int h = getHeight();
        int skH = SUB_FONT.getHeight() + PAD * 2;
        
        if (y > h - skH) return; // softkeys handled in released

        int areaY = TITLE_BAR_H + PAD;
        int areaH = h - TITLE_BAR_H - skH - PAD * 4 - NAME_FONT.getHeight() - SUB_FONT.getHeight();
        int areaW = w - PAD * 2;
        int barSpace = areaW / Math.max(1, bandCount);

        if (y >= areaY && y <= areaY + areaH) {
            int band = (x - PAD) / barSpace;
            if (band >= 0 && band < bandCount) {
                focusedBand = band;
                int zeroY = areaY + areaH / 2;
                int bh = zeroY - y;
                int level = (int)((long)bh * (maxLevel > 0 ? maxLevel : 1500) / (areaH / 2));
                if (level < minLevel) level = minLevel;
                if (level > maxLevel) level = maxLevel;
                Settings.eqCustomLevels[focusedBand] = level;
                pm.setEqualizerBandLevel(focusedBand, level);
                pm.applyEqualizer();
                repaint();
            }
        }
    }

    protected void pointerReleased(int x, int y) {
        if ((System.currentTimeMillis() - pressTime_T) < 500) {
            int w = getWidth();
            int h = getHeight();
            int skH = SUB_FONT.getHeight() + PAD * 2;
            if (y > h - skH) {
                if (x > w / 2) display.setCurrent(backScreen);
                else saveAndExit();
            }
        }
    }

    public void commandAction(Command c, Displayable d) {}
}
