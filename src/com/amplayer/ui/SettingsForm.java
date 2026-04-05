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
 * Settings screen — Canvas-based list of configurable options.
 *
 * Items:
 *   0  Marquee         toggle (on/off)
 *   1  Cache size      → open text-entry dialog
 *   2  Last.fm         status line + login / logout action
 *   3  Visualizer      → open VisualizerCanvas
 *
 * Navigation: UP/DOWN move selection, FIRE/SELECT activates.
 */
public class SettingsForm extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Colors / fonts  (same palette as the rest of the app)
    // -------------------------------------------------------------------------

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_HDR      = 0x111111;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_SEL_BG   = 0x1C1C1E;
    private static final int COLOR_TEXT1    = 0xFFFFFF;
    private static final int COLOR_TEXT2    = 0x8E8E93;

    private static final Font HDR_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font NAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUB_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD      = 8;
    private static final int ACCENT_W = 3;
    private static final int HDR_H    = HDR_FONT.getHeight() + PAD * 2;
    private static final int ITEM_H   = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;

    // -------------------------------------------------------------------------
    // Row indices
    // -------------------------------------------------------------------------

    private static final int ROW_MARQUEE    = 0;
    private static final int ROW_CACHE      = 1;
    private static final int ROW_LASTFM     = 2;
    // private static final int ROW_VISUALIZER = 3;
    private static final int ROW_COUNT      = 3;

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
        setFullScreenMode(false);
        addCommand(CMD_BACK);
        addCommand(CMD_SELECT);
        setCommandListener(this);
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
        g.setColor(COLOR_HDR);
        g.fillRect(0, 0, w, HDR_H);
        g.setFont(HDR_FONT);
        g.setColor(COLOR_TEXT1);
        g.drawString("Settings", PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, HDR_H - 2, w, 2);

        int listH = h - HDR_H;
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, HDR_H, w, listH);

        int visible = listH / ITEM_H + 2;
        int end = Math.min(ROW_COUNT, scrollOffset + visible);

        for (int i = scrollOffset; i < end; i++) {
            int y   = HDR_H + (i - scrollOffset) * ITEM_H;
            boolean sel = (i == selectedIndex);

            if (sel) {
                g.setColor(COLOR_SEL_BG);
                g.fillRect(0, y, w, ITEM_H);
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y, ACCENT_W, ITEM_H);
            }

            String label = rowLabel(i);
            String sub   = rowSub(i);

            g.setFont(NAME_FONT);
            g.setColor(COLOR_TEXT1);
            g.drawString(label, PAD + ACCENT_W + 4, y + PAD, Graphics.LEFT | Graphics.TOP);

            g.setFont(SUB_FONT);
            g.setColor(COLOR_TEXT2);
            g.drawString(sub, PAD + ACCENT_W + 4, y + PAD + NAME_FONT.getHeight(),
                         Graphics.LEFT | Graphics.TOP);

            g.setColor(COLOR_DIVIDER);
            g.drawLine(PAD, y + ITEM_H - 1, w - PAD, y + ITEM_H - 1);
        }

        g.setClip(cx, cy, cw, ch);
    }

    // -------------------------------------------------------------------------
    // Row label / sublabel helpers
    // -------------------------------------------------------------------------

    private static String rowLabel(int row) {
        switch (row) {
            case ROW_MARQUEE:    return "Marquee";
            case ROW_CACHE:      return "Cache Size";
            case ROW_LASTFM:     return "Last.fm";
            // case ROW_VISUALIZER: return "Visualizer";
        }
        return "";
    }

    private static String rowSub(int row) {
        switch (row) {
            case ROW_MARQUEE:
                return Settings.marqueeEnabled ? "Scrolling text: On" : "Scrolling text: Off";
            case ROW_CACHE:
                return "Limit: " + Settings.cacheMb + " MB  (select to change)";
            case ROW_LASTFM:
                if (Settings.lastFmUser.length() > 0)
                    return "Signed in as " + Settings.lastFmUser;
                return "Not signed in  (select to connect)";
            // case ROW_VISUALIZER:
            //     return "Open spectrum visualizer";
        }
        return "";
    }

    // -------------------------------------------------------------------------
    // Key input
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                ensureVisible();
                repaint();
            }
        } else if (action == DOWN) {
            if (selectedIndex < ROW_COUNT - 1) {
                selectedIndex++;
                ensureVisible();
                repaint();
            }
        } else if (action == FIRE) {
            activate(selectedIndex);
        }
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

    // -------------------------------------------------------------------------
    // Activation
    // -------------------------------------------------------------------------

    private void activate(int row) {
        switch (row) {
            case ROW_MARQUEE:
                Settings.marqueeEnabled = !Settings.marqueeEnabled;
                Settings.save();
                repaint();
                break;
            case ROW_CACHE:
                openCacheDialog();
                break;
            case ROW_LASTFM:
                if (Settings.lastFmUser.length() > 0) openLastFmLogout();
                else                                   openLastFmLogin();
                break;
            // case ROW_VISUALIZER:
            //     openVisualizer();
            //     break;
        }
    }

    // -------------------------------------------------------------------------
    // Cache size dialog
    // -------------------------------------------------------------------------

    private void openCacheDialog() {
        final Form form = new Form("Cache Size");
        form.append(new StringItem("", "Enter the maximum cache size in MB (1-200):"));
        final TextField tf = new TextField("", String.valueOf(Settings.cacheMb), 5,
                                           TextField.NUMERIC);
        form.append(tf);
        Command ok   = new Command("OK",     Command.OK,   1);
        Command back = new Command("Cancel", Command.BACK, 2);
        form.addCommand(ok);
        form.addCommand(back);
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
        final StringItem statusItem = new StringItem("", "");
        form.append(statusItem);
        Command ok     = new Command("Sign In", Command.OK,   1);
        Command cancel = new Command("Cancel",  Command.BACK, 2);
        form.addCommand(ok);
        form.addCommand(cancel);
        final SettingsForm self = this;
        form.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    final String user = userTf.getString().trim();
                    final String pass = passTf.getString();
                    if (user.length() == 0 || pass.length() == 0) {
                        statusItem.setText("Please enter username and password.");
                        return;
                    }
                    statusItem.setText("Signing in...");
                    new Thread(new Runnable() {
                        public void run() {
                            try {
                                LastFmHelper lfmHelper = new LastFmHelper();
                                String sk = lfmHelper.getMobileSession(user, pass);
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
                                    public void run() {
                                        statusItem.setText("Error: " + msg);
                                    }
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
        Settings.lastFmUser = "";
        Settings.lastFmSk   = "";
        Settings.save();
        midlet.onLastFmSignedOut();
        repaint();
    }

    // -------------------------------------------------------------------------
    // Visualizer
    // -------------------------------------------------------------------------

    private void openVisualizer() {
        VisualizerCanvas viz = new VisualizerCanvas(display, this);
        if (pm != null) {
            viz.setTrackInfo(pm.getCurrentName(), pm.getCurrentArtist());
        }
        display.setCurrent(viz);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            display.setCurrent(backScreen);
        } else if (c == CMD_SELECT) {
            activate(selectedIndex);
        }
    }

    // -------------------------------------------------------------------------
    // Scroll helper
    // -------------------------------------------------------------------------

    private void ensureVisible() {
        int h     = getHeight();
        int listH = h - HDR_H;
        int vis   = listH / ITEM_H;
        if (vis < 1) vis = 1;
        if (selectedIndex < scrollOffset)
            scrollOffset = selectedIndex;
        else if (selectedIndex >= scrollOffset + vis)
            scrollOffset = selectedIndex - vis + 1;
    }
}
