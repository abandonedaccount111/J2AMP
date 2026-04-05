package com.amplayer.ui;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import com.amplayer.api.AMAPI;
import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.IOUtils;
import com.amplayer.utils.Settings;
import com.amplayer.utils.SocketHttpConnection;
import java.io.InputStream;
import java.util.Hashtable;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Full-screen detail view for an album or playlist.
 *
 * Layout:
 *   Top 1/3  — artwork (async, loaded AFTER tracks to avoid KErrServerBusy -16)
 *              + name / artist text
 *   Bottom 2/3 — scrollable track list (clipped so it cannot overdraw the header)
 *       Albums:    track number + name
 *       Playlists: track number + name + artist
 *
 * Colors are sourced from attributes.artwork in the API response:
 *   bgColor     → background
 *   textColor1  → primary text + accent / tint
 *   textColor2  → secondary text (subname / artist header)
 *   textColor3  → track names
 *   textColor4  → track artist names (playlists)
 */
public class DetailView extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Fonts
    // -------------------------------------------------------------------------

    private static final Font HEADER_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font NAME_FONT    = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUBNAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD            = 8;
    private static final int COLOR_ERROR    = 0xFF3B30;

    // -------------------------------------------------------------------------
    // Dynamic colors  (defaults until API response arrives)
    // -------------------------------------------------------------------------

    private int colorBg    = 0x000000;   // bgColor
    private int colorText1 = 0xFFFFFF;   // textColor1 — primary text + accent/tint
    private int colorText2 = 0x8E8E93;   // textColor2 — subname / artist header
    private int colorText3 = 0xDDDDDD;   // textColor3 — track names
    private int colorText4 = 0x8E8E93;   // textColor4 — track artists (playlists)

    // -------------------------------------------------------------------------
    // Commands  (used only on non-Nokia devices)
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK           = new Command("Back",           Command.BACK, 1);
    private static final Command CMD_PLAY           = new Command("Play",           Command.OK,   1);
    private static final Command CMD_GO_TO_ARTIST   = new Command("Go to Artist",   Command.ITEM, 2);
    private static final Command CMD_GO_TO_ALBUM    = new Command("Go to Album",    Command.ITEM, 3);
    private static final Command CMD_PLAY_NEXT      = new Command("Play Next",      Command.ITEM, 4);
    private static final Command CMD_ADD_TO_QUEUE   = new Command("Add to Queue",   Command.ITEM, 5);

    // -------------------------------------------------------------------------
    // Nokia soft-key menu
    // -------------------------------------------------------------------------

    private final boolean isNokia;
    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;
    // Built in constructor based on isPlaylist
    private final String[] nokiaMenuItems;

    // -------------------------------------------------------------------------
    // Immutable identity
    // -------------------------------------------------------------------------

    private final String             id;
    private final boolean            isPlaylist;
    private final boolean            isLibrary;       // true → use /v1/me/library/ endpoint
    private final String             name;
    private final String             subname;
    private final String             artUrlTemplate;  // may contain {w}, {h}, {f}
    private final AMAPI              api;
    private final String             storefront;
    private final String             storefrontLanguage;
    private final Display            display;
    private final Displayable        backScreen;
    private final AppleMusicMIDlet   midlet;          // null if opened without midlet context

    // -------------------------------------------------------------------------
    // Async state
    // -------------------------------------------------------------------------

    private Image   artImage;
    private boolean artLoadStarted = false;

    private String[] trackIds;
    private String[] trackNames;
    private String[] trackArtists;
    private int      trackCount   = 0;
    private boolean  tracksLoaded = false;
    private String   errorMsg     = null;

    /** Artist ID extracted from album relationship — only set for non-playlist views. */
    private String   albumArtistId = null;

    // -------------------------------------------------------------------------
    // Scroll state
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;
    private int scrollOffset  = 0;

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

    public DetailView(String id, boolean isPlaylist, boolean isLibrary,
                      String name, String subname,
                      String artUrlTemplate, AMAPI api, String storefront,
                      Display display, Displayable backScreen,
                      AppleMusicMIDlet midlet) {
        super();
        this.id             = id;
        this.isPlaylist     = isPlaylist;
        this.isLibrary      = isLibrary;
        this.name           = name;
        this.subname        = subname;
        this.artUrlTemplate = artUrlTemplate;
        this.api            = api;
        this.storefront     = storefront;
        this.storefrontLanguage = api.getStorefrontLanguage();
        this.display        = display;
        this.backScreen     = backScreen;
        this.midlet         = midlet;

        isNokia = Settings.getDeviceEnvironment().indexOf("nokia") >= 0;
        if (isPlaylist) {
            nokiaMenuItems = new String[]{ "Play", "Go to Artist", "Go to Album", "Play Next", "Add to Queue" };
        } else {
            nokiaMenuItems = new String[]{ "Play", "Go to Artist", "Play Next", "Add to Queue" };
        }
        if (!isNokia) {
            addCommand(CMD_BACK);
            addCommand(CMD_PLAY);
            addCommand(CMD_GO_TO_ARTIST);
            if (isPlaylist) addCommand(CMD_GO_TO_ALBUM);
            addCommand(CMD_PLAY_NEXT);
            addCommand(CMD_ADD_TO_QUEUE);
            setCommandListener(this);
            setFullScreenMode(false);
        } else {
            setFullScreenMode(true);
        } 

        loadTracks();
    }

    // -------------------------------------------------------------------------
    // Track + color loading
    // -------------------------------------------------------------------------

    private void loadTracks() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String type     = isPlaylist ? "playlists" : "albums";
                    String endpoint = isLibrary
                        ? "/v1/me/library/" + type + "/" + id
                        : "/v1/catalog/" + storefront + "/" + type + "/" + id;

                    Hashtable params = new Hashtable();
                    // For catalog albums also include the artists relationship so we
                    // can navigate to the artist page without an extra round-trip.
                    String include = "tracks";
                    if (!isPlaylist && !isLibrary) include += ",artists";
                    params.put("include", include);
                    params.put("l",       storefrontLanguage);

                    JSONObject resp = api.APIRequest(endpoint, params, "GET", null, null);
                    parseTracks(resp);
                    tracksLoaded = true;
                } catch (Exception e) {
                    errorMsg = e.getMessage() != null ? e.getMessage() : e.toString();
                }
                repaint();

                // Load artwork only AFTER tracks are done to avoid KErrServerBusy -16
                if (tracksLoaded && !artLoadStarted) {
                    artLoadStarted = true;
                    int h    = getHeight();
                    int artH = (h > PAD * 4) ? h / 3 : 100;
                    loadArtwork(artH - PAD * 2);
                }
            }
        }).start();
    }

    private void parseTracks(JSONObject resp) {
        JSONArray data = resp.getArray("data", null);
        if (data == null || data.size() == 0) { trackCount = 0; return; }

        JSONObject resource = data.getObject(0);

        // — Extract artwork colors from attributes.artwork —
        JSONObject attrs = resource.getObject("attributes", null);
        if (attrs != null) {
            JSONObject art = attrs.getObject("artwork", null);
            if (art != null) {
                colorBg    = parseColor(art.getString("bgColor",    null), colorBg);
                colorText1 = parseColor(art.getString("textColor1", null), colorText1);
                colorText2 = parseColor(art.getString("textColor2", null), colorText2);
                colorText3 = parseColor(art.getString("textColor3", null), colorText3);
                colorText4 = parseColor(art.getString("textColor4", null), colorText4);
            }
        }

        // — Extract artist ID (catalog albums only) —
        JSONObject rels      = resource.getObject("relationships", null);
        if (!isPlaylist && !isLibrary && rels != null) {
            JSONObject artistsRel = rels.getObject("artists", null);
            if (artistsRel != null) {
                JSONArray artistData = artistsRel.getArray("data", null);
                if (artistData != null && artistData.size() > 0) {
                    albumArtistId = artistData.getObject(0).getString("id", "");
                }
            }
        }

        // — Extract tracks —
        if (rels == null) { trackCount = 0; return; }
        JSONObject tracksRel = rels.getObject("tracks", null);
        if (tracksRel == null) { trackCount = 0; return; }
        JSONArray  tracks    = tracksRel.getArray("data", null);
        if (tracks == null)  { trackCount = 0; return; }

        trackCount   = tracks.size();
        trackIds     = new String[trackCount];
        trackNames   = new String[trackCount];
        trackArtists = new String[trackCount];

        for (int i = 0; i < trackCount; i++) {
            JSONObject track      = tracks.getObject(i);
            String     rawId      = track.getString("id", "");
            JSONObject trackAttrs = track.getObject("attributes", null);

            if (trackAttrs != null) {
                // For library tracks the resource ID is an opaque library ID.
                // playParams contains the salable catalog Adam ID needed for playback.
                if (isLibrary) {
                    JSONObject pp = trackAttrs.getObject("playParams", null);
                    if (pp != null) {
                        String catalogId = pp.getString("catalogId", "");
                        if (catalogId.length() > 0) {
                            rawId = catalogId;
                        } else {
                            String ppId = pp.getString("id", "");
                            // library IDs start with "i." — skip those
                            if (ppId.length() > 0 && !ppId.startsWith("i.")) rawId = ppId;
                        }
                    }
                }
                trackNames[i]   = trackAttrs.getString("name",       "Unknown");
                trackArtists[i] = trackAttrs.getString("artistName", "");
            } else {
                trackNames[i]   = "Unknown";
                trackArtists[i] = "";
            }
            trackIds[i] = rawId;
        }
    }

    // -------------------------------------------------------------------------
    // Artwork loading  (called from track thread after tracks finish)
    // -------------------------------------------------------------------------

    private void loadArtwork(final int size) {
        if (artUrlTemplate == null || artUrlTemplate.length() == 0) return;
        new Thread(new Runnable() {
            public void run() {
                SocketHttpConnection conn = null;
                InputStream          in   = null;
                try {
                    String s   = String.valueOf(size);
                    String url = strReplace(artUrlTemplate, "{w}", s);
                    url        = strReplace(url,            "{h}", s);
                    url        = strReplace(url,            "{f}", "jpg");

                    // Repplace the dimension from /image/thumb/gen/<aaa>x<bbb> to /image/thumb/gen/{s}x{s}
                    url = strReplace(url, "/image/thumb/gen/600x600", "/image/thumb/gen/" + s + "x" + s);

                    url = strReplace(url, "https://", "http://");

                    conn = SocketHttpConnection.open(url);
                    conn.setRequestMethod("GET");
                    in   = conn.openInputStream();
                    byte[] imgData = IOUtils.readAll(in);
                    Image raw = Image.createImage(imgData, 0, imgData.length);
                    artImage  = scaleImage(raw, size, size);
                } catch (Exception ignored) {
                    // leave artImage null — placeholder draws instead
                } finally {
                    IOUtils.closeQuietly(in);
                    try { if (conn != null) conn.close(); } catch (Exception ignored) {}
                }
                repaint();
            }
        }).start();
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w     = getWidth();
        int h     = getHeight();
        int artH  = h / 3;
        int listY = artH + 2;
        int skH   = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
        int listH = h - listY - skH;

        // Full background in bgColor
        g.setColor(colorBg);
        g.fillRect(0, 0, w, h);

        paintArtSection(g, w, artH);

        // Tint line using textColor1 as accent
        g.setColor(colorText1);
        g.fillRect(0, artH, w, 2);

        if (errorMsg != null) {
            g.setFont(SUBNAME_FONT);
            g.setColor(COLOR_ERROR);
            g.drawString("Error: " + errorMsg, PAD, listY + PAD, Graphics.LEFT | Graphics.TOP);
        } else if (!tracksLoaded) {
            g.setFont(SUBNAME_FONT);
            g.setColor(colorText2);
            g.drawString("Loading tracks...", w / 2, listY + listH / 2,
                         Graphics.HCENTER | Graphics.BASELINE);
        } else {
            paintTrackList(g, w, listY, listH);
        }
        if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h); }
    }

    private void paintArtSection(Graphics g, int w, int artH) {
        // Art section background = slightly offset shade of bgColor
        g.setColor(shiftBrightness(colorBg, 20));
        g.fillRect(0, 0, w, artH);

        int imgSize = artH - PAD * 2;

        if (artImage != null) {
            g.drawImage(artImage, PAD, PAD, Graphics.LEFT | Graphics.TOP);
        } else {
            // Placeholder box
            g.setColor(shiftBrightness(colorBg, 40));
            g.fillRoundRect(PAD, PAD, imgSize, imgSize, 10, 10);
            g.setColor(colorText2);
            g.setFont(HEADER_FONT);
            g.drawString("?", PAD + imgSize / 2, PAD + imgSize / 2,
                         Graphics.HCENTER | Graphics.BASELINE);
        }

        // Name + subname to the right of art
        int textX = PAD + imgSize + PAD;
        int textW = w - textX - PAD;

        g.setFont(HEADER_FONT);
        g.setColor(colorText1);
        g.drawString(clip(name, HEADER_FONT, textW), textX, PAD,
                     Graphics.LEFT | Graphics.TOP);

        g.setFont(NAME_FONT);
        g.setColor(colorText2);
        g.drawString(clip(subname, NAME_FONT, textW),
                     textX, PAD + HEADER_FONT.getHeight() + 4,
                     Graphics.LEFT | Graphics.TOP);

        if (tracksLoaded) {
            g.setFont(SUBNAME_FONT);
            g.setColor(colorText1);   // tint color for metadata
            g.drawString(trackCount + " tracks",
                         textX, PAD + HEADER_FONT.getHeight() + NAME_FONT.getHeight() + 8,
                         Graphics.LEFT | Graphics.TOP);
        }
    }

    private void paintTrackList(Graphics g, int w, int listY, int listH) {
        // Clip so items cannot overdraw the album art / header above listY
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, listY, w, listH);

        int itemH   = trackItemH();
        int visible = listH / itemH + 1;
        int end     = Math.min(trackCount, scrollOffset + visible);
        int numW    = 30;

        int textX  = numW + 4;
        int availW = w - textX - PAD - 3; // -3 for scroll bar

        for (int i = scrollOffset; i < end; i++) {
            int     y   = listY + (i - scrollOffset) * itemH;
            boolean sel = (i == selectedIndex);

            if (sel) {
                g.setColor(selectionColor());
                g.fillRect(0, y, w, itemH);
                g.setColor(colorText1);
                g.fillRect(0, y, 3, itemH);

                // Update marquee bounds for this row
                int ovf = NAME_FONT.stringWidth(trackNames[i]) - availW;
                if (isPlaylist && trackArtists[i] != null)
                    ovf = Math.max(ovf, SUBNAME_FONT.stringWidth(trackArtists[i]) - availW);
                mqMaxOvf = ovf > 0 ? ovf + 16 : 0;
            }

            // Track number
            g.setFont(SUBNAME_FONT);
            g.setColor(colorText4);
            g.drawString(String.valueOf(i + 1), numW - 4, y + PAD,
                         Graphics.RIGHT | Graphics.TOP);

            // Track name
            g.setFont(NAME_FONT);
            g.setColor(colorText3);
            if (sel && NAME_FONT.stringWidth(trackNames[i]) > availW) {
                int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                g.setClip(textX, y + PAD, availW, NAME_FONT.getHeight());
                g.drawString(trackNames[i], textX - mqOffset, y + PAD, Graphics.LEFT | Graphics.TOP);
                g.setClip(scx, scy, scw, sch);
            } else {
                g.drawString(clip(trackNames[i], NAME_FONT, availW), textX, y + PAD,
                             Graphics.LEFT | Graphics.TOP);
            }

            // Artist (playlists only)
            if (isPlaylist && trackArtists[i] != null && trackArtists[i].length() > 0) {
                g.setFont(SUBNAME_FONT);
                g.setColor(colorText4);
                int subY = y + PAD + NAME_FONT.getHeight();
                if (sel && SUBNAME_FONT.stringWidth(trackArtists[i]) > availW) {
                    int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                    g.setClip(textX, subY, availW, SUBNAME_FONT.getHeight());
                    g.drawString(trackArtists[i], textX - mqOffset, subY, Graphics.LEFT | Graphics.TOP);
                    g.setClip(scx, scy, scw, sch);
                } else {
                    g.drawString(clip(trackArtists[i], SUBNAME_FONT, availW), textX, subY,
                                 Graphics.LEFT | Graphics.TOP);
                }
            }

            // Divider
            g.setColor(dividerColor());
            g.drawLine(numW, y + itemH - 1, w - PAD, y + itemH - 1);
        }

        // Scroll bar — tinted with textColor1
        if (trackCount > visible) {
            int barH = Math.max(8, listH * visible / trackCount);
            int barY = listY + (listH - barH) * scrollOffset / Math.max(1, trackCount - visible);
            g.setColor(shiftBrightness(colorBg, 30));
            g.fillRect(w - 3, listY, 3, listH);
            g.setColor(colorText1);
            g.fillRect(w - 3, barY, 3, barH);
        }

        // Restore clip
        g.setClip(cx, cy, cw, ch);
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
        }
        if (!tracksLoaded || trackCount == 0) return;
        int action = getGameAction(keyCode);
        if (action == UP) {
            if (selectedIndex > 0) {
                selectedIndex--;
                mqReset();
                if (selectedIndex < scrollOffset) scrollOffset = selectedIndex;
                repaint();
            }
        } else if (action == DOWN) {
            if (selectedIndex < trackCount - 1) {
                selectedIndex++;
                mqReset();
                int listH   = getHeight() - getHeight() / 3 - 2 - (isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0);
                int visible = listH / trackItemH() + 1;
                if (selectedIndex >= scrollOffset + visible)
                    scrollOffset = selectedIndex - visible + 1;
                repaint();
            }
        } else if (action == FIRE || keyCode == -5) {
            onTrackSelected();
        }
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

    private void onTrackSelected() {
        if (midlet == null || trackIds == null || selectedIndex < 0
                || selectedIndex >= trackCount) return;
        if (isPlaylist) {
            midlet.playQueueFromPlaylist(
                trackIds, trackNames, trackArtists, artUrlTemplate, selectedIndex,
                id, name, artUrlTemplate);
        } else {
            midlet.playQueue(trackIds, trackNames, trackArtists, artUrlTemplate, selectedIndex);
        }
    }

    private void executeNokiaMenuItem(int index) {
        // Menu order: Play, Go to Artist, [Go to Album if playlist], Play Next, Add to Queue
        if (isPlaylist) {
            switch (index) {
                case 0: onTrackSelected();          break;
                case 1: goToArtist();               break;
                case 2: goToAlbum();                break;
                case 3: queueSelectedTrack(true);   break;
                case 4: queueSelectedTrack(false);  break;
            }
        } else {
            switch (index) {
                case 0: onTrackSelected();          break;
                case 1: goToArtist();               break;
                case 2: queueSelectedTrack(true);   break;
                case 3: queueSelectedTrack(false);  break;
            }
        }
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(shiftBrightness(colorBg, isLight(colorBg) ? -40 : 40));
        g.fillRect(0, barY, w, skH);
        g.setColor(dividerColor());
        g.drawLine(0, barY, w, barY);
        g.setFont(SUBNAME_FONT);
        int labelY = barY + (skH - SUBNAME_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(colorText1);
            g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(colorText2);
            g.drawString("Close", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(colorText1);
            g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(colorText2);
            g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h) {
        int itemH  = NAME_FONT.getHeight() + 6;
        int skH    = SUBNAME_FONT.getHeight() + PAD * 2;
        int menuH  = itemH * nokiaMenuItems.length + PAD * 2;
        int menuY  = h - skH - menuH;

        g.setColor(shiftBrightness(colorBg, isLight(colorBg) ? -40 : 40));
        g.fillRect(0, menuY, w, menuH);
        g.setColor(dividerColor());
        g.drawLine(0, menuY, w, 1);

        for (int i = 0; i < nokiaMenuItems.length; i++) {
            int y = menuY + PAD + i * itemH;
            if (i == nokiaMenuSel) {
                g.setColor(colorText1);
                g.fillRect(0, y - 2, w, itemH);
                g.setColor(colorBg);
            } else {
                g.setColor(colorText3);
            }
            g.setFont(NAME_FONT);
            g.drawString(nokiaMenuItems[i], PAD, y, Graphics.LEFT | Graphics.TOP);
        }
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            display.setCurrent(backScreen);
        } else if (c == CMD_PLAY) {
            onTrackSelected();
        } else if (c == CMD_PLAY_NEXT) {
            queueSelectedTrack(true);
        } else if (c == CMD_ADD_TO_QUEUE) {
            queueSelectedTrack(false);
        } else if (c == CMD_GO_TO_ARTIST) {
            goToArtist();
        } else if (c == CMD_GO_TO_ALBUM) {
            goToAlbum();
        }
    }

    // -------------------------------------------------------------------------
    // Queue helpers
    // -------------------------------------------------------------------------

    private void queueSelectedTrack(boolean playNext) {
        if (midlet == null || trackIds == null
                || selectedIndex < 0 || selectedIndex >= trackCount) return;
        PlaybackManager pm = midlet.getPlaybackManager();
        if (pm == null) return;
        String[] ids     = new String[]{ trackIds[selectedIndex]     };
        String[] names   = new String[]{ trackNames[selectedIndex]   };
        String[] artists = new String[]{ trackArtists[selectedIndex] };
        if (playNext) pm.insertNext(ids, names, artists);
        else          pm.appendToQueue(ids, names, artists);
    }

    // -------------------------------------------------------------------------
    // Go-To helpers
    // -------------------------------------------------------------------------

    /** Go to the artist of the currently highlighted track (or album artist). */
    private void goToArtist() {
        if (midlet == null) return;
        if (!isPlaylist) {
            // Album view — we may already have the artist ID from the album's relationships
            if (albumArtistId != null && albumArtistId.length() > 0) {
                midlet.showArtist(albumArtistId, subname);
                return;
            }
            // Fallback: fetch the album with artists relationship
            final String aid = id;
            final String artistName = subname;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Hashtable params = new Hashtable();
                        params.put("include", "artists");
                        JSONObject resp = api.APIRequest(
                            "/v1/catalog/" + storefront + "/albums/" + aid,
                            params, "GET", null, null);
                        JSONArray data = resp.getArray("data", null);
                        if (data == null || data.size() == 0) return;
                        JSONObject rels = data.getObject(0).getObject("relationships", null);
                        if (rels == null) return;
                        JSONObject ar = rels.getObject("artists", null);
                        if (ar == null) return;
                        JSONArray ad = ar.getArray("data", null);
                        if (ad == null || ad.size() == 0) return;
                        final String artistId = ad.getObject(0).getString("id", "");
                        if (artistId.length() > 0) {
                            display.callSerially(new Runnable() {
                                public void run() { midlet.showArtist(artistId, artistName); }
                            });
                        }
                    } catch (Exception ignored) {}
                }
            }).start();
            return;
        }
        // Playlist view — use the selected track
        if (trackIds == null || selectedIndex < 0 || selectedIndex >= trackCount) return;
        final String trackId     = trackIds[selectedIndex];
        final String artistName  = trackArtists[selectedIndex];
        new Thread(new Runnable() {
            public void run() {
                try {
                    Hashtable params = new Hashtable();
                    params.put("include", "artists");
                    JSONObject resp = api.APIRequest(
                        "/v1/catalog/" + storefront + "/songs/" + trackId,
                        params, "GET", null, null);
                    JSONArray data = resp.getArray("data", null);
                    if (data == null || data.size() == 0) return;
                    JSONObject rels = data.getObject(0).getObject("relationships", null);
                    if (rels == null) return;
                    JSONObject ar = rels.getObject("artists", null);
                    if (ar == null) return;
                    JSONArray ad = ar.getArray("data", null);
                    if (ad == null || ad.size() == 0) return;
                    final String artistId = ad.getObject(0).getString("id", "");
                    if (artistId.length() > 0) {
                        display.callSerially(new Runnable() {
                            public void run() { midlet.showArtist(artistId, artistName); }
                        });
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    /** Go to the album of the currently highlighted track (playlist mode only). */
    private void goToAlbum() {
        if (midlet == null || !isPlaylist) return;
        if (trackIds == null || selectedIndex < 0 || selectedIndex >= trackCount) return;
        final String trackId    = trackIds[selectedIndex];
        final String artistName = trackArtists[selectedIndex];
        new Thread(new Runnable() {
            public void run() {
                try {
                    Hashtable params = new Hashtable();
                    params.put("include", "albums");
                    JSONObject resp = api.APIRequest(
                        "/v1/catalog/" + storefront + "/songs/" + trackId,
                        params, "GET", null, null);
                    JSONArray data = resp.getArray("data", null);
                    if (data == null || data.size() == 0) return;
                    JSONObject song = data.getObject(0);
                    JSONObject rels = song.getObject("relationships", null);
                    if (rels == null) return;
                    JSONObject albumsRel = rels.getObject("albums", null);
                    if (albumsRel == null) return;
                    JSONArray albumData = albumsRel.getArray("data", null);
                    if (albumData == null || albumData.size() == 0) return;
                    final String albumId = albumData.getObject(0).getString("id", "");
                    JSONObject attrs = song.getObject("attributes", null);
                    final String albumName = attrs != null ? attrs.getString("albumName", "Album") : "Album";
                    if (albumId.length() > 0) {
                        display.callSerially(new Runnable() {
                            public void run() {
                                midlet.showAlbumById(albumId, albumName, artistName, "");
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    // -------------------------------------------------------------------------
    // Color helpers
    // -------------------------------------------------------------------------

    /**
     * Parses a 6-char hex color string (no "#") into an RGB int.
     * Returns def if null or unparseable.
     */
    private static int parseColor(String hex, int def) {
        if (hex == null || hex.length() < 6) return def;
        try {
            // Parse the 6 hex digits as two 3-digit halves to stay within
            // Integer.parseInt's signed range on all CLDC versions.
            int hi = Integer.parseInt(hex.substring(0, 3), 16); // 0x000..0xFFF
            int lo = Integer.parseInt(hex.substring(3, 6), 16); // 0x000..0xFFF
            return (hi << 12) | lo;
        } catch (NumberFormatException e) {
            return def;
        }
    }

    /**
     * Returns a row-selection background: bgColor shifted toward mid-gray
     * by ~15%, preserving apparent lightness direction.
     */
    private int selectionColor() {
        return shiftBrightness(colorBg, isLight(colorBg) ? -30 : 30);
    }

    /** Divider is a subtler shift of bgColor (~10%). */
    private int dividerColor() {
        return shiftBrightness(colorBg, isLight(colorBg) ? -18 : 18);
    }

    /** True if the color is perceptually light (luminance > 50%). */
    private static boolean isLight(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8)  & 0xFF;
        int b =  color        & 0xFF;
        return (r * 299 + g * 587 + b * 114) > 127500; // > 127.5 * 1000
    }

    /**
     * Shifts each RGB channel by delta (positive = brighter, negative = darker),
     * clamping to [0, 255].
     */
    private static int shiftBrightness(int color, int delta) {
        int r = clamp(((color >> 16) & 0xFF) + delta);
        int g = clamp(((color >> 8)  & 0xFF) + delta);
        int b = clamp(( color        & 0xFF) + delta);
        return (r << 16) | (g << 8) | b;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    // -------------------------------------------------------------------------
    // Layout helpers
    // -------------------------------------------------------------------------

    private int trackItemH() {
        return isPlaylist
            ? NAME_FONT.getHeight() + SUBNAME_FONT.getHeight() + PAD * 2
            : NAME_FONT.getHeight() + PAD * 2;
    }

    private static String clip(String text, Font font, int maxW) {
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    private static String strReplace(String src, String from, String to) {
        int i = src.indexOf(from);
        if (i == -1) return src;
        return src.substring(0, i) + to + src.substring(i + from.length());
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
