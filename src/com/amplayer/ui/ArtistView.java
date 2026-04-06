package com.amplayer.ui;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import com.amplayer.api.AMAPI;
import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.utils.IOUtils;
import com.amplayer.utils.Settings;
import com.amplayer.utils.SocketHttpConnection;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Full-screen artist detail view.
 *
 * Sections (loaded asynchronously):
 *   Top Songs  — tapping plays the song immediately
 *   Albums     — tapping opens a DetailView
 *   Playlists  — tapping opens a DetailView
 */
public class ArtistView extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Fonts & colors
    // -------------------------------------------------------------------------

    private static final Font TITLE_FONT   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font SECTION_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_SMALL);
    private static final Font NAME_FONT    = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUB_FONT     = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_TITLE_BG = 0x111111;
    private static final int COLOR_SEL      = 0x1C1C1E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_NAME     = 0xFFFFFF;
    private static final int COLOR_SUB      = 0x8E8E93;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;
    private static final int COLOR_HDR_BG   = 0x111111;

    private static final int PAD        = 8;
    private static final int TITLE_H    = TITLE_FONT.getHeight() + PAD * 2;
    private static final int ITEM_H     = NAME_FONT.getHeight() + SUB_FONT.getHeight() + PAD * 2;
    private static final int SECTION_H  = SECTION_FONT.getHeight() + PAD;
    private static final int ACCENT_W   = 3;

    // -------------------------------------------------------------------------
    // Commands  (used only on non-Nokia devices)
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK         = new Command("Back",         Command.BACK, 1);
    private static final Command CMD_SELECT       = new Command("Select",       Command.OK,   1);
    private static final Command CMD_PLAY_NEXT    = new Command("Play Next",    Command.ITEM, 2);
    private static final Command CMD_ADD_TO_QUEUE = new Command("Add to Queue", Command.ITEM, 3);

    // -------------------------------------------------------------------------
    // Nokia soft-key menu
    // -------------------------------------------------------------------------

    private static final String[] NOKIA_MENU_ITEMS = { "Select", "Play Next", "Add to Queue" };
    private final boolean isNokia;
    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;

    // -------------------------------------------------------------------------
    // Identity
    // -------------------------------------------------------------------------

    private final String           artistId;
    private final String           artistName;
    private final AMAPI            api;
    private final String           storefront;
    private final String           storefrontLanguage;
    private final Display          display;
    private final Displayable      backScreen;
    private final AppleMusicMIDlet midlet;

    // Artist artwork (small, shown in title bar)
    private Image   artistImage     = null;
    private String  artUrlTemplate;   // may be null; resolved during loadContent if absent

    // -------------------------------------------------------------------------
    // Flat item list (sections + entries)
    // -------------------------------------------------------------------------

    /** Row types: "header", "song", "album", "playlist" */
    private String[] rowTypes;
    private String[] rowNames;
    private String[] rowSubs;    // artist name (songs/albums) or curator
    private String[] rowIds;     // resource ID (blank for headers)
    private String[] rowArtUrls;
    private int      rowCount = 0;

    /** Parallel arrays for songs — used to build a play queue for top songs */
    private final Vector songItems = new Vector(); // JSONObject{id,name,artist,art}

    private boolean loaded  = false;
    private String  errorMsg = null;

    // -------------------------------------------------------------------------
    // Scroll state
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;
    private int scrollPx      = 0;

    // ── Marquee (selected row only) ──────────────────────────────────────────
    private static final int MQ_SPEED = 2;
    private static final int MQ_PAUSE = 20;
    private int  mqOffset  = 0;
    private int  mqPause   = MQ_PAUSE;
    private int  mqMaxOvf  = 0;
    private volatile boolean mqRunning = false;

    // -------------------------------------------------------------------------
    // Per-row heights and cumulative y
    // -------------------------------------------------------------------------

    private int[] rowHeights;
    private int[] rowYPos;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public ArtistView(String artistId, String artistName, String artUrlTemplate,
                      AMAPI api, String storefront,
                      Display display, Displayable backScreen,
                      AppleMusicMIDlet midlet) {
        super();
        this.artistId           = artistId;
        this.artistName         = artistName;
        this.artUrlTemplate     = (artUrlTemplate != null) ? artUrlTemplate : "";
        this.api                = api;
        this.storefront         = storefront;
        this.storefrontLanguage = api.getStorefrontLanguage();
        this.display            = display;
        this.backScreen         = backScreen;
        this.midlet             = midlet;

        isNokia = Settings.getDeviceEnvironment().indexOf("nokia") >= 0;
        if (!isNokia) {
            addCommand(CMD_BACK);
            addCommand(CMD_SELECT);
            addCommand(CMD_PLAY_NEXT);
            addCommand(CMD_ADD_TO_QUEUE);
            setCommandListener(this);
            setFullScreenMode(false);
        } else {
            setFullScreenMode(true);
        }

        loadContent();
    }

    // -------------------------------------------------------------------------
    // Data loading
    // -------------------------------------------------------------------------

    private void loadContent() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Vector rows = new Vector();
                    Hashtable params = new Hashtable();
                    params.put("l",     storefrontLanguage);
                    params.put("limit", "20");

                    // If no art URL was passed in, fetch the artist object to get it
                    if (artUrlTemplate.length() == 0) {
                        try {
                            JSONObject artistResp = api.APIRequest(
                                "/v1/catalog/" + storefront + "/artists/" + artistId,
                                null, "GET", null, null);
                            JSONArray d = artistResp.getArray("data", null);
                            if (d != null && d.size() > 0) {
                                JSONObject attrs = d.getObject(0).getObject("attributes", null);
                                if (attrs != null) artUrlTemplate = extractArtUrl(attrs);
                            }
                        } catch (Exception ignored) {}
                    }

                    // Download the artist image
                    if (artUrlTemplate.length() > 0) {
                        try {
                            int imgSize = TITLE_H - PAD * 2;
                            String s   = String.valueOf(imgSize);
                            String url = strReplace(artUrlTemplate, "{w}", s);
                                   url = strReplace(url, "{h}", s);
                                   url = strReplace(url, "{f}", "jpg");
                                   url = strReplace(url, "https://", "http://");
                            SocketHttpConnection conn = SocketHttpConnection.open(url);
                            conn.setRequestMethod("GET");
                            InputStream in = conn.openInputStream();
                            try {
                                byte[] imgData = IOUtils.readAll(in);
                                Image raw = Image.createImage(imgData, 0, imgData.length);
                                artistImage = scaleImage(raw, imgSize, imgSize);
                            } finally {
                                IOUtils.closeQuietly(in);
                                try { conn.close(); } catch (Exception ignored) {}
                            }
                        } catch (Exception ignored) {}
                    }

                    // Top Songs
                    try {
                        JSONObject resp = api.APIRequest(
                            "/v1/catalog/" + storefront + "/artists/" + artistId + "/view/top-songs",
                            params, "GET", null, null);
                        JSONArray data = resp.getArray("data", null);
                        if (data != null && data.size() > 0) {
                            addHeader(rows, "Top Songs");
                            for (int i = 0; i < data.size(); i++) {
                                JSONObject song  = data.getObject(i);
                                JSONObject attrs = song.getObject("attributes", null);
                                if (attrs == null) continue;
                                String id  = song.getString("id", "");
                                String nm  = attrs.getString("name",       "Unknown");
                                String art = extractArtUrl(attrs);
                                addRow(rows, "song", nm, artistName, id, art);
                                JSONObject si = new JSONObject();
                                si.put("id",     id);
                                si.put("name",   nm);
                                si.put("artist", artistName);
                                si.put("art",    art);
                                songItems.addElement(si);
                            }
                        }
                    } catch (Exception ignored) {}

                    // Albums
                    try {
                        JSONObject resp = api.APIRequest(
                            "/v1/catalog/" + storefront + "/artists/" + artistId + "/albums",
                            params, "GET", null, null);
                        JSONArray data = resp.getArray("data", null);
                        if (data != null && data.size() > 0) {
                            addHeader(rows, "Albums");
                            for (int i = 0; i < data.size(); i++) {
                                JSONObject album = data.getObject(i);
                                JSONObject attrs = album.getObject("attributes", null);
                                if (attrs == null) continue;
                                addRow(rows, "album",
                                    attrs.getString("name", "Unknown"),
                                    artistName,
                                    album.getString("id", ""),
                                    extractArtUrl(attrs));
                            }
                        }
                    } catch (Exception ignored) {}

                    // Featured Playlists
                    try {
                        JSONObject resp = api.APIRequest(
                            "/v1/catalog/" + storefront + "/artists/" + artistId + "/view/featured-playlists",
                            params, "GET", null, null);
                        JSONArray data = resp.getArray("data", null);
                        if (data != null && data.size() > 0) {
                            addHeader(rows, "Playlists");
                            for (int i = 0; i < data.size(); i++) {
                                JSONObject pl    = data.getObject(i);
                                JSONObject attrs = pl.getObject("attributes", null);
                                if (attrs == null) continue;
                                String sub = attrs.getString("curatorName", null);
                                if (sub == null || sub.length() == 0)
                                    sub = attrs.getString("description", "");
                                addRow(rows, "playlist",
                                    attrs.getString("name", "Unknown"),
                                    sub,
                                    pl.getString("id", ""),
                                    extractArtUrl(attrs));
                            }
                        }
                    } catch (Exception ignored) {}

                    buildArrays(rows);
                    loaded = true;
                } catch (Exception e) {
                    errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
                }
                repaint();
            }
        }).start();
    }

    private static void addHeader(Vector rows, String title) {
        String[] r = new String[5];
        r[0] = "header"; r[1] = title; r[2] = ""; r[3] = ""; r[4] = "";
        rows.addElement(r);
    }

    private static void addRow(Vector rows, String type,
                               String name, String sub, String id, String art) {
        String[] r = new String[5];
        r[0] = type; r[1] = name; r[2] = sub; r[3] = id; r[4] = art;
        rows.addElement(r);
    }

    private void buildArrays(Vector rows) {
        rowCount   = rows.size();
        rowTypes   = new String[rowCount];
        rowNames   = new String[rowCount];
        rowSubs    = new String[rowCount];
        rowIds     = new String[rowCount];
        rowArtUrls = new String[rowCount];
        rowHeights = new int[rowCount];
        rowYPos    = new int[rowCount + 1];
        rowYPos[0] = 0;
        for (int i = 0; i < rowCount; i++) {
            String[] r = (String[]) rows.elementAt(i);
            rowTypes[i]   = r[0];
            rowNames[i]   = r[1];
            rowSubs[i]    = r[2];
            rowIds[i]     = r[3];
            rowArtUrls[i] = r[4];
            rowHeights[i] = "header".equals(r[0]) ? SECTION_H : ITEM_H;
            rowYPos[i + 1] = rowYPos[i] + rowHeights[i];
        }
        // Skip leading headers
        while (selectedIndex < rowCount - 1 && "header".equals(rowTypes[selectedIndex]))
            selectedIndex++;
    }

    private static String extractArtUrl(JSONObject attrs) {
        JSONObject art = attrs.getObject("artwork", null);
        if (art == null) return "";
        return art.getString("url", "");
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w     = getWidth();
        int h     = getHeight();
        int skH   = isNokia ? SUB_FONT.getHeight() + PAD * 2 : 0;
        int listH = h - TITLE_H - skH;

        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Title bar
        g.setColor(COLOR_TITLE_BG);
        g.fillRect(0, 0, w, TITLE_H);

        int imgSize  = TITLE_H - PAD * 2;
        int nameX    = PAD;
        if (artistImage != null) {
            g.drawImage(artistImage, PAD, PAD, Graphics.LEFT | Graphics.TOP);
            nameX = PAD + imgSize + PAD;
        }
        g.setFont(TITLE_FONT);
        g.setColor(COLOR_NAME);
        g.drawString(artistName, nameX, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, TITLE_H - 2, w, 2);

        if (!loaded && errorMsg == null) {
            g.setFont(SUB_FONT);
            g.setColor(COLOR_SUB);
            g.drawString("Loading...", w / 2, TITLE_H + listH / 2,
                         Graphics.HCENTER | Graphics.BASELINE);
            if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH); }
            return;
        }
        if (errorMsg != null) {
            g.setFont(SUB_FONT);
            g.setColor(0xFF3B30);
            g.drawString(errorMsg, PAD, TITLE_H + PAD, Graphics.LEFT | Graphics.TOP);
            if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH); }
            return;
        }
        if (rowCount == 0) {
            g.setFont(SUB_FONT);
            g.setColor(COLOR_SUB);
            g.drawString("No content found.", w / 2, TITLE_H + listH / 2,
                         Graphics.HCENTER | Graphics.BASELINE);
            if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH); }
            return;
        }

        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, TITLE_H, w, listH);

        for (int i = 0; i < rowCount; i++) {
            int absY = rowYPos[i];
            int y    = TITLE_H + absY - scrollPx;
            int ih   = rowHeights[i];

            if (y + ih <= TITLE_H) continue;
            if (y >= h)            break;

            if ("header".equals(rowTypes[i])) {
                g.setColor(COLOR_HDR_BG);
                g.fillRect(0, y, w, ih);
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y + ih - 1, w, 1);
                g.setFont(SECTION_FONT);
                g.setColor(COLOR_ACCENT);
                g.drawString(rowNames[i].toUpperCase(), PAD, y + PAD / 2,
                             Graphics.LEFT | Graphics.TOP);
            } else {
                boolean sel = (i == selectedIndex);
                if (sel) {
                    g.setColor(COLOR_SEL);
                    g.fillRect(0, y, w, ih);
                    g.setColor(COLOR_ACCENT);
                    g.fillRect(0, y, ACCENT_W, ih);
                }
                int textX  = PAD + ACCENT_W + 4;
                int availW = w - textX - PAD - 3;

                if (sel) {
                    int ovf = NAME_FONT.stringWidth(rowNames[i]) - availW;
                    if (rowSubs[i] != null && rowSubs[i].length() > 0)
                        ovf = Math.max(ovf, SUB_FONT.stringWidth(rowSubs[i]) - availW);
                    mqMaxOvf = ovf > 0 ? ovf + 16 : 0;
                }

                g.setFont(NAME_FONT);
                g.setColor(COLOR_NAME);
                if (sel && NAME_FONT.stringWidth(rowNames[i]) > availW) {
                    int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                    g.setClip(textX, y + PAD, availW, NAME_FONT.getHeight());
                    g.drawString(rowNames[i], textX - mqOffset, y + PAD, Graphics.LEFT | Graphics.TOP);
                    g.setClip(scx, scy, scw, sch);
                } else {
                    g.drawString(clip(rowNames[i], NAME_FONT, availW), textX, y + PAD,
                                 Graphics.LEFT | Graphics.TOP);
                }

                if (rowSubs[i] != null && rowSubs[i].length() > 0) {
                    g.setFont(SUB_FONT);
                    g.setColor(COLOR_SUB);
                    int subY = y + PAD + NAME_FONT.getHeight();
                    if (sel && SUB_FONT.stringWidth(rowSubs[i]) > availW) {
                        int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                        g.setClip(textX, subY, availW, SUB_FONT.getHeight());
                        g.drawString(rowSubs[i], textX - mqOffset, subY, Graphics.LEFT | Graphics.TOP);
                        g.setClip(scx, scy, scw, sch);
                    } else {
                        g.drawString(clip(rowSubs[i], SUB_FONT, availW), textX, subY,
                                     Graphics.LEFT | Graphics.TOP);
                    }
                }
                g.setColor(COLOR_DIVIDER);
                g.drawLine(PAD, y + ih - 1, w - PAD, y + ih - 1);
            }
        }

        // Scroll bar
        int totalH = rowYPos[rowCount];
        if (totalH > listH) {
            int barH = Math.max(8, listH * listH / totalH);
            int barY = TITLE_H + (listH - barH) * scrollPx / Math.max(1, totalH - listH);
            g.setColor(0x3A3A3C);
            g.fillRect(w - 3, TITLE_H, 3, listH);
            g.setColor(COLOR_ACCENT);
            g.fillRect(w - 3, barY, 3, barH);
        }

        g.setClip(cx, cy, cw, ch);
        if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH); }
    }

    // -------------------------------------------------------------------------
    // Key input
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        if (isNokia) {
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
                    display.setCurrent(backScreen);
                }
                return;
            }
            if (nokiaMenuOpen) {
                int action = getGameAction(keyCode);
                if (action == UP && nokiaMenuSel > 0) {
                    nokiaMenuSel--;
                    repaint();
                } else if (action == DOWN && nokiaMenuSel < NOKIA_MENU_ITEMS.length - 1) {
                    nokiaMenuSel++;
                    repaint();
                } else if (action == FIRE || keyCode == -5) {
                    nokiaMenuOpen = false;
                    executeNokiaMenuItem(nokiaMenuSel);
                    repaint();
                }
                return;
            }
        }
        if (!loaded || rowCount == 0) return;
        int action = getGameAction(keyCode);
        if (action == UP) {
            moveUp();
        } else if (action == DOWN) {
            moveDown();
        } else if (action == FIRE || keyCode == -5) {
            fireSelection();
        }
    }

    private void executeNokiaMenuItem(int index) {
        switch (index) {
            case 0: fireSelection();       break;
            case 1: queueSelected(true);   break;
            case 2: queueSelected(false);  break;
        }
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_TITLE_BG);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUB_FONT);
        int labelY = barY + (skH - SUB_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(COLOR_NAME);
            g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_SUB);
            g.drawString("Close", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(COLOR_NAME);
            g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_SUB);
            g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h, int skH) {
        int itemH  = SUB_FONT.getHeight() + 6;
        int menuH  = itemH * NOKIA_MENU_ITEMS.length + PAD * 2;
        int menuY  = h - skH - menuH;

        g.setColor(COLOR_TITLE_BG);
        g.fillRect(0, menuY, w, menuH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, menuY, w, menuY);

        for (int i = 0; i < NOKIA_MENU_ITEMS.length; i++) {
            int y = menuY + PAD + i * itemH;
            if (i == nokiaMenuSel) {
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y - 2, w, itemH);
                g.setColor(COLOR_BG);
            } else {
                g.setColor(COLOR_NAME);
            }
            g.setFont(SUB_FONT);
            g.drawString(NOKIA_MENU_ITEMS[i], PAD, y, Graphics.LEFT | Graphics.TOP);
        }
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

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

    private void moveUp() {
        int prev = selectedIndex - 1;
        while (prev >= 0 && "header".equals(rowTypes[prev])) prev--;
        if (prev >= 0) { selectedIndex = prev; mqReset(); ensureVisible(); repaint(); }
    }

    private void moveDown() {
        int next = selectedIndex + 1;
        while (next < rowCount && "header".equals(rowTypes[next])) next++;
        if (next < rowCount) { selectedIndex = next; mqReset(); ensureVisible(); repaint(); }
    }

    private void ensureVisible() {
        int skH   = isNokia ? SUB_FONT.getHeight() + PAD * 2 : 0;
        int listH = getHeight() - TITLE_H - skH;
        int absY  = rowYPos[selectedIndex];
        int ih    = rowHeights[selectedIndex];
        if (absY < scrollPx)                   scrollPx = absY;
        else if (absY + ih > scrollPx + listH) scrollPx = absY + ih - listH;
    }
    
    private static String strReplace(String src, String from, String to) {
        int i = src.indexOf(from);
        if (i == -1) return src;
        return src.substring(0, i) + to + src.substring(i + from.length());
    }

    private void fireSelection() {
        if (selectedIndex < 0 || selectedIndex >= rowCount) return;
        String type = rowTypes[selectedIndex];
        String id   = rowIds[selectedIndex];
        String nm   = rowNames[selectedIndex];
        String sub  = rowSubs[selectedIndex];
        String art  = rowArtUrls[selectedIndex];

        if ("song".equals(type)) {
            // Find in songItems and queue all top songs starting from selected
            int n = songItems.size();
            int startIndex = 0;
            String[] ids     = new String[n];
            String[] names   = new String[n];
            String[] artists = new String[n];
            for (int i = 0; i < n; i++) {
                JSONObject si = (JSONObject) songItems.elementAt(i);
                ids[i]     = si.getString("id",     "");
                names[i]   = si.getString("name",   "");
                artists[i] = si.getString("artist", "");
                if (ids[i].equals(id)) startIndex = i;
            }
            midlet.playQueue(ids, names, artists, art, startIndex);
        } else if ("album".equals(type)) {
            DetailView dv = new DetailView(
                id, false, false, nm, sub, art,
                api, storefront, display, this, midlet);
            display.setCurrent(dv);
        } else if ("playlist".equals(type)) {
            DetailView dv = new DetailView(
                id, true, false, nm, sub, art,
                api, storefront, display, this, midlet);
            display.setCurrent(dv);
        }
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            display.setCurrent(backScreen);
        } else if (c == CMD_SELECT) {
            fireSelection();
        } else if (c == CMD_PLAY_NEXT) {
            queueSelected(true);
        } else if (c == CMD_ADD_TO_QUEUE) {
            queueSelected(false);
        }
    }

    /**
     * Add the currently highlighted item to the playback queue.
     * Songs are added immediately; albums and playlists are fetched
     * asynchronously then added in bulk.
     */
    private void queueSelected(final boolean playNext) {
        if (!loaded || selectedIndex < 0 || selectedIndex >= rowCount) return;
        if ("header".equals(rowTypes[selectedIndex])) return;

        final com.amplayer.playback.PlaybackManager pm = midlet.getPlaybackManager();
        if (pm == null) return;

        final String type = rowTypes[selectedIndex];
        final String id   = rowIds[selectedIndex];

        if ("song".equals(type)) {
            // Find in songItems
            for (int i = 0; i < songItems.size(); i++) {
                JSONObject si = (JSONObject) songItems.elementAt(i);
                if (id.equals(si.getString("id", ""))) {
                    String[] ids     = { si.getString("id",     "") };
                    String[] names   = { si.getString("name",   "") };
                    String[] artists = { si.getString("artist", "") };
                    if (playNext) pm.insertNext(ids, names, artists);
                    else          pm.appendToQueue(ids, names, artists);
                    return;
                }
            }
        } else {
            // Album or playlist — fetch tracks asynchronously
            final boolean isPlaylist = "playlist".equals(type);
            new Thread(new Runnable() {
                public void run() {
                    try {
                        String resType = isPlaylist ? "playlists" : "albums";
                        String endpoint = "/v1/catalog/" + storefront + "/" + resType + "/" + id;
                        Hashtable params = new Hashtable();
                        params.put("include", "tracks");
                        params.put("l",       storefrontLanguage);
                        JSONObject resp = api.APIRequest(endpoint, params, "GET", null, null);
                        JSONArray data = resp.getArray("data", null);
                        if (data == null || data.size() == 0) return;
                        JSONObject rels = data.getObject(0).getObject("relationships", null);
                        if (rels == null) return;
                        JSONObject tracksRel = rels.getObject("tracks", null);
                        if (tracksRel == null) return;
                        JSONArray tracks = tracksRel.getArray("data", null);
                        if (tracks == null || tracks.size() == 0) return;

                        int n = tracks.size();
                        String[] ids     = new String[n];
                        String[] names   = new String[n];
                        String[] artists = new String[n];
                        for (int i = 0; i < n; i++) {
                            JSONObject track = tracks.getObject(i);
                            JSONObject attrs = track.getObject("attributes", null);
                            ids[i]     = track.getString("id", "");
                            names[i]   = attrs != null ? attrs.getString("name",       "") : "";
                            artists[i] = attrs != null ? attrs.getString("artistName", "") : "";
                        }
                        if (playNext) pm.insertNext(ids, names, artists);
                        else          pm.appendToQueue(ids, names, artists);
                    } catch (Exception ignored) {}
                }
            }).start();
        }
    }

    // -------------------------------------------------------------------------
    // Image helpers
    // -------------------------------------------------------------------------

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private static Image scaleImage(Image src, int targetW, int targetH) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        if (srcW == targetW && srcH == targetH) return src;
        int[] srcRow = new int[srcW];
        int[] dstPx  = new int[targetW * targetH];
        int   prevSy = -1;
        for (int y = 0; y < targetH; y++) {
            int sy = y * srcH / targetH;
            if (sy != prevSy) {
                src.getRGB(srcRow, 0, srcW, 0, sy, srcW, 1);
                prevSy = sy;
            }
            int off = y * targetW;
            for (int x = 0; x < targetW; x++)
                dstPx[off + x] = srcRow[x * srcW / targetW];
        }
        return Image.createRGBImage(dstPx, targetW, targetH, false);
    }
}
