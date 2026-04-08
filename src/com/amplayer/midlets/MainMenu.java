package com.amplayer.midlets;

import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.Settings;
import java.util.Calendar;
import java.util.Date;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

public class MainMenu extends Canvas implements CommandListener {

    public static final int ITEM_NOW_PLAYING = 0;
    public static final int ITEM_SEARCH      = 1;
    public static final int ITEM_STATIONS    = 2;
    public static final int ITEM_SONGS       = 3;
    public static final int ITEM_ALBUMS      = 4;
    public static final int ITEM_PLAYLIST    = 5;
    public static final int ITEM_SETTINGS    = 6;

    // -------------------------------------------------------------------------
    // Colors — same palette as BaseList
    // -------------------------------------------------------------------------

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_SELECTED = 0x1C1C1E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_NAME     = 0xFFFFFF;
    private static final int COLOR_SUBNAME  = 0x8E8E93;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;

    // -------------------------------------------------------------------------
    // Fonts & layout
    // -------------------------------------------------------------------------

    private static final Font NAME_FONT    = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUBNAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font STATUS_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD          = 8;
    private static final int ACCENT_W     = 3;
    private static final int STATUS_BAR_H = STATUS_FONT.getHeight() + PAD;
    private static final int ITEM_H       = NAME_FONT.getHeight() + SUBNAME_FONT.getHeight() + PAD * 2;

    // -------------------------------------------------------------------------
    // Menu items
    // -------------------------------------------------------------------------

    private static final String[] LABELS = {
        "Now Playing",
        "Search",
        "Stations",
        "Songs",
        "Albums",
        "Playlists",
        "Settings"
    };

    private static final String[] SUBLABELS = {
        "Currently playing track",
        "Find music in catalog or library",
        "Your personal stations",
        "Your library songs",
        "Your library albums",
        "Your playlists",
        "Marquee, cache, Last.fm, Queue,.."
    };

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;
    private int scrollOffset  = 0;   // first visible item index

    // "Press back again to exit" state
    private long    backPressedTime = 0;
    private boolean showExitToast   = false;
    private static final long EXIT_TIMEOUT = 3000; // 3 seconds

    // -------------------------------------------------------------------------
    // Commands  (used only on non-Nokia devices)
    // -------------------------------------------------------------------------

    private static final Command CMD_SELECT = new Command("Select", Command.OK,   1);
    private static final Command CMD_EXIT   = new Command("Exit",   Command.EXIT, 1);

    private final AppleMusicMIDlet midlet;
    private final Display          display;
    private       PlaybackManager  pm;  // set after PM is created; used for live track subtext

    // -------------------------------------------------------------------------
    // Nokia soft-key bar
    // -------------------------------------------------------------------------

    private final boolean isNokia;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public MainMenu(AppleMusicMIDlet midlet, Display display) {
        super();
        this.midlet  = midlet;
        this.display = display;
        isNokia = Settings.getDeviceEnvironment().indexOf("nokia") >= 0;
        if (!isNokia) {
            addCommand(CMD_SELECT);
            addCommand(CMD_EXIT);
            setCommandListener(this);
            setFullScreenMode(false);
        } else {
            setFullScreenMode(true);
        }
    }

