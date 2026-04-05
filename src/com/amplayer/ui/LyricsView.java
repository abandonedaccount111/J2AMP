package com.amplayer.ui;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import com.amplayer.api.AMAPI;
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
 * Full-screen lyrics canvas.
 *
 * - Fetches TTML from /v1/catalog/{sf}/songs/{id}/lyrics
 * - Wraps and centres each lyric line
 * - Syncs highlight to playback position at 100 ms intervals
 * - Automatically reloads when the track changes
 */
public class LyricsView extends Canvas
        implements CommandListener, PlaybackManager.Listener {

    // -------------------------------------------------------------------------
    // Colors / fonts
    // -------------------------------------------------------------------------

    private static final int COLOR_BG      = 0x000000;
    private static final int COLOR_HEADER  = 0x111111;
    private static final int COLOR_DIVIDER = 0x2C2C2E;
    private static final int COLOR_PAST    = 0x444447;
    private static final int COLOR_ACTIVE  = 0xFFFFFF;
    private static final int COLOR_FUTURE  = 0x8E8E93;

    // FACE_SYSTEM gives the best Unicode/CJK coverage available on the device.
    // FACE_PROPORTIONAL maps to Nokia Sans on S60 which lacks CJK glyphs.
    private static final Font HDR_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font LINE_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    private static final Font ACT_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);

    private static final int PAD      = 8;
    private static final int LINE_GAP = 10;  // vertical gap between logical lines

    // -------------------------------------------------------------------------
    // Commands  (used only on non-Nokia devices)
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK = new Command("Back", Command.BACK, 1);

    // -------------------------------------------------------------------------
    // Nokia soft-key menu
    // -------------------------------------------------------------------------

    private static final String[] NOKIA_MENU_ITEMS = { "Back" };
    private final boolean isNokia;
    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    private final AMAPI                    api;
    private final String                   storefront;
    private final PlaybackManager          pm;
    private final Display                  display;
    private final Displayable              backScreen;
    /** Previous PM listener (NowPlayingScreen) — chained for all callbacks. */
    private final PlaybackManager.Listener prevListener;

    // -------------------------------------------------------------------------
    // Mutable track state
    // -------------------------------------------------------------------------

    private String currentSongId;
    private String currentTitle;

    // -------------------------------------------------------------------------
    // Parsed lyrics  (parallel arrays; lineCount is the valid length)
    // -------------------------------------------------------------------------

    private long[]   beginMs   = new long[0];
    private long[]   endMs     = new long[0];
    private String[] lines     = new String[0];
    private int      lineCount = 0;

    // -------------------------------------------------------------------------
    // Wrap cache — rebuilt once per unique (maxW, lyrics set); avoids
    // allocating Vector + String[] on every 100 ms paint() call.
    // wrappedLines[i]  = LINE_FONT rows for line i
    // activeWrapped    = ACT_FONT  rows for the currently highlighted line
    // -------------------------------------------------------------------------

    private String[][] wrappedLines      = null;
    private String[]   activeWrapped     = null;
    private int        activeCacheIdx    = -1;
    private int        wrapCacheW        = -1;  // maxW the cache was built for

    // -------------------------------------------------------------------------
    // UI state
    // -------------------------------------------------------------------------

    private String           status      = "Loading...";
    private boolean          loading     = true;
    private volatile boolean timerActive = false;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public LyricsView(AMAPI api, String storefront, String songId,
                      PlaybackManager pm, Display display, Displayable backScreen,
                      String songTitle) {
        this.api           = api;
        this.storefront    = storefront;
        this.pm            = pm;
        this.display       = display;
        this.backScreen    = backScreen;
        this.currentSongId = songId;
        this.currentTitle  = songTitle != null ? songTitle : "";

        // Chain the existing listener (NowPlayingScreen) so it keeps receiving events.
        prevListener = pm.getListener();
        pm.setListener(this);

        isNokia = Settings.getDeviceEnvironment().indexOf("nokia") >= 0;
        setFullScreenMode(true);
        if (!isNokia) {
            addCommand(CMD_BACK);
            setCommandListener(this);
        }

        fetchLyrics(songId);
        startTimer();
    }

    // -------------------------------------------------------------------------
    // PlaybackManager.Listener — auto-reload on track change; chain everything
    // -------------------------------------------------------------------------

    public void onTrackChanged(int index) {
        if (prevListener != null) prevListener.onTrackChanged(index);
        String newId    = pm.getCurrentId();
        String newTitle = pm.getCurrentName();
        synchronized (this) {
            currentSongId  = newId;
            currentTitle   = newTitle != null ? newTitle : "";
            lineCount      = 0;
            beginMs        = new long[0];
            endMs          = new long[0];
            lines          = new String[0];
            wrappedLines   = null;
            activeWrapped  = null;
            activeCacheIdx = -1;
            wrapCacheW     = -1;
            status         = "Loading...";
            loading        = true;
        }
        repaint();
        fetchLyrics(newId);
    }

    public void onPlayStateChanged(boolean playing) {
        if (prevListener != null) prevListener.onPlayStateChanged(playing);
    }

    public void onError(String msg) {
        if (prevListener != null) prevListener.onError(msg);
    }

    // -------------------------------------------------------------------------
    // Network — fetch and parse on a background thread
    // -------------------------------------------------------------------------

    private void fetchLyrics(final String songId) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    String endpoint = "/v1/catalog/" + storefront
                                    + "/songs/" + songId + "/lyrics";
                    JSONObject resp  = api.APIRequest(endpoint, null, "GET", null, null);
                    JSONArray  data  = resp.getArray("data", null);
                    if (data == null || data.size() == 0) {
                        setStatus("No lyrics available");
                        return;
                    }
                    JSONObject attrs = data.getObject(0).getObject("attributes", null);
                    if (attrs == null) {
                        setStatus("No lyrics data");
                        return;
                    }
                    String ttml = attrs.getString("ttml", null);
                    if (ttml == null || ttml.length() == 0) {
                        setStatus("No TTML data");
                        return;
                    }
                    // Only apply if the song hasn't changed since we started fetching
                    synchronized (LyricsView.this) {
                        if (!songId.equals(currentSongId)) return;
                        parseTtml(ttml);
                        // Invalidate wrap cache so paint() rebuilds it with current width
                        wrappedLines   = null;
                        activeWrapped  = null;
                        activeCacheIdx = -1;
                        wrapCacheW     = -1;
                        loading        = false;
                    }
                    repaint();
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    if (msg.startsWith("HTTP 404")) setStatus("Lyrics not found");
                    else                            setStatus(msg);
                }
            }
        }).start();
    }

    private void setStatus(String msg) {
        synchronized (this) { status = msg; loading = false; }
        repaint();
    }

    // -------------------------------------------------------------------------
    // TTML parsing
    // -------------------------------------------------------------------------

    private void parseTtml(String ttml) {
        // Count <p  occurrences to size arrays up front (avoids Vector on heap)
        int count = 0;
        int pos   = 0;
        while ((pos = ttml.indexOf("<p ", pos)) >= 0) { count++; pos++; }

        long[]   newBeginMs = new long[count];
        long[]   newEndMs   = new long[count];
        String[] newLines   = new String[count];
        int      n          = 0;

        pos = 0;
        while (true) {
            int pStart = ttml.indexOf("<p ", pos);
            if (pStart < 0) break;

            int tagClose = ttml.indexOf('>', pStart);
            if (tagClose < 0) break;

            String tag      = ttml.substring(pStart, tagClose + 1);
            String beginVal = attrValue(tag, "begin");
            String endVal   = attrValue(tag, "end");
            if (beginVal == null || endVal == null) { pos = pStart + 1; continue; }

            int contentStart = tagClose + 1;
            int pEnd         = ttml.indexOf("</p>", contentStart);
            if (pEnd < 0) { pos = pStart + 1; continue; }

            String text = stripTags(ttml.substring(contentStart, pEnd));
            if (text.length() > 0) {
                newBeginMs[n] = parseTtmlTime(beginVal);
                newEndMs[n]   = parseTtmlTime(endVal);
                newLines[n]   = text;
                n++;
            }
            pos = pEnd + 4;
        }

        // Trim arrays to exact count to free over-allocated slots
        if (n < count) {
            long[]   tb = new long[n];   System.arraycopy(newBeginMs, 0, tb, 0, n); newBeginMs = tb;
            long[]   te = new long[n];   System.arraycopy(newEndMs,   0, te, 0, n); newEndMs   = te;
            String[] tl = new String[n]; System.arraycopy(newLines,   0, tl, 0, n); newLines   = tl;
        }
        beginMs   = newBeginMs;
        endMs     = newEndMs;
        lines     = newLines;
        lineCount = n;
    }

    /** Build LINE_FONT wrap cache for all lines at the given max pixel width. */
    private void buildWrapCache(int maxW) {
        String[][] cache = new String[lineCount][];
        for (int i = 0; i < lineCount; i++)
            cache[i] = wrapText(lines[i], LINE_FONT, maxW);
        wrappedLines   = cache;
        wrapCacheW     = maxW;
        activeWrapped  = null;
        activeCacheIdx = -1;
    }

    private static String attrValue(String tag, String name) {
        String needle = name + "=\"";
        int    start  = tag.indexOf(needle);
        if (start < 0) return null;
        start += needle.length();
        int end = tag.indexOf('"', start);
        return end > start ? tag.substring(start, end) : null;
    }

    /** Parse TTML timestamp: "SS.mmm", "M:SS.mmm", "H:MM:SS.mmm" → milliseconds. */
    private static long parseTtmlTime(String t) {
        int colon1 = t.indexOf(':');
        int colon2 = colon1 >= 0 ? t.indexOf(':', colon1 + 1) : -1;

        long hours = 0, mins = 0;
        String secPart;
        if (colon2 >= 0) {
            hours   = Long.parseLong(t.substring(0, colon1).trim());
            mins    = Long.parseLong(t.substring(colon1 + 1, colon2).trim());
            secPart = t.substring(colon2 + 1);
        } else if (colon1 >= 0) {
            mins    = Long.parseLong(t.substring(0, colon1).trim());
            secPart = t.substring(colon1 + 1);
        } else {
            secPart = t;
        }

        int  dot  = secPart.indexOf('.');
        long secs = 0, frac = 0;
        if (dot >= 0) {
            secs = Long.parseLong(secPart.substring(0, dot).trim());
            String fracStr = secPart.substring(dot + 1);
            while (fracStr.length() < 3) fracStr += "0";
            if (fracStr.length() > 3)    fracStr  = fracStr.substring(0, 3);
            frac = Long.parseLong(fracStr);
        } else {
            secs = Long.parseLong(secPart.trim());
        }
        return hours * 3600000L + mins * 60000L + secs * 1000L + frac;
    }

    private static String stripTags(String s) {
        StringBuffer sb    = new StringBuffer(s.length());
        boolean      inTag = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if      (c == '<') { inTag = true;  }
            else if (c == '>') { inTag = false; }
            else if (!inTag)   { sb.append(c);  }
        }
        String r = sb.toString().trim();
        int idx;
        while ((idx = r.indexOf("  ")) >= 0)
            r = r.substring(0, idx) + " " + r.substring(idx + 2);
        return r;
    }

    // -------------------------------------------------------------------------
    // Text wrapping
    // -------------------------------------------------------------------------

    /**
     * Wrap text to fit within maxW pixels using word boundaries.
     * Returns an array of visual rows, each centred when drawn.
     */
    private static String[] wrapText(String text, Font f, int maxW) {
        if (text == null || text.length() == 0) return new String[]{ "" };
        if (f.stringWidth(text) <= maxW) return new String[]{ text };

        Vector rows    = new Vector();
        int    start   = 0;
        int    len     = text.length();

        while (start < len) {
            // Find how many characters fit on this row
            int end = len;
            // Binary-search-style: walk forward until we exceed maxW
            int cur = start;
            int lastSpace = -1;
            while (cur < len) {
                if (text.charAt(cur) == ' ') lastSpace = cur;
                if (f.stringWidth(text.substring(start, cur + 1)) > maxW) {
                    // Exceeded — break at last space if possible
                    end = (lastSpace > start) ? lastSpace : cur;
                    break;
                }
                cur++;
            }
            rows.addElement(text.substring(start, end).trim());
            start = end + 1;  // skip the space
        }

        String[] result = new String[rows.size()];
        for (int i = 0; i < result.length; i++) result[i] = (String) rows.elementAt(i);
        return result;
    }

    // -------------------------------------------------------------------------
    // Sync timer — 100 ms
    // -------------------------------------------------------------------------

    private void startTimer() {
        if (timerActive) return;
        timerActive = true;
        new Thread(new Runnable() {
            public void run() {
                while (timerActive) {
                    repaint();
                    try { Thread.sleep(100); } catch (InterruptedException e) { break; }
                }
            }
        }).start();
    }

    // -------------------------------------------------------------------------
    // Current line lookup
    // -------------------------------------------------------------------------

    private int findCurrentLine() {
        if (lineCount == 0) return -1;
        long pos = pm.getMediaTimeMs();
        int  cur = -1;
        for (int i = 0; i < lineCount; i++) {
            if (beginMs[i] <= pos) cur = i;
            else break;
        }
        return cur;
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
        int hdrH = HDR_FONT.getHeight() + PAD * 2;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, 0, w, hdrH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, hdrH, w, hdrH);
        g.setFont(HDR_FONT);
        g.setColor(COLOR_ACTIVE);
        String title;
        synchronized (this) { title = currentTitle; }
        g.drawString(wrapText(title, HDR_FONT, w - PAD * 2)[0],
                     w / 2, PAD, Graphics.HCENTER | Graphics.TOP);

        int lyricsTop = hdrH + 1;
        int skH       = isNokia ? LINE_FONT.getHeight() + PAD * 2 : 0;
        int lyricsH   = h - lyricsTop - skH;
        int maxW      = w - PAD * 4;

        // Loading / error
        boolean isLoading;
        String  statusText;
        int     lc;
        synchronized (this) { isLoading = loading; statusText = status; lc = lineCount; }

        if (isLoading || lc == 0) {
            g.setFont(LINE_FONT);
            g.setColor(COLOR_FUTURE);
            g.drawString(statusText, w / 2, lyricsTop + lyricsH / 2,
                         Graphics.HCENTER | Graphics.BASELINE);
            if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h); }
            return;
        }

        int curLine = findCurrentLine();

        // Ensure LINE_FONT wrap cache is valid
        if (wrapCacheW != maxW || wrappedLines == null) buildWrapCache(maxW);

        // Ensure ACT_FONT wrap for the current active line is cached
        if (curLine != activeCacheIdx) {
            activeWrapped  = (curLine >= 0 && curLine < lc)
                             ? wrapText(lines[curLine], ACT_FONT, maxW) : null;
            activeCacheIdx = curLine;
        }

        // Compute total pixel height of all lines before curLine (for scroll)
        int heightBefore = 0;
        for (int i = 0; i < curLine && i < lc; i++) {
            String[] rows = wrappedLines[i];
            heightBefore += rows.length * LINE_FONT.getHeight() + LINE_GAP;
        }

        // Place the active line's first row at the vertical centre of the lyrics area
        int centerY = lyricsTop + lyricsH / 2;
        int baseY   = (curLine <= 0)
                ? lyricsTop + PAD * 2
                : centerY - heightBefore;

        // Clip to lyrics area
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, lyricsTop, w, lyricsH);

        int y = baseY;
        for (int i = 0; i < lc; i++) {
            boolean  active = (i == curLine);
            Font     f      = active ? ACT_FONT : LINE_FONT;
            String[] rows   = active ? activeWrapped : wrappedLines[i];
            if (rows == null) rows = wrappedLines[i]; // safety fallback
            int      blockH = rows.length * f.getHeight() + LINE_GAP;

            if (y + blockH < lyricsTop) { y += blockH; continue; }
            if (y > lyricsTop + lyricsH)  break;

            // Active-line highlight pill
            if (active) {
                g.setColor(0x1C1C1E);
                g.fillRoundRect(PAD / 2, y - 2,
                                w - PAD, rows.length * f.getHeight() + 4,
                                8, 8);
            }

            int color = active ? COLOR_ACTIVE
                               : (i < curLine ? COLOR_PAST : COLOR_FUTURE);
            g.setFont(f);
            g.setColor(color);
            for (int r = 0; r < rows.length; r++) {
                int rowY = y + r * f.getHeight();
                if (rowY + f.getHeight() >= lyricsTop && rowY <= lyricsTop + lyricsH) {
                    g.drawString(rows[r], w / 2, rowY, Graphics.HCENTER | Graphics.TOP);
                }
            }

            y += blockH;
        }

        g.setClip(cx, cy, cw, ch);
        if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h); }
    }

    // -------------------------------------------------------------------------
    // Key input — mirrors NowPlayingScreen bindings
    // -------------------------------------------------------------------------

    private static final long SEEK_STEP_MS = 10000L; // 10 s per repeated event

    protected void keyPressed(int keyCode) {
        if (isNokia) {
            if (keyCode == -6) {
                if (nokiaMenuOpen) {
                    nokiaMenuOpen = false;
                    goBack();
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
                    goBack();
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
                    goBack();
                    repaint();
                }
                return;
            }
        }
        int action = getGameAction(keyCode);
        switch (action) {
            case FIRE:
                if (pm.isPlaying()) pm.pause(); else pm.resume();
                return;
            case LEFT:
                pm.previous();
                return;
            case RIGHT:
                pm.next();
                return;
        }
        switch (keyCode) {
            case KEY_NUM4: pm.previous();                                      break;
            case KEY_NUM5: if (pm.isPlaying()) pm.pause(); else pm.resume();   break;
            case KEY_NUM6: pm.next();                                          break;
        }
    }

    private void goBack() {
        timerActive = false;
        pm.setListener(prevListener);
        display.setCurrent(backScreen);
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(LINE_FONT);
        int labelY = barY + (skH - LINE_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(COLOR_ACTIVE);
            g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_FUTURE);
            g.drawString("Close", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(COLOR_ACTIVE);
            g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_FUTURE);
            g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h) {
        int itemH  = LINE_FONT.getHeight() + 6;
        int skH    = LINE_FONT.getHeight() + PAD * 2;
        int menuH  = itemH * NOKIA_MENU_ITEMS.length + PAD * 2;
        int menuY  = h - skH - menuH;

        g.setColor(COLOR_HEADER);
        g.fillRect(0, menuY, w, menuH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, menuY, w, 1);

        for (int i = 0; i < NOKIA_MENU_ITEMS.length; i++) {
            int y = menuY + PAD + i * itemH;
            if (i == nokiaMenuSel) {
                g.setColor(COLOR_ACTIVE);
                g.fillRect(0, y - 2, w, itemH);
                g.setColor(COLOR_BG);
            } else {
                g.setColor(COLOR_ACTIVE);
            }
            g.setFont(LINE_FONT);
            g.drawString(NOKIA_MENU_ITEMS[i], PAD, y, Graphics.LEFT | Graphics.TOP);
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
        if (c == CMD_BACK) goBack();
    }
}
