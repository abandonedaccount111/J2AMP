package com.amplayer.ui;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import com.amplayer.api.AMAPI;
import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.Settings;
import com.amplayer.utils.IOUtils;
import com.amplayer.utils.SocketHttpConnection;
import java.util.Hashtable;
import java.io.InputStream;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

/**
 * Full-screen Now Playing canvas.
 *
 * D-pad mapping:
 *   CENTER (FIRE)  — play / pause
 *   LEFT           — previous track
 *   RIGHT          — next / skip track
 *   UP             — volume +10
 *   DOWN           — volume −10
 */
public class NowPlayingScreen extends Canvas
        implements CommandListener, PlaybackManager.Listener {

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    private static final int COLOR_BG      = 0x000000;
    private static final int COLOR_TEXT1   = 0xFFFFFF;
    private static final int COLOR_TEXT2   = 0x8E8E93;
    private static final int COLOR_ACCENT  = 0xFA2D48;
    private static final int COLOR_BAR_BG  = 0x2C2C2E;
    private static final int COLOR_CTRL_BG = 0x111111;
    private static final int COLOR_DIVIDER = 0x2C2C2E;

    // -------------------------------------------------------------------------
    // Fonts
    // -------------------------------------------------------------------------

    private static final Font TITLE_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font NAME_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SMALL_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD = 8;

    // -------------------------------------------------------------------------
    // Commands  (soft-keys as fallback for devices without full d-pad)
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK           = new Command("Back",           Command.BACK, 1);
    private static final Command CMD_PREV           = new Command("Prev",           Command.ITEM, 2);
    private static final Command CMD_NEXT           = new Command("Next",           Command.ITEM, 3);
    private static final Command CMD_LYRICS         = new Command("Lyrics",         Command.ITEM, 4);
    private static final Command CMD_QUEUE          = new Command("Queue",          Command.ITEM, 5);
    private static final Command CMD_SHUFFLE        = new Command("Shuffle",        Command.ITEM, 6);
    private static final Command CMD_REPEAT         = new Command("Repeat",         Command.ITEM, 7);
    private static final Command CMD_GO_TO_ARTIST   = new Command("Go to Artist",   Command.ITEM, 8);
    private static final Command CMD_GO_TO_ALBUM    = new Command("Go to Album",    Command.ITEM, 9);
    private static final Command CMD_GO_TO_PLAYLIST = new Command("Go to Playlist", Command.ITEM, 10);
    private static final Command CMD_VISUALIZER     = new Command("Visualizer",     Command.ITEM, 11);

    // -------------------------------------------------------------------------
    // References
    // -------------------------------------------------------------------------

    private final PlaybackManager    pm;
    private final Display            display;
    private final Displayable        backScreen;
    private volatile AMAPI           api;    // set lazily once credentials are ready
    private final AppleMusicMIDlet   midlet;

    // -------------------------------------------------------------------------
    // Art
    // -------------------------------------------------------------------------

    private Image   artImage;
    private boolean artLoadStarted  = false;
    private String  lastArtTemplate = "";

    // -------------------------------------------------------------------------
    // Track info snapshot (updated on PM callback)
    // -------------------------------------------------------------------------

    private String  trackName    = "";
    private String  trackArtist  = "";
    private String  trackAlbum   = "";
    private boolean loadingTrack = false;
    private String  errorMsg     = null;

    // -------------------------------------------------------------------------
    // Progress timer
    // -------------------------------------------------------------------------

    private volatile boolean timerActive = false;

    // -------------------------------------------------------------------------
    // Marquee
    // -------------------------------------------------------------------------

    private static final int MARQUEE_SPEED       = 2;  // px per tick
    private static final int MARQUEE_PAUSE_TICKS = 20; // ticks to pause before restarting

    private int  marqueeOffset  = 0;
    private int  marqueePause   = MARQUEE_PAUSE_TICKS;
    private int  marqueeMaxOvf  = 0;  // max overflow in px; 0 = nothing to scroll
    private volatile boolean marqueeRunning = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public NowPlayingScreen(PlaybackManager pm, Display display, Displayable backScreen,
                            AppleMusicMIDlet midlet) {
        super();
        this.pm         = pm;
        this.display    = display;
        this.backScreen = backScreen;
        this.midlet     = midlet;

        pm.setListener(this);

        // Full-screen mode: prevents S60 from mapping the center/OK key to the
        // Options softkey.  BACK is still reachable via the right hardware key.
        setFullScreenMode(true);

        addCommand(CMD_BACK);
        addCommand(CMD_PREV);
        addCommand(CMD_NEXT);
        addCommand(CMD_LYRICS);
        addCommand(CMD_QUEUE);
        addCommand(CMD_SHUFFLE);
        addCommand(CMD_REPEAT);
        addCommand(CMD_GO_TO_ARTIST);
        addCommand(CMD_GO_TO_ALBUM);
        addCommand(CMD_GO_TO_PLAYLIST);
        addCommand(CMD_VISUALIZER);
        setCommandListener(this);

        trackName    = pm.getCurrentName();
        trackArtist  = pm.getCurrentArtist();
        loadingTrack = pm.isLoading();
    }

    /** Called once API credentials are ready (after first playback auth). */
    public void setAPI(AMAPI api) { this.api = api; }

    // -------------------------------------------------------------------------
    // PlaybackManager.Listener
    // -------------------------------------------------------------------------

    public void onTrackChanged(int index) {
        trackName      = pm.getCurrentName();
        trackArtist    = pm.getCurrentArtist();
        trackAlbum     = "";
        loadingTrack   = true;
        errorMsg       = null;
        artImage       = null;
        artLoadStarted = false;
        marqueeOffset  = 0;
        marqueePause   = MARQUEE_PAUSE_TICKS;
        marqueeMaxOvf  = 0;
        repaint();
    }

    public void onPlayStateChanged(boolean playing) {
        loadingTrack = false;
        if (playing) startProgressTimer();
        else         stopProgressTimer();
        repaint();
    }

    public void onError(String msg) {
        loadingTrack = false;
        errorMsg     = msg;
        repaint();
    }

    // -------------------------------------------------------------------------
    // Art loading
    // -------------------------------------------------------------------------

    private void triggerArtLoad() {
        if (!Settings.artEnabled) return;
        if (artLoadStarted) return;
        if (loadingTrack) return;  // wait until song finishes loading to avoid KErrServerBusy -16
        String tmpl = pm.getArtUrlTemplate();
        // Need at least a fallback template or a known track ID to fetch from
        final String trackId = pm.getCurrentId();
        if ((tmpl == null || tmpl.length() == 0) && (trackId == null || trackId.length() == 0)) return;
        if (tmpl != null && tmpl.equals(lastArtTemplate) && artImage != null) return;
        artLoadStarted  = true;
        lastArtTemplate = tmpl != null ? tmpl : "";

        final int    artSize  = computeArtSize();
        final String fallback = tmpl;
        final AMAPI  a        = api;

        new Thread(new Runnable() {
            public void run() {
                // 1. Try to get the track's own artwork URL from the catalog API
                String artUrlTemplate = null;
                if (a != null && trackId != null && trackId.length() > 0) {
                    try {
                        JSONObject resp = a.APIRequest(
                            "/v1/catalog/" + a.getStorefront() + "/songs/" + trackId,
                            null, "GET", null, null);
                        JSONArray data = resp.getArray("data", null);
                        if (data != null && data.size() > 0) {
                            JSONObject attrs = data.getObject(0).getObject("attributes", null);
                            if (attrs != null) {
                                JSONObject artwork = attrs.getObject("artwork", null);
                                if (artwork != null) artUrlTemplate = artwork.getString("url", null);
                                final String album = attrs.getString("albumName", "");
                                if (album.length() > 0) {
                                    display.callSerially(new Runnable() {
                                        public void run() { trackAlbum = album; repaint(); }
                                    });
                                }
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // 2. Fall back to the album/playlist art template
                if (artUrlTemplate == null || artUrlTemplate.length() == 0)
                    artUrlTemplate = fallback;
                if (artUrlTemplate == null || artUrlTemplate.length() == 0) {
                    repaint();
                    return;
                }

                // 3. Build final sized URL and download
                String url = buildArtUrl(artUrlTemplate, artSize);
                if (url == null) { repaint(); return; }

                SocketHttpConnection conn = null;
                InputStream          in   = null;
                try {
                    conn = SocketHttpConnection.open(url);
                    conn.setRequestMethod("GET");
                    in      = conn.openInputStream();
                    byte[] imgData = IOUtils.readAll(in);
                    Image raw      = Image.createImage(imgData, 0, imgData.length);
                    artImage       = scaleImage(raw, artSize, artSize);
                } catch (Exception ignored) {
                } finally {
                    IOUtils.closeQuietly(in);
                    try { if (conn != null) conn.close(); } catch (Exception ignored) {}
                }
                repaint();
            }
        }).start();
    }

    private int computeArtSize() {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return 64;
        int ctrlH = NAME_FONT.getHeight() + PAD * 3;
        int size;
        if (w > h) {
            int artPaneW = w * 35 / 100;
            size = Math.min(artPaneW - PAD * 2, h - ctrlH - PAD * 2);
        } else {
            size = Math.min(w - PAD * 4, h / 2 - PAD * 2);
        }
        return size < 32 ? 32 : size;
    }

    private String buildArtUrl(String tmpl, int size) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0) return null;
        String s = String.valueOf(size);
        tmpl = strReplace(tmpl, "{w}", s);
        tmpl = strReplace(tmpl, "{h}", s);
        tmpl = strReplace(tmpl, "{f}", "jpg");
        tmpl = strReplace(tmpl, "https://", "http://");
        tmpl = strReplace(tmpl, "/image/thumb/gen/600x600", "/image/thumb/gen/" + s + "x" + s);
        return tmpl;
    }

    private static Image scaleImage(Image src, int targetW, int targetH) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        if (srcW == targetW && srcH == targetH) return src;
        // Read one source row at a time — peak extra memory is srcW*4 bytes,
        // not srcW*srcH*4 bytes as a full-image buffer would require.
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

    // -------------------------------------------------------------------------
    // Progress timer
    // -------------------------------------------------------------------------

    private void startProgressTimer() {
        if (timerActive) return;
        timerActive = true;
        new Thread(new Runnable() {
            public void run() {
                while (timerActive && pm.isPlaying()) {
                    repaint();
                    try { Thread.sleep(1000); } catch (InterruptedException e) { break; }
                }
                timerActive = false;
            }
        }).start();
    }

    private void stopProgressTimer() { timerActive = false; }

    // -------------------------------------------------------------------------
    // Marquee timer — runs while screen is shown, independent of play state
    // -------------------------------------------------------------------------

    protected void showNotify() { startMarquee(); }
    protected void hideNotify() { stopMarquee();  }

    private void startMarquee() {
        if (!Settings.marqueeEnabled) return;
        if (marqueeRunning) return;
        marqueeRunning = true;
        new Thread(new Runnable() {
            public void run() {
                while (marqueeRunning) {
                    try { Thread.sleep(80); } catch (InterruptedException e) { break; }
                    tickMarquee();
                    repaint();
                }
            }
        }).start();
    }

    private void stopMarquee() { marqueeRunning = false; }

    private void tickMarquee() {
        if (marqueeMaxOvf <= 0) { marqueeOffset = 0; return; }
        if (marqueePause > 0)   { marqueePause--;    return; }
        marqueeOffset += MARQUEE_SPEED;
        if (marqueeOffset >= marqueeMaxOvf) {
            marqueeOffset = 0;
            marqueePause  = MARQUEE_PAUSE_TICKS;
        }
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        if (!artLoadStarted) triggerArtLoad();

        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Controls bar at bottom (same for both layouts)
        int ctrlH  = NAME_FONT.getHeight() + PAD * 3;
        int ctrlY  = h - ctrlH;
        g.setColor(COLOR_CTRL_BG);
        g.fillRect(0, ctrlY, w, ctrlH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, ctrlY, w, ctrlY);

        // Controls: [SHF]  [|<]  [>||]  [>|]  [RPT]
        int fifthW    = w / 5;
        int btnTY     = ctrlY + PAD;
        int iconCY    = ctrlY + ctrlH / 2;   // vertical centre of control bar
        boolean shuffled   = pm.isShuffled();
        int     repeatMode = pm.getRepeatMode();

        drawShuffleIcon(g, fifthW / 2, iconCY,
                        shuffled ? COLOR_ACCENT : COLOR_TEXT2);

        drawPrevIcon(g, fifthW + fifthW / 2, iconCY, COLOR_TEXT2);

        if (pm.isPlaying()) drawPauseIcon(g, w / 2, iconCY, COLOR_ACCENT);
        else                drawPlayIcon (g, w / 2, iconCY, COLOR_ACCENT);

        drawNextIcon(g, 3 * fifthW + fifthW / 2, iconCY, COLOR_TEXT2);

        drawRepeatIcon(g, 4 * fifthW + fifthW / 2, iconCY, repeatMode,
                       repeatMode != PlaybackManager.REPEAT_NONE ? COLOR_ACCENT : COLOR_TEXT2);

        long pos  = pm.getMediaTimeMs();
        long dur  = pm.getDurationMs();
        int  barH = 4;
        int  timeH = SMALL_FONT.getHeight() + 4;

        if (w > h) {
            // ── Wide / landscape (e.g. 320×240) — iPod split layout ──────────
            int artPaneW = w * 35 / 100;
            int artSize  = Math.min(artPaneW - PAD * 2, ctrlY - PAD * 2);
            if (artSize < 32) artSize = 32;
            int artX = (artPaneW - artSize) / 2;

            drawArt(g, artX, PAD, artSize);

            int rightX = artPaneW + PAD;
            int rightW = w - artPaneW - PAD * 2;
            int barW   = rightW;
            int barX   = rightX;
            int barY   = ctrlY - PAD - barH - timeH;

            // Update marquee max overflow based on this frame's available width
            updateMarqueeMaxOvf(rightW);

            int textY = PAD * 2;
            g.setColor(COLOR_TEXT1);
            drawMarqueeText(g, trackName, NAME_FONT, rightX, textY, rightW);
            textY += NAME_FONT.getHeight() + 4;

            g.setColor(COLOR_ACCENT);
            drawMarqueeText(g, trackArtist, SMALL_FONT, rightX, textY, rightW);
            textY += SMALL_FONT.getHeight() + 3;

            g.setColor(COLOR_TEXT2);
            if (loadingTrack) {
                g.setFont(SMALL_FONT);
                g.drawString("Loading...", rightX, textY, Graphics.LEFT | Graphics.TOP);
            } else if (errorMsg != null) {
                g.setColor(0xFF3B30);
                g.setFont(SMALL_FONT);
                g.drawString(clip(errorMsg, SMALL_FONT, rightW), rightX, textY,
                             Graphics.LEFT | Graphics.TOP);
            } else if (trackAlbum.length() > 0) {
                drawMarqueeText(g, trackAlbum, SMALL_FONT, rightX, textY, rightW);
            }

            drawProgressBar(g, barX, barY, barW, barH, pos, dur);

        } else {
            // ── Portrait — stacked layout ─────────────────────────────────────
            int availW = w - PAD * 4;
            int barW   = availW;
            int barX   = PAD * 2;
            int barY   = ctrlY - PAD - barH - timeH;

            int textRows = NAME_FONT.getHeight() + SMALL_FONT.getHeight() * 2 + 3 + 3 + 3;
            int artSize  = Math.min(w - PAD * 4, barY - PAD * 4 - textRows);
            if (artSize < 32) artSize = 32;
            int artX = (w - artSize) / 2;

            drawArt(g, artX, PAD * 2, artSize);

            // Update marquee max overflow
            updateMarqueeMaxOvf(availW);

            int textY  = PAD * 2 + artSize + PAD;
            int textX  = PAD * 2;   // left edge for marquee; centered when text fits

            g.setColor(COLOR_TEXT1);
            drawMarqueeTextCentered(g, trackName, NAME_FONT, textX, textY, availW, w);
            textY += NAME_FONT.getHeight() + 3;

            g.setColor(COLOR_ACCENT);
            drawMarqueeTextCentered(g, trackArtist, SMALL_FONT, textX, textY, availW, w);
            textY += SMALL_FONT.getHeight() + 3;

            g.setColor(COLOR_TEXT2);
            if (loadingTrack) {
                g.setFont(SMALL_FONT);
                g.drawString("Loading...", w / 2, textY, Graphics.HCENTER | Graphics.TOP);
            } else if (errorMsg != null) {
                g.setFont(SMALL_FONT);
                g.setColor(0xFF3B30);
                g.drawString(clip(errorMsg, SMALL_FONT, availW), w / 2, textY,
                             Graphics.HCENTER | Graphics.TOP);
            } else if (trackAlbum.length() > 0) {
                drawMarqueeTextCentered(g, trackAlbum, SMALL_FONT, textX, textY, availW, w);
            }

            drawProgressBar(g, barX, barY, barW, barH, pos, dur);
        }
    }

    /**
     * Compute the maximum scroll distance needed across all three text fields
     * for the given available width. Called once per paint frame so the
     * marquee thread always has an up-to-date bound.
     */
    private void updateMarqueeMaxOvf(int availW) {
        int ovf = 0;
        ovf = Math.max(ovf, NAME_FONT.stringWidth(trackName)   - availW);
        ovf = Math.max(ovf, SMALL_FONT.stringWidth(trackArtist) - availW);
        if (trackAlbum.length() > 0)
            ovf = Math.max(ovf, SMALL_FONT.stringWidth(trackAlbum) - availW);
        int newMaxOvf = ovf > 0 ? ovf + 16 : 0; // +16 px gap before wrap
        if (newMaxOvf != marqueeMaxOvf) {
            marqueeMaxOvf = newMaxOvf;
            if (marqueeMaxOvf == 0) { marqueeOffset = 0; marqueePause = MARQUEE_PAUSE_TICKS; }
        }
    }

    /** Draw text left-aligned with marquee scrolling when it overflows availW. */
    private void drawMarqueeText(Graphics g, String text, Font font,
                                  int x, int y, int availW) {
        g.setFont(font);
        if (font.stringWidth(text) <= availW) {
            g.drawString(text, x, y, Graphics.LEFT | Graphics.TOP);
            return;
        }
        int cx = g.getClipX(), cy2 = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(x, y, availW, font.getHeight());
        g.drawString(text, x - marqueeOffset, y, Graphics.LEFT | Graphics.TOP);
        g.setClip(cx, cy2, cw, ch);
    }

    /**
     * Like {@link #drawMarqueeText} but centers text when it fits.
     * When scrolling, text is left-aligned and clipped.
     */
    private void drawMarqueeTextCentered(Graphics g, String text, Font font,
                                          int leftX, int y, int availW, int fullW) {
        g.setFont(font);
        if (font.stringWidth(text) <= availW) {
            g.drawString(text, fullW / 2, y, Graphics.HCENTER | Graphics.TOP);
            return;
        }
        int cx = g.getClipX(), cy2 = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(leftX, y, availW, font.getHeight());
        g.drawString(text, leftX - marqueeOffset, y, Graphics.LEFT | Graphics.TOP);
        g.setClip(cx, cy2, cw, ch);
    }

    private void drawArt(Graphics g, int x, int y, int size) {
        if (artImage != null) {
            g.drawImage(artImage, x, y, Graphics.LEFT | Graphics.TOP);
        } else {
            g.setColor(0x1C1C1E);
            g.fillRoundRect(x, y, size, size, 12, 12);
            g.setFont(TITLE_FONT);
            g.setColor(COLOR_TEXT2);
            g.drawString("?", x + size / 2, y + size / 2,
                         Graphics.HCENTER | Graphics.BASELINE);
        }
    }

    private void drawProgressBar(Graphics g, int barX, int barY,
                                  int barW, int barH, long pos, long dur) {
        g.setColor(COLOR_BAR_BG);
        g.fillRoundRect(barX, barY, barW, barH, barH, barH);
        if (dur > 0) {
            int filled = (int)((long) barW * pos / dur);
            if (filled > 0) {
                g.setColor(COLOR_ACCENT);
                g.fillRoundRect(barX, barY, filled, barH, barH, barH);
            }
        }
        g.setFont(SMALL_FONT);
        g.setColor(COLOR_TEXT2);
        g.drawString(formatTime(pos), barX, barY + barH + 4, Graphics.LEFT | Graphics.TOP);
        if (dur > 0) {
            g.drawString(formatTime(dur), barX + barW, barY + barH + 4,
                         Graphics.RIGHT | Graphics.TOP);
        }
    }

    // -------------------------------------------------------------------------
    // Key input  (direct d-pad mapping — no focus cycling)
    // -------------------------------------------------------------------------

    private static final long SEEK_STEP_MS = 10000L; // 10 s per repeated key event

    protected void keyPressed(int keyCode) {
        int action = getGameAction(keyCode);
        switch (action) {
            case FIRE:
                if (pm.isPlaying()) pm.pause(); else pm.resume();
                repaint();
                return;
            case LEFT:
                pm.previous();
                return;
            case RIGHT:
                pm.next();
                return;
        }
        // Number-key shortcuts
        switch (keyCode) {
            case KEY_NUM4: pm.previous();                                      break;
            case KEY_NUM5: if (pm.isPlaying()) pm.pause(); else pm.resume();
                           repaint();                                          break;
            case KEY_NUM6: pm.next();                                          break;
        }
    }

    protected void keyRepeated(int keyCode) {
        int action = getGameAction(keyCode);
        if (action == LEFT || keyCode == KEY_NUM4) {
            pm.seekBy(-SEEK_STEP_MS);
        } else if (action == RIGHT || keyCode == KEY_NUM6) {
            pm.seekBy(SEEK_STEP_MS);
        }
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            stopProgressTimer();
            display.setCurrent(backScreen);
        } else if (c == CMD_PREV) {
            pm.previous();
        } else if (c == CMD_NEXT) {
            pm.next();
        } else if (c == CMD_LYRICS) {
            AMAPI a = api;
            if (a == null) return;  // credentials not ready yet
            String id = pm.getCurrentId();
            if (id == null || id.length() == 0) return;
            display.setCurrent(new LyricsView(
                a, a.getStorefront(), id, pm, display, this, trackName));
        } else if (c == CMD_QUEUE) {
            display.setCurrent(new QueueView(pm, display, this));
        } else if (c == CMD_SHUFFLE) {
            pm.toggleShuffle();
            repaint();
        } else if (c == CMD_REPEAT) {
            pm.cycleRepeat();
            repaint();
        } else if (c == CMD_GO_TO_ARTIST) {
            goToArtist();
        } else if (c == CMD_GO_TO_ALBUM) {
            goToAlbum();
        } else if (c == CMD_GO_TO_PLAYLIST) {
            goToPlaylist();
        } else if (c == CMD_VISUALIZER) {
            if (midlet != null) midlet.showVisualizer(this);
        }
    }

    // -------------------------------------------------------------------------
    // Go-To helpers — fetch relationship data on demand
    // -------------------------------------------------------------------------

    private void goToArtist() {
        final AMAPI a   = api;
        final String id = pm.getCurrentId();
        if (a == null || id == null || id.length() == 0) return;
        final String artistName = pm.getCurrentArtist();
        new Thread(new Runnable() {
            public void run() {
                try {
                    Hashtable params = new Hashtable();
                    params.put("include", "artists");
                    JSONObject resp = a.APIRequest(
                        "/v1/catalog/" + a.getStorefront() + "/songs/" + id,
                        params, "GET", null, null);
                    JSONArray data = resp.getArray("data", null);
                    if (data == null || data.size() == 0) return;
                    JSONObject rels = data.getObject(0).getObject("relationships", null);
                    if (rels == null) return;
                    JSONObject artistsRel = rels.getObject("artists", null);
                    if (artistsRel == null) return;
                    JSONArray artistData = artistsRel.getArray("data", null);
                    if (artistData == null || artistData.size() == 0) return;
                    final String artistId = artistData.getObject(0).getString("id", "");
                    if (artistId.length() > 0 && midlet != null) {
                        display.callSerially(new Runnable() {
                            public void run() { midlet.showArtist(artistId, artistName); }
                        });
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void goToAlbum() {
        final AMAPI a   = api;
        final String id = pm.getCurrentId();
        if (a == null || id == null || id.length() == 0) return;
        new Thread(new Runnable() {
            public void run() {
                try {
                    Hashtable params = new Hashtable();
                    params.put("include", "albums");
                    JSONObject resp = a.APIRequest(
                        "/v1/catalog/" + a.getStorefront() + "/songs/" + id,
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
                    JSONObject album = albumData.getObject(0);
                    final String albumId = album.getString("id", "");
                    // Try to get album name/artist from song attributes
                    JSONObject attrs = song.getObject("attributes", null);
                    final String albumName  = attrs != null ? attrs.getString("albumName",  "Album") : "Album";
                    final String artistName = attrs != null ? attrs.getString("artistName", "")      : "";
                    final String artUrl     = pm.getArtUrlTemplate();
                    if (albumId.length() > 0 && midlet != null) {
                        display.callSerially(new Runnable() {
                            public void run() {
                                midlet.showAlbumById(albumId, albumName, artistName, artUrl);
                            }
                        });
                    }
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private void goToPlaylist() {
        if (midlet == null) return;
        final String pid  = midlet.getSourcePlaylistId();
        final String pnm  = midlet.getSourcePlaylistName();
        final String part = midlet.getSourcePlaylistArtUrl();
        if (pid == null || pid.length() == 0) return;
        AMAPI a = api;
        if (a == null) return;
        display.setCurrent(new DetailView(
            pid, true, false, pnm != null ? pnm : "Playlist", "", part != null ? part : "",
            a, a.getStorefront(), display, this, midlet));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String formatTime(long ms) {
        int secs = (int)(ms / 1000L);
        int mins = secs / 60;
        secs     = secs % 60;
        return mins + ":" + (secs < 10 ? "0" : "") + secs;
    }

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    /**
     * Play icon: solid right-pointing triangle centred at (cx, cy).
     *   ▶   base on left, tip on right
     */
    private static void drawPlayIcon(Graphics g, int cx, int cy, int color) {
        g.setColor(color);
        g.fillTriangle(cx - 6, cy - 6, cx - 6, cy + 6, cx + 6, cy);
    }

    /**
     * Pause icon: two vertical bars centred at (cx, cy).
     *   ❙❙
     */
    private static void drawPauseIcon(Graphics g, int cx, int cy, int color) {
        g.setColor(color);
        g.fillRect(cx - 5, cy - 6, 3, 12);
        g.fillRect(cx + 2, cy - 6, 3, 12);
    }

    /**
     * Next icon: right-pointing triangle + vertical bar on right, centred at (cx, cy).
     *   ▶|
     */
    private static void drawNextIcon(Graphics g, int cx, int cy, int color) {
        g.setColor(color);
        g.fillTriangle(cx - 6, cy - 5, cx - 6, cy + 5, cx + 2, cy);
        g.fillRect(cx + 4, cy - 6, 3, 12);
    }

    /**
     * Previous icon: vertical bar on left + left-pointing triangle, centred at (cx, cy).
     *   |◀
     */
    private static void drawPrevIcon(Graphics g, int cx, int cy, int color) {
        g.setColor(color);
        g.fillRect(cx - 7, cy - 6, 3, 12);
        g.fillTriangle(cx + 6, cy - 5, cx + 6, cy + 5, cx - 2, cy);
    }

    /**
     * Shuffle icon: two crossing diagonal arrows, both pointing right.
     *
     *  ──↘         Upper path (bottom-left → top-right)
     *     ╳
     *  ──↗         Lower path (top-left → bottom-right)
     *
     * Centred at (cx, cy), total span ~18 × 10 px.
     */
    private static void drawShuffleIcon(Graphics g, int cx, int cy, int color) {
        g.setColor(color);
        // x positions for the four vertical "columns"
        int x0 = cx - 9;   // left edge
        int x1 = cx - 3;   // end of left horizontal / start of diagonal
        int x2 = cx + 3;   // end of diagonal / start of right horizontal
        int x3 = cx + 9;   // tip of arrowhead
        int yt  = cy - 3;  // upper path y
        int yb  = cy + 3;  // lower path y
        int aw  = 3;        // arrowhead arm half-height

        // Upper path  (yb → yt)
        g.drawLine(x0, yb, x1, yb);
        g.drawLine(x1, yb, x2, yt);
        g.drawLine(x2, yt, x3 - aw - 1, yt);
        g.fillTriangle(x3, yt, x3 - aw - 1, yt - aw, x3 - aw - 1, yt + aw);

        // Lower path  (yt → yb)
        g.drawLine(x0, yt, x1, yt);
        g.drawLine(x1, yt, x2, yb);
        g.drawLine(x2, yb, x3 - aw - 1, yb);
        g.fillTriangle(x3, yb, x3 - aw - 1, yb - aw, x3 - aw - 1, yb + aw);
    }

    /**
     * Repeat icon: a rectangular loop with directional arrows.
     *
     *  ←──────┐   top edge, left-pointing arrow
     *  │       │
     *  └──────→   bottom edge, right-pointing arrow
     *
     * For REPEAT_ONE a small "1" is drawn inside the loop.
     * Centred at (cx, cy), total span ~18 × 12 px.
     */
    private static void drawRepeatIcon(Graphics g, int cx, int cy, int mode, int color) {
        g.setColor(color);
        int x0 = cx - 9;   // left edge
        int x1 = cx + 9;   // right edge
        int yt  = cy - 5;  // top edge y
        int yb  = cy + 5;  // bottom edge y
        int aw  = 3;        // arrowhead arm half-height

        // Top edge — left-pointing arrowhead at left end
        g.drawLine(x0 + aw + 1, yt, x1, yt);
        g.fillTriangle(x0, yt, x0 + aw + 1, yt - aw, x0 + aw + 1, yt + aw);

        // Bottom edge — right-pointing arrowhead at right end
        g.drawLine(x0, yb, x1 - aw - 1, yb);
        g.fillTriangle(x1, yb, x1 - aw - 1, yb - aw, x1 - aw - 1, yb + aw);

        // Vertical sides
        g.drawLine(x0, yt, x0, yb);
        g.drawLine(x1, yt, x1, yb);

        // For REPEAT_ONE overlay a "1" in the centre of the loop
        if (mode == PlaybackManager.REPEAT_ONE) {
            g.setFont(SMALL_FONT);
            g.drawString("1", cx, cy - SMALL_FONT.getHeight() / 2,
                         Graphics.HCENTER | Graphics.TOP);
        }
    }

    private static String strReplace(String src, String from, String to) {
        int i = src.indexOf(from);
        if (i == -1) return src;
        return src.substring(0, i) + to + src.substring(i + from.length());
    }
}
