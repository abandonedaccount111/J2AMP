package com.amplayer.midlets;

import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.Settings;
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
    public static final int ITEM_SONGS       = 2;
    public static final int ITEM_ALBUMS      = 3;
    public static final int ITEM_PLAYLIST    = 4;
    public static final int ITEM_SETTINGS    = 5;

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

    private static final Font TITLE_FONT   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font NAME_FONT    = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUBNAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD         = 8;
    private static final int ACCENT_W    = 3;
    private static final int TITLE_BAR_H = TITLE_FONT.getHeight() + PAD * 2;
    private static final int ITEM_H      = NAME_FONT.getHeight() + SUBNAME_FONT.getHeight() + PAD * 2;

    // -------------------------------------------------------------------------
    // Menu items
    // -------------------------------------------------------------------------

    private static final String[] LABELS = {
        "Now Playing",
        "Search",
        "Songs",
        "Albums",
        "Playlist",
        "Settings"
    };

    private static final String[] SUBLABELS = {
        "Currently playing track",
        "Find music in catalog or library",
        "Your library songs",
        "Your library albums",
        "Your playlists",
        "Marquee, cache, Last.fm"
    };

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;
    private int scrollOffset  = 0;   // first visible item index

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
        setTitle("J2AMP");
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
        int listH = h - TITLE_BAR_H - skH;

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Title bar (drawn first, outside clip)
        g.setColor(0x111111);
        g.fillRect(0, 0, w, TITLE_BAR_H);
        g.setFont(TITLE_FONT);
        g.setColor(COLOR_ACCENT);
        g.drawString("J2AMP", PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, TITLE_BAR_H - 2, w, 2);

        // Clip list area so items cannot overdraw the title bar
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, TITLE_BAR_H, w, listH);

        int visibleCount = listH / ITEM_H + 1; // +1 to fill partial item at bottom
        int end = Math.min(LABELS.length, scrollOffset + visibleCount);

        for (int i = scrollOffset; i < end; i++) {
            int y = TITLE_BAR_H + (i - scrollOffset) * ITEM_H;

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
            int full     = listH / ITEM_H; // fully visible items
            int barH     = Math.max(8, listH * full / LABELS.length);
            int maxScroll = LABELS.length - full;
            int barY     = TITLE_BAR_H + (maxScroll > 0
                            ? (listH - barH) * scrollOffset / maxScroll : 0);
            g.setColor(0x3A3A3C);
            g.fillRect(w - 3, TITLE_BAR_H, 3, listH);
            g.setColor(COLOR_ACCENT);
            g.fillRect(w - 3, barY, 3, barH);
        }

        // Restore clip
        g.setClip(cx, cy, cw, ch);
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
    // Key handling
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        if (isNokia) {
            if (keyCode == -6) { fireSelection(); return; }
            if (keyCode == -7) { midlet.notifyDestroyed(); return; }
        }
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
                int listH = getHeight() - TITLE_BAR_H - skH;
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
            midlet.notifyDestroyed();
        } else if (c == CMD_SELECT) {
            fireSelection();
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
}
