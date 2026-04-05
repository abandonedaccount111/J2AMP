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
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

/**
 * Settings screen — Canvas-based scrollable list of configurable options.
 *
 * Rows (some conditional):
 *   0  Marquee                 toggle on/off
 *   1  Cache Size              numeric dialog
 *   2  Performance             cycle Auto / Low / Normal (presets art + preload)
 *   3  Album Art               independent toggle
 *   4  Preload                 independent toggle
 *   5  Last.fm                 sign in / sign out
 *   6  Now Playing Updates     toggle (only when signed in to Last.fm)
 *   7  Visualizer              open VisualizerCanvas
 */
public class SettingsForm extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Colors / fonts
    // -------------------------------------------------------------------------

    private static final int COLOR_BG      = 0x000000;
    private static final int COLOR_HDR     = 0x111111;
    private static final int COLOR_DIVIDER = 0x2C2C2E;
    private static final int COLOR_ACCENT  = 0xFA2D48;
    private static final int COLOR_SEL_BG  = 0x1C1C1E;
    private static final int COLOR_TEXT1   = 0xFFFFFF;
    private static final int COLOR_TEXT2   = 0x8E8E93;

    private static final Font HDR_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font NAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUB_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD      = 8;
    private static final int ACCENT_W = 3;
    private static final int HDR_H    = HDR_FONT.getHeight() + PAD * 2;
    private static final int ITEM_H   = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;

    // -------------------------------------------------------------------------
    // Logical row IDs  (fixed — display rows are remapped via logicalRow())
    // -------------------------------------------------------------------------

    private static final int LR_MARQUEE    = 0;
    private static final int LR_CACHE      = 1;
    private static final int LR_PERF       = 2;
    private static final int LR_ART        = 3;
    private static final int LR_PRELOAD    = 4;
    private static final int LR_LASTFM     = 5;
    private static final int LR_LASTFM_NP  = 6;
    private static final int LR_VISUALIZER = 7;

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK   = new Command("Back",   Command.BACK, 1);
    private static final Command CMD_SELECT = new Command("Select", Command.OK,   1);

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final AppleMusicMIDlet midlet;
    private final Display          display;
    private final Displayable      backScreen;
    private final PlaybackManager  pm;

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;
    private int scrollOffset  = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public SettingsForm(AppleMusicMIDlet midlet, Display display, Displayable backScreen,
                        PlaybackManager pm) {
        super();
        this.midlet     = midlet;
        this.display    = display;
        this.backScreen = backScreen;
        this.pm         = pm;
        setFullScreenMode(true);
        addCommand(CMD_BACK);
        addCommand(CMD_SELECT);
        setCommandListener(this);
    }

    // -------------------------------------------------------------------------
    // Dynamic row count and mapping
    // -------------------------------------------------------------------------

    private boolean signedIn() {
        return Settings.lastFmUser != null && Settings.lastFmUser.length() > 0;
    }

    /** Total number of visible rows. */
    private int rowCount() {
        // Base: marquee, cache, performance, art, preload, lastfm, visualizer = 7
        // +1 for Now Playing sub-row when signed in to Last.fm
        return signedIn() ? 8 : 7;
    }

    /** Map a display-row index to a logical row constant. */
    private int logicalRow(int di) {
        // Rows 0-5 map 1:1 (marquee, cache, perf, art, preload, lastfm)
        if (di <= LR_LASTFM) return di;
        if (signedIn()) {
            return (di == 6) ? LR_LASTFM_NP : LR_VISUALIZER;
        }
        return LR_VISUALIZER; // di==6 → visualizer when not signed in
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w     = getWidth();
        int h     = getHeight();
        int total = rowCount();

        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Header
        g.setColor(COLOR_HDR);
        g.fillRect(0, 0, w, HDR_H);
        g.setFont(HDR_FONT);
        g.setColor(COLOR_TEXT1);
        g.drawString("Settings", PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, HDR_H - 2, w, 2);

        int listH = h - HDR_H;
        int cx = g.getClipX(), cy2 = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, HDR_H, w, listH);

        int visible = listH / ITEM_H + 2;
        int end     = Math.min(total, scrollOffset + visible);

        for (int i = scrollOffset; i < end; i++) {
            int y   = HDR_H + (i - scrollOffset) * ITEM_H;
            boolean sel = (i == selectedIndex);
            int     lr  = logicalRow(i);

            if (sel) {
                g.setColor(COLOR_SEL_BG);
                g.fillRect(0, y, w, ITEM_H);
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y, ACCENT_W, ITEM_H);
            }

            int textX = PAD + ACCENT_W + 4;
            int availW = w - textX - PAD - 3;

            g.setFont(NAME_FONT);
            g.setColor(COLOR_TEXT1);
            g.drawString(clip(rowLabel(lr), NAME_FONT, availW), textX, y + PAD,
                         Graphics.LEFT | Graphics.TOP);

            g.setFont(SUB_FONT);
            g.setColor(COLOR_TEXT2);
            g.drawString(clip(rowSub(lr), SUB_FONT, availW), textX,
                         y + PAD + NAME_FONT.getHeight(), Graphics.LEFT | Graphics.TOP);

            g.setColor(COLOR_DIVIDER);
            g.drawLine(PAD, y + ITEM_H - 1, w - PAD, y + ITEM_H - 1);
        }

        // Scrollbar
        int totalH = total * ITEM_H;
        if (totalH > listH && total > 1) {
            int vis  = listH / ITEM_H;
            int barH = Math.max(8, listH * vis / total);
            int barY = HDR_H + (listH - barH) * scrollOffset / Math.max(1, total - vis);
            g.setColor(COLOR_DIVIDER);
            g.fillRect(w - 3, HDR_H, 3, listH);
            g.setColor(COLOR_ACCENT);
            g.fillRect(w - 3, barY, 3, barH);
        }

        g.setClip(cx, cy2, cw, ch);
    }

    // -------------------------------------------------------------------------
    // Row text
    // -------------------------------------------------------------------------

    private static String rowLabel(int lr) {
        switch (lr) {
            case LR_MARQUEE:    return "Marquee";
            case LR_CACHE:      return "Cache Size";
            case LR_PERF:       return "Performance";
            case LR_ART:        return "Album Art";
            case LR_PRELOAD:    return "Preload";
            case LR_LASTFM:     return "Last.fm";
            case LR_LASTFM_NP:  return "Now Playing Updates";
            case LR_VISUALIZER: return "Visualizer";
        }
        return "";
    }

    private static String rowSub(int lr) {
        switch (lr) {
            case LR_MARQUEE:
                return Settings.marqueeEnabled ? "Scrolling text: On" : "Scrolling text: Off";
            case LR_CACHE:
                return "Limit: " + Settings.cacheMb + " MB";
            case LR_PERF: {
                if ("low".equals(Settings.performanceMode))
                    return "Low Memory (forced)";
                if ("normal".equals(Settings.performanceMode))
                    return "Normal (forced)";
                return Settings.lowMemoryMode ? "Auto - Low Memory" : "Auto - Normal";
            }
            case LR_ART:
                return Settings.artEnabled ? "Load album art: On" : "Load album art: Off";
            case LR_PRELOAD:
                return Settings.preloadEnabled
                    ? "Preload next/prev track: On"
                    : "Preload next/prev track: Off";
            case LR_LASTFM:
                return (Settings.lastFmUser != null && Settings.lastFmUser.length() > 0)
                    ? "Signed in as " + Settings.lastFmUser
                    : "Not signed in";
            case LR_LASTFM_NP:
                return Settings.lastFmNowPlaying
                    ? "Send Now Playing: On"
                    : "Send Now Playing: Off";
            case LR_VISUALIZER:
                return "Open spectrum visualizer";
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // Key input
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        int total  = rowCount();
        if (action == UP) {
            if (selectedIndex > 0) { selectedIndex--; ensureVisible(); repaint(); }
        } else if (action == DOWN) {
            if (selectedIndex < total - 1) { selectedIndex++; ensureVisible(); repaint(); }
        } else if (action == FIRE) {
            activate(logicalRow(selectedIndex));
        }
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

    // -------------------------------------------------------------------------
    // Row actions
    // -------------------------------------------------------------------------

    private void activate(int lr) {
        switch (lr) {
            case LR_MARQUEE:
                Settings.marqueeEnabled = !Settings.marqueeEnabled;
                Settings.save();
                repaint();
                break;
            case LR_CACHE:
                openCacheDialog();
                break;
            case LR_PERF:
                cyclePerformanceMode();
                break;
            case LR_ART:
                Settings.artEnabled = !Settings.artEnabled;
                Settings.save();
                repaint();
                break;
            case LR_PRELOAD:
                Settings.preloadEnabled = !Settings.preloadEnabled;
                Settings.save();
                repaint();
                break;
            case LR_LASTFM:
                if (signedIn()) openLastFmLogout();
                else            openLastFmLogin();
                break;
            case LR_LASTFM_NP:
                if (signedIn()) {
                    Settings.lastFmNowPlaying = !Settings.lastFmNowPlaying;
                    Settings.save();
                    repaint();
                }
                break;
            case LR_VISUALIZER:
                openVisualizer();
                break;
        }
    }

    private void cyclePerformanceMode() {
        if ("auto".equals(Settings.performanceMode))      Settings.performanceMode = "low";
        else if ("low".equals(Settings.performanceMode))  Settings.performanceMode = "normal";
        else                                              Settings.performanceMode = "auto";
        Settings.applyPerformanceMode();
        Settings.save();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Cache dialog
    // -------------------------------------------------------------------------

    private void openCacheDialog() {
        final Form form = new Form("Cache Size");
        form.append(new StringItem("", "Enter maximum cache size in MB (1-200):"));
        final TextField tf = new TextField("MB", String.valueOf(Settings.cacheMb), 5,
                                           TextField.NUMERIC);
        form.append(tf);
        Command ok     = new Command("OK",     Command.OK,   1);
        Command cancel = new Command("Cancel", Command.BACK, 2);
        form.addCommand(ok);
        form.addCommand(cancel);
        final SettingsForm self = this;
        form.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    try {
                        int mb = Integer.parseInt(tf.getString().trim());
                        if (mb < 1)   mb = 1;
                        if (mb > 200) mb = 200;
                        Settings.cacheMb = mb;
                        Settings.save();
                        PlaybackManager.setCacheSize(mb);
                    } catch (NumberFormatException ignored) {}
                }
                display.setCurrent(self);
                repaint();
            }
        });
        display.setCurrent(form);
    }

    // -------------------------------------------------------------------------
    // Last.fm login / logout
    // -------------------------------------------------------------------------

    private void openLastFmLogin() {
        final Form form = new Form("Last.fm Sign In");
        form.append(new StringItem("", "Enter your Last.fm credentials:"));
        final TextField userTf = new TextField("Username", "", 64, TextField.ANY);
        final TextField passTf = new TextField("Password", "", 64, TextField.PASSWORD);
        form.append(userTf);
        form.append(passTf);
        final StringItem status = new StringItem("", "");
        form.append(status);
        Command signIn = new Command("Sign In", Command.OK,   1);
        Command cancel = new Command("Cancel",  Command.BACK, 2);
        form.addCommand(signIn);
        form.addCommand(cancel);
        final SettingsForm self = this;
        form.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    final String user = userTf.getString().trim();
                    final String pass = passTf.getString();
                    if (user.length() == 0 || pass.length() == 0) {
                        status.setText("Enter username and password.");
                        return;
                    }
                    status.setText("Signing in...");
                    final LastFmHelper lfm = new LastFmHelper();
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                String sk = lfm.getMobileSession(user, pass);
                                Settings.lastFmUser = user;
                                Settings.lastFmSk   = sk;
                                Settings.save();
                                midlet.onLastFmSignedIn(sk);
                                display.callSerially(new Runnable() {
                                    public void run() {
                                        display.setCurrent(self);
                                        repaint();
                                    }
                                });
                            } catch (final Exception e) {
                                final String msg = e.getMessage() != null
                                    ? e.getMessage() : "Sign in failed";
                                display.callSerially(new Runnable() {
                                    public void run() { status.setText("Error: " + msg); }
                                });
                            }
                        }
                    }).start();
                } else {
                    display.setCurrent(self);
                }
            }
        });
        display.setCurrent(form);
    }

    private void openLastFmLogout() {
        Settings.lastFmUser       = "";
        Settings.lastFmSk         = "";
        Settings.lastFmNowPlaying = false;
        Settings.save();
        midlet.onLastFmSignedOut();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Visualizer
    // -------------------------------------------------------------------------

    private void openVisualizer() {
        VisualizerCanvas viz = new VisualizerCanvas(display, this);
        if (pm != null) viz.setTrackInfo(pm.getCurrentName(), pm.getCurrentArtist());
        display.setCurrent(viz);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK)   display.setCurrent(backScreen);
        else if (c == CMD_SELECT) activate(logicalRow(selectedIndex));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void ensureVisible() {
        int listH = getHeight() - HDR_H;
        int vis   = listH / ITEM_H;
        if (vis < 1) vis = 1;
        if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
        else if (selectedIndex >= scrollOffset + vis) scrollOffset = selectedIndex - vis + 1;
    }

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