    /** Called once the PlaybackManager is ready so the Now Playing row can show live info. */
    public void setPlaybackManager(PlaybackManager pm) {
        this.pm = pm;
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w     = getWidth();
        int h     = getHeight();
        int skH   = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
        int listTop = STATUS_BAR_H;
        int listH   = h - STATUS_BAR_H - skH;

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // ── Status bar (time + battery) ──────────────────────────
        g.setColor(0x111111);
        g.fillRect(0, 0, w, STATUS_BAR_H);
        g.setFont(STATUS_FONT);
        int stY = (STATUS_BAR_H - STATUS_FONT.getHeight()) / 2;

        // Time HH:MM
        g.setColor(COLOR_SUBNAME);
        g.drawString(getCurrentTime(), PAD, stY, Graphics.LEFT | Graphics.TOP);

        // Battery icon (right side)
        int batLevel = getBatteryLevel();
        if (batLevel >= 0) {
            int batW = 22;   // body width (longer)
            int batH = 10;    // body height (less wide)
            int nubW = 2;
            int batX = w - PAD - batW - nubW;
            int batY = (STATUS_BAR_H - batH) / 2;

            // Dark inner background (gives depth)
            g.setColor(0x1A1A1A);
            g.fillRect(batX + 1, batY + 1, batW - 2, batH - 2);

            // Body outline
            g.setColor(0x636366);
            g.drawRect(batX, batY, batW - 1, batH - 1);

            // Nub on right
            int nubH = batH - 4;
            if (nubH < 2) nubH = 2;
            int nubY = batY + (batH - nubH) / 2;
            g.setColor(0x636366);
            g.fillRect(batX + batW, nubY, nubW, nubH);

            // Fill proportional to level
            int fillW = (batW - 4) * batLevel / 100;
            if (fillW > 0) {
                int fillColor;
                int hlColor;  // highlight
                if (batLevel > 50)       { fillColor = 0x30D158; hlColor = 0x5AE678; }
                else if (batLevel > 20)  { fillColor = 0xFFD60A; hlColor = 0xFFE545; }
                else                     { fillColor = 0xFF453A; hlColor = 0xFF6B61; }

                // Main fill
                g.setColor(fillColor);
                g.fillRect(batX + 2, batY + 2, fillW, batH - 4);

                // Top highlight (1px lighter stripe)
                g.setColor(hlColor);
                g.drawLine(batX + 2, batY + 2, batX + 2 + fillW - 1, batY + 2);
            }
        }

        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, STATUS_BAR_H - 1, w, STATUS_BAR_H - 1);

