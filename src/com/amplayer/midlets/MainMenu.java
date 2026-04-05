package com.amplayer.midlets;

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
    // Commands
    // -------------------------------------------------------------------------

    private static final Command CMD_SELECT = new Command("Select", Command.OK,   1);
    private static final Command CMD_EXIT   = new Command("Exit",   Command.EXIT, 1);

    private final AppleMusicMIDlet midlet;
    private final Display          display;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public MainMenu(AppleMusicMIDlet midlet, Display display) {
        super();
        setTitle("J2AMP");
        this.midlet  = midlet;
        this.display = display;
        addCommand(CMD_SELECT);
        addCommand(CMD_EXIT);
        setCommandListener(this);
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w     = getWidth();
        int h     = getHeight();
        int listH = h - TITLE_BAR_H;

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

            g.setFont(NAME_FONT);
            g.setColor(COLOR_NAME);
            g.drawString(LABELS[i], PAD + ACCENT_W + 4, y + PAD, Graphics.LEFT | Graphics.TOP);

            g.setFont(SUBNAME_FONT);
            g.setColor(COLOR_SUBNAME);
            g.drawString(SUBLABELS[i], PAD + ACCENT_W + 4,
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
    }

    // -------------------------------------------------------------------------
    // Key handling
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
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
                int h     = getHeight();
                int listH = h - TITLE_BAR_H;
                int vis   = listH / ITEM_H;
                if (selectedIndex >= scrollOffset + vis)
                    scrollOffset = selectedIndex - vis + 1;
                repaint();
            }
        } else if (action == FIRE) {
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
}
