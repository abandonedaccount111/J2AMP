package com.amplayer.ui;

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
 * Scrollable queue browser.
 *
 * Shows every track in the current playback queue.  The currently playing
 * track is highlighted with an accent left-bar and bold text.  Pressing FIRE
 * on any row calls PlaybackManager.play(index) to jump there immediately.
 */
public class QueueView extends Canvas
        implements CommandListener, PlaybackManager.Listener {

    // -------------------------------------------------------------------------
    // Colors / fonts
    // -------------------------------------------------------------------------

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_HEADER   = 0x111111;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_TEXT1    = 0xFFFFFF;
    private static final int COLOR_TEXT2    = 0x8E8E93;
    private static final int COLOR_SEL_BG   = 0x1C1C1E;

    private static final Font HDR_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font NAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUB_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font NUM_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD = 8;

    // -------------------------------------------------------------------------
    // Commands  (used only on non-Nokia devices)
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK     = new Command("Back",     Command.BACK, 1);
    private static final Command CMD_PLAY     = new Command("Play",     Command.OK,   1);
    private static final Command CMD_AUTOPLAY = new Command("Autoplay", Command.ITEM, 2);

    private String[] nokiaMenuItems;
    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final PlaybackManager          pm;
    private final Display                  display;
    private final Displayable              backScreen;
    private final PlaybackManager.Listener prevListener;

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    private int currentIndex;
    private int selectedIndex;
    private int scrollOffset;

    // ── Marquee (selected row only) ──────────────────────────────────────────
    private static final int MQ_SPEED = 2;
    private static final int MQ_PAUSE = 20;
    private int  mqOffset  = 0;
    private int  mqPause   = MQ_PAUSE;
    private int  mqMaxOvf  = 0;
    private volatile boolean mqRunning = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public QueueView(PlaybackManager pm, Display display, Displayable backScreen) {
        this.pm         = pm;
        this.display    = display;
        this.backScreen = backScreen;

        prevListener  = pm.getListener();
        pm.setListener(this);

        currentIndex  = pm.getCurrentIndex();
        selectedIndex = currentIndex >= 0 ? currentIndex : 0;
        scrollOffset  = 0;
        ensureVisible(selectedIndex);

        updateNokiaMenuItems();
        setFullScreenMode(true);
    }

    // -------------------------------------------------------------------------
    // PlaybackManager.Listener — chain to previous listener
    // -------------------------------------------------------------------------

    public void onTrackChanged(int index) {
        if (prevListener != null) prevListener.onTrackChanged(index);
        currentIndex  = pm.getCurrentIndex();
        selectedIndex = currentIndex >= 0 ? currentIndex : selectedIndex;
        mqReset();
        ensureVisible(selectedIndex);
        repaint();
    }

    public void onPlayStateChanged(boolean playing) {
        if (prevListener != null) prevListener.onPlayStateChanged(playing);
        repaint();
    }

    public void onError(String msg) {
        if (prevListener != null) prevListener.onError(msg);
    }

    // -------------------------------------------------------------------------
    // Key input
    // -------------------------------------------------------------------------

    protected void showNotify() { mqStart(); }
    protected void hideNotify() { mqStop();  }

    private void mqStart() {
        if (!Settings.marqueeEnabled) return;
        if (mqRunning) return;
        mqRunning = true;
        new Thread(new Runnable() {
            public void run() {
                while (mqRunning) {
                    try { Thread.sleep(80); } catch (InterruptedException e) { break; }
                    mqTick(); repaint();
                }
            }
        }).start();
    }
    private void mqStop()  { mqRunning = false; }
    private void mqReset() { mqOffset = 0; mqPause = MQ_PAUSE; mqMaxOvf = 0; }
    private void mqTick() {
        if (mqMaxOvf <= 0) { mqOffset = 0; return; }
        if (mqPause > 0)   { mqPause--;    return; }
        mqOffset += MQ_SPEED;
        if (mqOffset >= mqMaxOvf) { mqOffset = 0; mqPause = MQ_PAUSE; }
    }

    // -------------------------------------------------------------------------
    // Touch Events
    // -------------------------------------------------------------------------

    private int startY_T = -1;
    private int startOffset_T = 0;
    private boolean isDragging_T = false;
    private long pressTime_T = 0;

    protected void pointerPressed(int x, int y) {
        startY_T = y;
        startOffset_T = scrollOffset;
        isDragging_T = false;
        pressTime_T = System.currentTimeMillis();
    }

    protected void pointerDragged(int x, int y) {
        if (nokiaMenuOpen) return;
        if (Math.abs(y - startY_T) > 5) {
            isDragging_T = true;
            int itemH = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;
            int deltaItems = (startY_T - y) / itemH;
            int newOffset = startOffset_T + deltaItems;

            int hdrH = HDR_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2 + 2;
            int listTop = hdrH + 1;
            int skH   = SUB_FONT.getHeight() + PAD * 2;
            int listH = getHeight() - listTop - skH;

            int visible = listH / itemH;
            if (visible < 1) visible = 1;
            int count = pm.getTrackCount();
            int maxScroll = Math.max(0, count - visible);

            if (newOffset < 0) newOffset = 0;
            if (newOffset > maxScroll) newOffset = maxScroll;

            if (scrollOffset != newOffset) {
                scrollOffset = newOffset;
                if (selectedIndex < scrollOffset) selectedIndex = scrollOffset;
                if (selectedIndex >= scrollOffset + visible) selectedIndex = scrollOffset + visible - 1;
                repaint();
            }
        }
    }

    protected void pointerReleased(int x, int y) {
        if (!isDragging_T && (System.currentTimeMillis() - pressTime_T) < 300) {
            int skH = SUB_FONT.getHeight() + PAD * 2;
            int h = getHeight();
            int w = getWidth();

            if (nokiaMenuOpen) {
                int itemH  = NAME_FONT.getHeight() + 6;
                int menuSize = nokiaMenuItems.length;
                int menuH  = itemH * menuSize + PAD * 2;
                int menuY  = h - skH - menuH;

                if (y >= menuY && y <= menuY + menuH) {
                    int clickedIdx = (y - menuY - PAD) / itemH;
                    if (clickedIdx >= 0 && clickedIdx < menuSize) {
                        nokiaMenuSel = clickedIdx;
                        nokiaMenuOpen = false;
                        executeNokiaMenuItem(nokiaMenuSel);
                        repaint();
                    }
                } else if (y > h - skH) { // Softkey bar
                    if (x > w / 2) { nokiaMenuOpen = false; repaint(); } // Close
                    else           { nokiaMenuOpen = false; executeNokiaMenuItem(nokiaMenuSel); repaint(); }
                } else {
                    nokiaMenuOpen = false; repaint();
                }
                return;
            }

            if (y > h - skH) {
                if (x > w / 2) {
                    pm.setListener(prevListener);
                    display.setCurrent(backScreen);
                } else {
                    nokiaMenuOpen = true; nokiaMenuSel = 0; repaint();
                }
                return;
            }

            int hdrH = HDR_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2 + 2;
            int listTop = hdrH + 1;

            if (y >= listTop) {
                int itemH = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;
                int clickedIndex = scrollOffset + (y - listTop) / itemH;
                int count = pm.getTrackCount();
                if (clickedIndex >= 0 && clickedIndex < count) {
                    selectedIndex = clickedIndex;
                    mqReset();
                    repaint();
                    jumpToSelected(); // fire selection
                }
            }
        }
    }

    protected void keyPressed(int keyCode) {
        if (keyCode == -6) {
            if (nokiaMenuOpen) {
                nokiaMenuOpen = false;
                executeNokiaMenuItem(nokiaMenuSel);
            } else {
                nokiaMenuOpen = true;
                nokiaMenuSel  = 0;
            }
            repaint();
            return;
        }
        if (keyCode == -7) {
            if (nokiaMenuOpen) {
                nokiaMenuOpen = false;
                repaint();
            } else {
                pm.setListener(prevListener);
                display.setCurrent(backScreen);
            }
            return;
        }
        if (nokiaMenuOpen) {
            int action = getGameAction(keyCode);
            if (action == UP && nokiaMenuSel > 0) {
                nokiaMenuSel--;
                repaint();
            } else if (action == DOWN && nokiaMenuSel < nokiaMenuItems.length - 1) {
                nokiaMenuSel++;
                repaint();
            } else if (action == FIRE || keyCode == -5) {
                nokiaMenuOpen = false;
                executeNokiaMenuItem(nokiaMenuSel);
                repaint();
            }
            return;
        }
        int count  = pm.getTrackCount();
        if (count == 0) return;
        int action = getGameAction(keyCode);
        if (action == UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                mqReset();
                ensureVisible(selectedIndex);
                repaint();
            }
        } else if (action == DOWN) {
            if (selectedIndex < count - 1) {
                selectedIndex++;
                mqReset();
                ensureVisible(selectedIndex);
                repaint();
            }
        } else if (action == FIRE || keyCode == -5) {
            jumpToSelected();
        }
    }

    private void executeNokiaMenuItem(int index) {
        if (index == 0) jumpToSelected(); // Play
        else if (index == 1) toggleAutoplay(); // Autoplay
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUB_FONT);
        int labelY = barY + (skH - SUB_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(COLOR_TEXT1);
            g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_TEXT2);
            g.drawString("Close", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(COLOR_TEXT1);
            g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_TEXT2);
            g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h) {
        int itemH  = NAME_FONT.getHeight() + 6;
        int skH    = SUB_FONT.getHeight() + PAD * 2;
        int menuH  = itemH * nokiaMenuItems.length + PAD * 2;
        int menuY  = h - skH - menuH;

        g.setColor(COLOR_HEADER);
        g.fillRect(0, menuY, w, menuH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, menuY, w, menuY);

        for (int i = 0; i < nokiaMenuItems.length; i++) {
            int y = menuY + PAD + i * itemH;
            if (i == nokiaMenuSel) {
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y - 2, w, itemH);
                g.setColor(COLOR_BG);
            } else {
                g.setColor(COLOR_TEXT1);
            }
            g.setFont(NAME_FONT);
            g.drawString(nokiaMenuItems[i], PAD, y, Graphics.LEFT | Graphics.TOP);
        }
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

    private void jumpToSelected() {
        pm.play(selectedIndex);
        display.setCurrent(backScreen);
        // Restore listener — NowPlayingScreen will re-register itself on setCurrent
        pm.setListener(prevListener);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            pm.setListener(prevListener);
            display.setCurrent(backScreen);
        } else if (c == CMD_PLAY) {
            jumpToSelected();
        } else if (c == CMD_AUTOPLAY) {
            toggleAutoplay();
        }
    }

    private void toggleAutoplay() {
        pm.toggleAutoplay();
        updateNokiaMenuItems();
        repaint();
    }

    private void updateNokiaMenuItems() {
        String label = pm.isAutoplayEnabled() ? "Autoplay: ON" : "Autoplay: OFF";
        nokiaMenuItems = new String[] { "Play", label };
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Header
        int hdrH = HDR_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2 + 2;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, 0, w, hdrH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, hdrH, w, hdrH);
        g.setFont(HDR_FONT);
        g.setColor(COLOR_TEXT1);
        g.drawString("Queue", w / 2, PAD, Graphics.HCENTER | Graphics.TOP);

        // Track count sub-label
        int count = pm.getTrackCount();
        g.setFont(SUB_FONT);
        g.setColor(COLOR_TEXT2);
        String headerSub = count + " tracks";
        if (pm.isAutoplayEnabled() && !pm.isStationMode()) {
            headerSub += " \u00B7 Autoplay";
        }
        g.drawString(headerSub, w / 2, PAD + HDR_FONT.getHeight() + 2,
                     Graphics.HCENTER | Graphics.TOP);

        int listTop = hdrH + 1;
        int skH     = SUB_FONT.getHeight() + PAD * 2;
        int listH   = h - listTop - skH;
        int itemH   = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;
        int numW    = NUM_FONT.stringWidth("999") + PAD;
        int visible = listH / itemH + 2;
        int end     = Math.min(count, scrollOffset + visible);
        int textX   = numW + PAD / 2;
        int availW  = w - textX - PAD - 3;

        // Clip to list area
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, listTop, w, listH);

        for (int i = scrollOffset; i < end; i++) {
            int y       = listTop + (i - scrollOffset) * itemH;
            boolean cur = (i == currentIndex);
            boolean sel = (i == selectedIndex);

            // Row background
            if (sel) {
                g.setColor(COLOR_SEL_BG);
                g.fillRect(0, y, w, itemH);
            }

            // Accent left bar for currently playing track
            if (cur) {
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y, 3, itemH);
            }

            // Track number
            g.setFont(NUM_FONT);
            g.setColor(cur ? COLOR_ACCENT : COLOR_TEXT2);
            g.drawString(String.valueOf(i + 1), numW - PAD / 2, y + PAD,
                         Graphics.RIGHT | Graphics.TOP);

            // Track name
            g.setFont(NAME_FONT);
            g.setColor(cur ? COLOR_ACCENT : COLOR_TEXT1);
            String name   = pm.getTrackName(i);
            String artist = pm.getTrackArtist(i);


            if (sel) {
                int ovf = NAME_FONT.stringWidth(name) - availW;
                if (artist != null && artist.length() > 0)
                    ovf = Math.max(ovf, SUB_FONT.stringWidth(artist) - availW);
                mqMaxOvf = ovf > 0 ? ovf + 16 : 0;
            }

            if (sel && NAME_FONT.stringWidth(name) > availW) {
                int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                g.setClip(textX, y + PAD, availW, NAME_FONT.getHeight());
                g.drawString(name, textX - mqOffset, y + PAD, Graphics.LEFT | Graphics.TOP);
                g.setClip(scx, scy, scw, sch);
            } else {
                g.drawString(clip(name, NAME_FONT, availW), textX, y + PAD,
                             Graphics.LEFT | Graphics.TOP);
            }

            // Artist
            g.setFont(SUB_FONT);
            g.setColor(COLOR_TEXT2);
            if (artist != null && artist.length() > 0) {
                int subY = y + PAD + NAME_FONT.getHeight();
                if (sel && SUB_FONT.stringWidth(artist) > availW) {
                    int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                    g.setClip(textX, subY, availW, SUB_FONT.getHeight());
                    g.drawString(artist, textX - mqOffset, subY, Graphics.LEFT | Graphics.TOP);
                    g.setClip(scx, scy, scw, sch);
                } else {
                    g.drawString(clip(artist, SUB_FONT, availW), textX, subY,
                                 Graphics.LEFT | Graphics.TOP);
                }
            }

            // Divider
            g.setColor(COLOR_DIVIDER);
            g.drawLine(numW, y + itemH - 1, w - PAD, y + itemH - 1);
        }

        // Scroll bar
        if (count > 0 && listH > 0) {
            int barH = Math.max(8, listH * Math.min(count, visible) / count);
            int barY = listTop + (listH - barH) * scrollOffset / Math.max(1, count - visible);
            g.setColor(COLOR_DIVIDER);
            g.fillRect(w - 3, listTop, 3, listH);
            g.setColor(COLOR_ACCENT);
            g.fillRect(w - 3, barY, 3, barH);
        }

        g.setClip(cx, cy, cw, ch);
        drawSoftKeyBar(g, w, h, skH);
        if (nokiaMenuOpen) drawNokiaMenu(g, w, h);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void ensureVisible(int index) {
        int h       = getHeight();
        int hdrH    = HDR_FONT.getHeight() + PAD * 2;
        int skH     = SUB_FONT.getHeight() + PAD * 2;
        int listH   = h - hdrH - 1 - skH;
        int itemH   = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;
        if (itemH <= 0) return;
        int visible = listH / itemH;
        if (visible < 1) visible = 1;
        if (index < scrollOffset) {
            scrollOffset = index;
        } else if (index >= scrollOffset + visible) {
            scrollOffset = index - visible + 1;
        }
    }

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