        // ── List area ────────────────────────────────────────────
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, listTop, w, listH);

        int visibleCount = listH / ITEM_H + 1;
        int end = Math.min(LABELS.length, scrollOffset + visibleCount);

        for (int i = scrollOffset; i < end; i++) {
            int y = listTop + (i - scrollOffset) * ITEM_H;

            if (i == selectedIndex) {
                g.setColor(COLOR_SELECTED);
                g.fillRect(0, y, w, ITEM_H);
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y, ACCENT_W, ITEM_H);
            }

            int textX  = PAD + ACCENT_W + 4;
            int availW = w - textX - PAD - 3;

            g.setFont(NAME_FONT);
            g.setColor(COLOR_NAME);
            g.drawString(clip(LABELS[i], NAME_FONT, availW), textX, y + PAD,
                         Graphics.LEFT | Graphics.TOP);

            String sub = (i == ITEM_NOW_PLAYING) ? nowPlayingSub() : SUBLABELS[i];
            g.setFont(SUBNAME_FONT);
            g.setColor(COLOR_SUBNAME);
            g.drawString(clip(sub, SUBNAME_FONT, availW), textX,
                         y + PAD + NAME_FONT.getHeight(), Graphics.LEFT | Graphics.TOP);

            g.setColor(COLOR_DIVIDER);
            g.drawLine(PAD, y + ITEM_H - 1, w - PAD, y + ITEM_H - 1);
        }

        // Scroll indicator
        int totalH = LABELS.length * ITEM_H;
        if (totalH > listH) {
            int full     = listH / ITEM_H;
            int barH     = Math.max(8, listH * full / LABELS.length);
            int maxScroll = LABELS.length - full;
            int barY     = listTop + (maxScroll > 0
                            ? (listH - barH) * scrollOffset / maxScroll : 0);
            g.setColor(0x3A3A3C);
            g.fillRect(w - 3, listTop, 3, listH);
            g.setColor(COLOR_ACCENT);
            g.fillRect(w - 3, barY, 3, barH);
        }

        // Restore clip
        g.setClip(cx, cy, cw, ch);

        // ── Exit toast ───────────────────────────────────────────
        if (showExitToast) {
            String msg = "Press back again to exit";
            g.setFont(SUBNAME_FONT);
            int tw = SUBNAME_FONT.stringWidth(msg) + PAD * 4;
            int th = SUBNAME_FONT.getHeight() + PAD * 2;
            int tx = (w - tw) / 2;
            int ty = h - skH - th - PAD * 2;
            g.setColor(0x333333);
            g.fillRoundRect(tx, ty, tw, th, 8, 8);
            g.setColor(COLOR_NAME);
            g.drawString(msg, w / 2, ty + PAD, Graphics.HCENTER | Graphics.TOP);
        }

        if (isNokia) drawSoftKeyBar(g, w, h, skH);
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(0x111111);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUBNAME_FONT);
        int labelY = barY + (skH - SUBNAME_FONT.getHeight()) / 2;
        g.setColor(COLOR_NAME);
        g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_SUBNAME);
        g.drawString("Exit", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
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
        if (Math.abs(y - startY_T) > 5) {
            isDragging_T = true;
            int deltaItems = (startY_T - y) / ITEM_H;
            int newOffset = startOffset_T + deltaItems;
            
            int skH   = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
            int listH = getHeight() - STATUS_BAR_H - skH;
            int vis   = listH / ITEM_H;
            int maxScroll = Math.max(0, LABELS.length - vis);
            
            if (newOffset < 0) newOffset = 0;
            if (newOffset > maxScroll) newOffset = maxScroll;
            
            if (scrollOffset != newOffset) {
                scrollOffset = newOffset;
                // keep selection somewhat in view
                if (selectedIndex < scrollOffset) selectedIndex = scrollOffset;
                if (selectedIndex >= scrollOffset + vis) selectedIndex = scrollOffset + vis - 1;
                repaint();
            }
        }
    }

    protected void pointerReleased(int x, int y) {
        if (!isDragging_T && (System.currentTimeMillis() - pressTime_T) < 300) {
            int listTop = STATUS_BAR_H;
            if (y >= listTop) {
                int clickedIndex = scrollOffset + (y - listTop) / ITEM_H;
                if (clickedIndex >= 0 && clickedIndex < LABELS.length) {
                    selectedIndex = clickedIndex;
                    repaint();
                    fireSelection();
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Input
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        // Clear exit toast on any non-back key
        if (isNokia) {
            if (keyCode == -6) { showExitToast = false; fireSelection(); return; }
            if (keyCode == -7) { handleBack(); return; }
        }
        showExitToast = false;
        int action = getGameAction(keyCode);
        if (action == UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
                repaint();
            }
        } else if (action == DOWN) {
            if (selectedIndex < LABELS.length - 1) {
                selectedIndex++;
                int skH   = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
                int listH = getHeight() - STATUS_BAR_H - skH;
                int vis   = listH / ITEM_H;
                if (selectedIndex >= scrollOffset + vis)
                    scrollOffset = selectedIndex - vis + 1;
                repaint();
            }
        } else if (action == FIRE || keyCode == -5) {
            fireSelection();
        }
    }

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

    private void fireSelection() {
        switch (selectedIndex) {
            case ITEM_NOW_PLAYING: midlet.showNowPlaying(); break;
            case ITEM_SEARCH:     midlet.showSearch();     break;
            case ITEM_STATIONS:   midlet.showStations();   break;
            case ITEM_SONGS:      midlet.showSongs();      break;
            case ITEM_ALBUMS:     midlet.showAlbums();     break;
            case ITEM_PLAYLIST:   midlet.showPlaylist();   break;
            case ITEM_SETTINGS:   midlet.showSettings();   break;
        }
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_EXIT) {
            handleBack();
        } else if (c == CMD_SELECT) {
            fireSelection();
        }
    }

    private void handleBack() {
        long now = System.currentTimeMillis();
        if (showExitToast && (now - backPressedTime) < EXIT_TIMEOUT) {
            midlet.notifyDestroyed();
        } else {
            backPressedTime = now;
            showExitToast   = true;
            repaint();
            // Auto-hide toast after timeout
            new Thread(new Runnable() {
                public void run() {
                    try { Thread.sleep(EXIT_TIMEOUT); } catch (InterruptedException e) {}
                    showExitToast = false;
                    repaint();
                }
            }).start();
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private String nowPlayingSub() {
        if (pm == null) return SUBLABELS[ITEM_NOW_PLAYING];
        String name = pm.getCurrentName();
        if (name == null || name.length() == 0) return SUBLABELS[ITEM_NOW_PLAYING];
        String artist = pm.getCurrentArtist();
        if (artist != null && artist.length() > 0) return artist + " - " + name;
        return name;
    }

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private static String getCurrentTime() {
        Calendar cal = Calendar.getInstance();
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int min  = cal.get(Calendar.MINUTE);
        return (hour < 10 ? "0" : "") + hour + ":" + (min < 10 ? "0" : "") + min;
    }

    private static int getBatteryLevel() {
        try {
            String level = System.getProperty("com.nokia.mid.batterylevel");
            if (level != null && level.length() > 0) {
                return Integer.parseInt(level.trim());
            }
        } catch (Exception ignored) {}
        return -1;
    }
}
