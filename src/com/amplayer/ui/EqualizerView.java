package com.amplayer.ui;

import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.Settings;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Equalizer screen for JSR-234 (AMMS).
 * Allows toggling EQ and selecting presets.
 */
public class EqualizerView extends Canvas implements CommandListener {

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

    private static final int PAD          = 8;
    private static final int ACCENT_W     = 3;
    private static final int TITLE_BAR_H  = TITLE_FONT.getHeight() + PAD * 2;
    private static final int ITEM_H        = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;

    private final AppleMusicMIDlet midlet;
    private final Display          display;
    private final Displayable      backScreen;
    private final PlaybackManager  pm;

    private int selectedIndex = 0;
    private int scrollOffset  = 0;
    
    private String[] presets    = null;
    private int      bandCount  = 0;

    public EqualizerView(AppleMusicMIDlet midlet, Display display, Displayable backScreen, PlaybackManager pm) {
        super();
        this.midlet     = midlet;
        this.display    = display;
        this.backScreen = backScreen;
        this.pm         = pm;
        setFullScreenMode(true);
        fetchPresets();
    }

    private void fetchPresets() {
        if (pm == null) return;
        presets = pm.getEqualizerPresets();
        bandCount = pm.getEqualizerNumberOfBands();
        if (presets != null && presets.length == 0) presets = null;
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
        g.drawString("Equalizer", PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, TITLE_BAR_H - 2, w, 2);

        int skH   = SUB_FONT.getHeight() + PAD * 2;
        int listH = h - TITLE_BAR_H - skH;

        if (bandCount == 0) {
            g.setFont(NAME_FONT);
            g.setColor(COLOR_SUBNAME);
            g.drawString("No player active or", w / 2, h / 2 - 10, Graphics.HCENTER | Graphics.TOP);
            g.drawString("EQ not supported", w / 2, h / 2 + 10, Graphics.HCENTER | Graphics.TOP);
        } else {
            int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
            g.setClip(0, TITLE_BAR_H, w, listH);

            int totalRows = 2 + (presets != null ? presets.length : 0);
            int visible   = listH / ITEM_H + 1;
            int end       = Math.min(totalRows, scrollOffset + visible);

            for (int i = scrollOffset; i < end; i++) {
                int y = TITLE_BAR_H + (i - scrollOffset) * ITEM_H;
                boolean sel = (i == selectedIndex);

                if (sel) {
                    g.setColor(COLOR_SELECTED);
                    g.fillRect(0, y, w, ITEM_H);
                    g.setColor(COLOR_ACCENT);
                    g.fillRect(0, y, ACCENT_W, ITEM_H);
                }

                int textX = PAD + ACCENT_W + 4;
                if (i == 0) {
                    // Row 0: Toggle
                    g.setFont(NAME_FONT);
                    g.setColor(COLOR_NAME);
                    g.drawString("Equalizer", textX, y + PAD, Graphics.LEFT | Graphics.TOP);
                    g.setFont(SUB_FONT);
                    g.setColor(COLOR_SUBNAME);
                    g.drawString(Settings.eqEnabled ? "On" : "Off", textX, y + PAD + NAME_FONT.getHeight(), Graphics.LEFT | Graphics.TOP);
                } else if (i == 1) {
                    // Row 1: Manual EQ
                    g.setFont(NAME_FONT);
                    g.setColor(COLOR_NAME);
                    g.drawString("Manual EQ", textX, y + PAD, Graphics.LEFT | Graphics.TOP);
                    g.setFont(SUB_FONT);
                    g.setColor(COLOR_SUBNAME);
                    g.drawString("Adjust bands", textX, y + PAD + NAME_FONT.getHeight(), Graphics.LEFT | Graphics.TOP);
                } else {
                    // Preset rows
                    int pIdx = i - 2;
                    g.setFont(NAME_FONT);
                    g.setColor(COLOR_NAME);
                    g.drawString(presets[pIdx], textX, y + PAD, Graphics.LEFT | Graphics.TOP);
                    
                    if (Settings.eqEnabled && Settings.eqPreset == pIdx) {
                        g.setFont(SUB_FONT);
                        g.setColor(COLOR_ACCENT);
                        g.drawString("Selected", textX, y + PAD + NAME_FONT.getHeight(), Graphics.LEFT | Graphics.TOP);
                    }
                }

                g.setColor(COLOR_DIVIDER);
                g.drawLine(PAD, y + ITEM_H - 1, w - PAD, y + ITEM_H - 1);
            }

            g.setClip(cx, cy, cw, ch);
            
            // Scrollbar
            if (totalRows * ITEM_H > listH) {
                int barH = Math.max(8, listH * listH / (totalRows * ITEM_H));
                int barY = TITLE_BAR_H + (listH - barH) * scrollOffset / Math.max(1, totalRows - (listH / ITEM_H));
                g.setColor(COLOR_DIVIDER);
                g.fillRect(w - 3, TITLE_BAR_H, 3, listH);
                g.setColor(COLOR_ACCENT);
                g.fillRect(w - 3, barY, 3, barH);
            }
        }

        drawSoftKeyBar(g, w, h, skH);
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
        g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_SUBNAME);
        g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
    }

    protected void keyPressed(int keyCode) {
        if (keyCode == -6 || keyCode == -5) {
            activate();
            return;
        }
        if (keyCode == -7) {
            display.setCurrent(backScreen);
            return;
        }
        int action = getGameAction(keyCode);
        int totalRows = 2 + (presets != null ? presets.length : 0);
        
        if (action == FIRE) {
            activate();
        } else if (action == UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                ensureVisible();
                repaint();
            }
        } else if (action == DOWN) {
            if (selectedIndex < totalRows - 1) {
                selectedIndex++;
                ensureVisible();
                repaint();
            }
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    // -------------------------------------------------------------------------
    // Touch 
    // -------------------------------------------------------------------------

    private long pressTime_T = 0;

    protected void pointerPressed(int x, int y) {
        pressTime_T = System.currentTimeMillis();
    }

    protected void pointerReleased(int x, int y) {
        if ((System.currentTimeMillis() - pressTime_T) < 500) {
            int w = getWidth();
            int h = getHeight();
            int skH = SUB_FONT.getHeight() + PAD * 2;
            
            // Softkeys
            if (y > h - skH) {
                if (x > w / 2) {
                    display.setCurrent(backScreen);
                } else {
                    activate();
                }
                return;
            }

            // List area
            if (y >= TITLE_BAR_H && y < h - skH) {
                int absY = y - TITLE_BAR_H;
                int clickedIdx = scrollOffset + (absY / ITEM_H);
                int totalRows = 2 + (presets != null ? presets.length : 0);
                if (clickedIdx >= 0 && clickedIdx < totalRows) {
                    selectedIndex = clickedIdx;
                    activate();
                    repaint();
                }
            }
        }
    }

    private void ensureVisible() {
        int listH = getHeight() - TITLE_BAR_H - (SUB_FONT.getHeight() + PAD * 2);
        int vis   = listH / ITEM_H;
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        else if (selectedIndex >= scrollOffset + vis) scrollOffset = selectedIndex - vis + 1;
    }

    private void activate() {
        if (selectedIndex == 0) {
            Settings.eqEnabled = !Settings.eqEnabled;
            Settings.save();
        } else if (selectedIndex == 1) {
            midlet.showCustomEQEdit(this);
        } else {
            Settings.eqPreset = selectedIndex - 2;
            Settings.eqEnabled = true; // auto-enable when picking preset
            Settings.save();
        }
        pm.applyEqualizer();
        repaint();
    }

    public void commandAction(Command c, Displayable d) {
        // Not used as we handle keys directly for premium feel
    }
    
    protected void showNotify() {
        fetchPresets();
        repaint();
    }

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
