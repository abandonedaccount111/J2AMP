package com.amplayer.ui;

import com.amplayer.utils.Settings;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Virtual-scroll list that loads item metadata in pages.
 *
 * All items are uniform height (no section headers), so visible-item
 * calculation is O(1).  The DataSource is invoked on a background thread
 * whenever the selection moves within LOAD_AHEAD rows of the last loaded item
 * and more pages are available.  appendItems() may be called from any thread.
 *
 * Typical usage:
 *   LazyList list = new LazyList("Songs", myDataSource, myListener);
 *   list.appendItems(types, names, subs, actions, nextUrl);  // first page
 *   display.setCurrent(list);
 */
public class LazyList extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    public interface DataSource {
        /**
         * Fetch the next page.  Called on a new background thread.
         * When done, call list.appendItems() (or list.setLoadError()).
         */
        void loadNextPage(LazyList list, String nextUrl);
    }

    public interface SelectionListener {
        void onItemSelected(int index, String type, String name,
                            String sub, BaseAction action);
    }

    public interface ContextListener {
        void onContextAction(int index, String type, String name,
                             String sub, BaseAction action, String tag);
    }

    // -------------------------------------------------------------------------
    // Colors  (same palette as BaseList)
    // -------------------------------------------------------------------------

    private static final int COLOR_BG        = 0x000000;
    private static final int COLOR_SELECTED  = 0x1C1C1E;
    private static final int COLOR_ACCENT    = 0xFA2D48;
    private static final int COLOR_NAME      = 0xFFFFFF;
    private static final int COLOR_SUBNAME   = 0x8E8E93;
    private static final int COLOR_DIVIDER   = 0x2C2C2E;
    private static final int COLOR_HEADER_BG = 0x111111;

    // -------------------------------------------------------------------------
    // Fonts & layout
    // -------------------------------------------------------------------------

    private static final Font NAME_FONT    = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUBNAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font TITLE_FONT   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);

    private static final int PAD         = 8;
    private static final int ACCENT_W    = 3;
    private static final int TITLE_BAR_H = TITLE_FONT.getHeight() + PAD * 2;
    static final         int ITEM_H      = NAME_FONT.getHeight() + SUBNAME_FONT.getHeight() + PAD * 2;

    /** Start loading the next page when selection is this many rows from the end. */
    private static final int LOAD_AHEAD = 6;

    // -------------------------------------------------------------------------
    // Item storage  (grown dynamically as pages arrive)
    // -------------------------------------------------------------------------

    private String[]     types;
    private String[]     names;
    private String[]     subs;
    private BaseAction[] actions;
    private int          loadedCount = 0;

    // -------------------------------------------------------------------------
    // Pagination state
    // -------------------------------------------------------------------------

    private String  nextUrl       = null;  // null = no more pages
    private boolean loadingMore   = false;
    private boolean loadScheduled = false; // guards against double-scheduling

    private final DataSource       dataSource;
    private final SelectionListener itemListener;
    private       Runnable          backAction;
    private       ContextListener   contextListener;

    // Context menu items (max 4)
    private final String[] contextLabels = new String[4];
    private final String[] contextTags   = new String[4];
    private int contextCount = 0;

    // -------------------------------------------------------------------------
    // Scroll / selection
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
    // Commands  (non-Nokia only)
    // -------------------------------------------------------------------------

    private static final Command CMD_SELECT = new Command("Select", Command.OK,   1);
    private static final Command CMD_BACK   = new Command("Back",   Command.BACK, 1);

    // -------------------------------------------------------------------------
    // Nokia soft-key menu
    // -------------------------------------------------------------------------

    private final boolean isNokia;
    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public LazyList(String title, DataSource source, SelectionListener listener) {
        super();
        setTitle(title);
        this.dataSource   = source;
        this.itemListener = listener;
        types   = new String[32];
        names   = new String[32];
        subs    = new String[32];
        actions = new BaseAction[32];
        isNokia = Settings.getDeviceEnvironment().indexOf("nokia") >= 0;
        if (!isNokia) {
            addCommand(CMD_SELECT);
            addCommand(CMD_BACK);
            setCommandListener(this);
            setFullScreenMode(false);
        } else {
            setFullScreenMode(true);
        }
    }

    // -------------------------------------------------------------------------
    // Data loading API — safe to call from any thread
    // -------------------------------------------------------------------------

    /**
     * Append a page of items.  Grows internal arrays as needed.
     *
     * @param newTypes   item type strings
     * @param newNames   display names
     * @param newSubs    sub-labels / artist names
     * @param newActions actions for each item
     * @param next       URL of the next page, or null if this is the last page
     */
    public synchronized void appendItems(String[] newTypes, String[] newNames,
                                          String[] newSubs, BaseAction[] newActions,
                                          String next) {
        int add = (newTypes != null) ? newTypes.length : 0;
        if (add > 0) {
            int required = loadedCount + add;
            if (required > types.length) {
                int cap = Math.max(required, types.length * 2);
                String[]     t2 = new String[cap];
                String[]     n2 = new String[cap];
                String[]     s2 = new String[cap];
                BaseAction[] a2 = new BaseAction[cap];
                System.arraycopy(types,   0, t2, 0, loadedCount);
                System.arraycopy(names,   0, n2, 0, loadedCount);
                System.arraycopy(subs,    0, s2, 0, loadedCount);
                System.arraycopy(actions, 0, a2, 0, loadedCount);
                types = t2; names = n2; subs = s2; actions = a2;
            }
            System.arraycopy(newTypes,   0, types,   loadedCount, add);
            System.arraycopy(newNames,   0, names,   loadedCount, add);
            System.arraycopy(newSubs,    0, subs,    loadedCount, add);
            System.arraycopy(newActions, 0, actions, loadedCount, add);
            loadedCount += add;
        }
        nextUrl       = next;
        loadingMore   = false;
        loadScheduled = false;
        maybeLoadMore();
        repaint();
    }

    /** Call when a page fetch fails — clears the loading indicator. */
    public synchronized void setLoadError() {
        loadingMore   = false;
        loadScheduled = false;
        repaint();
    }

    // -------------------------------------------------------------------------
    // Accessors for queue building
    // -------------------------------------------------------------------------

    public synchronized int        getLoadedCount()       { return loadedCount; }
    public synchronized String     getLoadedType(int i)   { return safeStr(types,   i); }
    public synchronized String     getLoadedName(int i)   { return safeStr(names,   i); }
    public synchronized String     getLoadedSub(int i)    { return safeStr(subs,    i); }
    public synchronized BaseAction getLoadedAction(int i) { return (i >= 0 && i < loadedCount) ? actions[i] : null; }
    public synchronized String     getNextUrl()           { return nextUrl; }
    public synchronized boolean    hasMore()              { return nextUrl != null; }

    private String safeStr(String[] arr, int i) {
        return (i >= 0 && i < loadedCount && arr[i] != null) ? arr[i] : "";
    }

    // -------------------------------------------------------------------------
    // Listener / action wiring
    // -------------------------------------------------------------------------

    public void setBackAction(Runnable r) { this.backAction = r; }

    public void setContextListener(ContextListener l) { this.contextListener = l; }

    /** Register an extra context-menu item (Nokia menu + non-Nokia command). */
    public void addContextItem(Command cmd, String tag) {
        if (contextCount < contextLabels.length) {
            contextLabels[contextCount] = cmd.getLabel();
            contextTags[contextCount]   = tag;
            contextCount++;
            if (!isNokia) addCommand(cmd);
        }
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w   = getWidth();
        int h   = getHeight();
        int skH = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
        int listH = h - TITLE_BAR_H - skH;
        if (listH < 1) listH = 1;

        int lc, lm;
        synchronized (this) { lc = loadedCount; lm = loadingMore ? 1 : 0; }

        // Background
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        // Title bar
        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, 0, w, TITLE_BAR_H);
        g.setFont(TITLE_FONT);
        g.setColor(COLOR_NAME);
        g.drawString(getTitle(), PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, TITLE_BAR_H - 2, w, 2);

        // Clip list area
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, TITLE_BAR_H, w, listH);

        int totalRows = lc + lm;

        if (totalRows == 0) {
            g.setFont(SUBNAME_FONT);
            g.setColor(COLOR_SUBNAME);
            g.drawString("Loading...", PAD + ACCENT_W + 4, TITLE_BAR_H + PAD,
                         Graphics.LEFT | Graphics.TOP);
            g.setClip(cx, cy, cw, ch);
            if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH); }
            return;
        }

        int firstRow = scrollPx / ITEM_H;
        int lastRow  = Math.min(totalRows, (scrollPx + listH + ITEM_H - 1) / ITEM_H);
        int textX    = PAD + ACCENT_W + 4;
        int availW   = w - textX - PAD - 3; // -3 for scroll bar

        for (int i = firstRow; i < lastRow; i++) {
            int y = TITLE_BAR_H + i * ITEM_H - scrollPx;

            if (i == lc) {
                // "Loading..." footer row
                g.setColor(COLOR_HEADER_BG);
                g.fillRect(0, y, w, ITEM_H);
                g.setFont(SUBNAME_FONT);
                g.setColor(COLOR_SUBNAME);
                g.drawString("Loading...", textX, y + PAD, Graphics.LEFT | Graphics.TOP);
                continue;
            }

            boolean sel = (i == selectedIndex);
            if (sel) {
                g.setColor(COLOR_SELECTED);
                g.fillRect(0, y, w, ITEM_H);
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y, ACCENT_W, ITEM_H);
                int ovf = Math.max(
                    NAME_FONT.stringWidth(names[i])   - availW,
                    SUBNAME_FONT.stringWidth(subs[i]) - availW);
                mqMaxOvf = ovf > 0 ? ovf + 16 : 0;
            }

            // Name
            g.setFont(NAME_FONT);
            g.setColor(COLOR_NAME);
            if (sel && NAME_FONT.stringWidth(names[i]) > availW) {
                int sx = g.getClipX(), sy2 = g.getClipY(), sw = g.getClipWidth(), sh = g.getClipHeight();
                g.setClip(textX, y + PAD, availW, NAME_FONT.getHeight());
                g.drawString(names[i], textX - mqOffset, y + PAD, Graphics.LEFT | Graphics.TOP);
                g.setClip(sx, sy2, sw, sh);
            } else {
                g.drawString(clip(names[i], NAME_FONT, availW), textX, y + PAD,
                             Graphics.LEFT | Graphics.TOP);
            }

            // Subname
            g.setFont(SUBNAME_FONT);
            g.setColor(COLOR_SUBNAME);
            int subY = y + PAD + NAME_FONT.getHeight();
            if (sel && SUBNAME_FONT.stringWidth(subs[i]) > availW) {
                int sx = g.getClipX(), sy2 = g.getClipY(), sw = g.getClipWidth(), sh = g.getClipHeight();
                g.setClip(textX, subY, availW, SUBNAME_FONT.getHeight());
                g.drawString(subs[i], textX - mqOffset, subY, Graphics.LEFT | Graphics.TOP);
                g.setClip(sx, sy2, sw, sh);
            } else {
                g.drawString(clip(subs[i], SUBNAME_FONT, availW), textX, subY,
                             Graphics.LEFT | Graphics.TOP);
            }

            // Divider
            g.setColor(COLOR_DIVIDER);
            g.drawLine(PAD, y + ITEM_H - 1, w - PAD, y + ITEM_H - 1);
        }

        // Scroll bar
        int totalH = totalRows * ITEM_H;
        if (totalH > listH) {
            int barH = Math.max(8, listH * listH / totalH);
            int barY = TITLE_BAR_H + (listH - barH) * scrollPx / Math.max(1, totalH - listH);
            g.setColor(0x3A3A3C);
            g.fillRect(w - 3, TITLE_BAR_H, 3, listH);
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
                if (nokiaMenuOpen) { nokiaMenuOpen = false; executeNokiaMenuItem(nokiaMenuSel); }
                else               { nokiaMenuOpen = true;  nokiaMenuSel = 0; }
                repaint();
                return;
            }
            if (keyCode == -7) {
                if (nokiaMenuOpen) { nokiaMenuOpen = false; repaint(); }
                else if (backAction != null) backAction.run();
                return;
            }
            if (nokiaMenuOpen) {
                int menuSize = 1 + contextCount;
                int action   = getGameAction(keyCode);
                if      (action == UP   && nokiaMenuSel > 0)             { nokiaMenuSel--; repaint(); }
                else if (action == DOWN && nokiaMenuSel < menuSize - 1)  { nokiaMenuSel++; repaint(); }
                else if (action == FIRE || keyCode == -5) {
                    nokiaMenuOpen = false;
                    executeNokiaMenuItem(nokiaMenuSel);
                    repaint();
                }
                return;
            }
        }
        int action = getGameAction(keyCode);
        if      (action == UP)                       moveUp();
        else if (action == DOWN)                     moveDown();
        else if (action == FIRE || keyCode == -5)    fireSelection();
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

    private void moveUp() {
        if (selectedIndex > 0) {
            selectedIndex--;
            mqReset();
            ensureVisible();
            repaint();
        }
    }

    private void moveDown() {
        int lc;
        synchronized (this) { lc = loadedCount; }
        if (selectedIndex < lc - 1) {
            selectedIndex++;
            mqReset();
            ensureVisible();
            repaint();
        }
    }

    private void ensureVisible() {
        int skH   = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
        int listH = getHeight() - TITLE_BAR_H - skH;
        if (listH < ITEM_H) return;
        int absY = selectedIndex * ITEM_H;
        if (absY < scrollPx) {
            scrollPx = absY;
        } else if (absY + ITEM_H > scrollPx + listH) {
            scrollPx = absY + ITEM_H - listH;
        }
        if (scrollPx < 0) scrollPx = 0;
        maybeLoadMore();
    }

    /** Trigger next-page load if selection is within LOAD_AHEAD of the last loaded item. */
    private synchronized void maybeLoadMore() {
        if (nextUrl == null || loadScheduled) return;
        if (selectedIndex < loadedCount - LOAD_AHEAD) return;
        loadScheduled = true;
        loadingMore   = true;
        final String url = nextUrl;
        new Thread(new Runnable() {
            public void run() { dataSource.loadNextPage(LazyList.this, url); }
        }).start();
    }

    private void fireSelection() {
        if (itemListener == null) return;
        int lc;
        synchronized (this) { lc = loadedCount; }
        if (selectedIndex < 0 || selectedIndex >= lc) return;
        itemListener.onItemSelected(selectedIndex, types[selectedIndex],
            names[selectedIndex], subs[selectedIndex], actions[selectedIndex]);
    }

    private void executeNokiaMenuItem(int index) {
        if (index == 0) {
            fireSelection();
        } else {
            int ci = index - 1;
            if (ci < contextCount && contextListener != null) {
                int lc;
                synchronized (this) { lc = loadedCount; }
                if (selectedIndex >= 0 && selectedIndex < lc) {
                    contextListener.onContextAction(
                        selectedIndex, types[selectedIndex],
                        names[selectedIndex], subs[selectedIndex],
                        actions[selectedIndex], contextTags[ci]);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Nokia soft-key bars
    // -------------------------------------------------------------------------

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUBNAME_FONT);
        int labelY = barY + (skH - SUBNAME_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(COLOR_NAME);    g.drawString("Select",  PAD,      labelY, Graphics.LEFT  | Graphics.TOP);
            g.setColor(COLOR_SUBNAME); g.drawString("Close",   w - PAD,  labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(COLOR_NAME);    g.drawString("Options", PAD,      labelY, Graphics.LEFT  | Graphics.TOP);
            g.setColor(COLOR_SUBNAME); g.drawString("Back",    w - PAD,  labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h, int skH) {
        int menuSize = 1 + contextCount;
        int itemH    = SUBNAME_FONT.getHeight() + 6;
        int menuH    = itemH * menuSize + PAD * 2;
        int menuY    = h - skH - menuH;

        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, menuY, w, menuH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, menuY, w, menuY);

        paintMenuItem(g, w, menuY, 0, itemH, "Select");
        for (int i = 0; i < contextCount; i++)
            paintMenuItem(g, w, menuY, i + 1, itemH, contextLabels[i]);
    }

    private void paintMenuItem(Graphics g, int w, int menuY, int idx, int itemH, String label) {
        int y = menuY + PAD + idx * itemH;
        if (idx == nokiaMenuSel) {
            g.setColor(COLOR_ACCENT);
            g.fillRect(0, y - 2, w, itemH);
            g.setColor(COLOR_BG);
        } else {
            g.setColor(COLOR_NAME);
        }
        g.setFont(SUBNAME_FONT);
        g.drawString(label, PAD, y, Graphics.LEFT | Graphics.TOP);
    }

    // -------------------------------------------------------------------------
    // Marquee
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
    // Commands  (non-Nokia)
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_SELECT) {
            fireSelection();
        } else if (c == CMD_BACK && backAction != null) {
            backAction.run();
        } else {
            // Context command
            for (int i = 0; i < contextCount; i++) {
                // Commands registered via addContextItem are not tracked here
                // because addContextItem stores only label/tag; non-Nokia context
                // commands would need a separate Command reference array.
                // For now, context commands on non-Nokia fire through the Nokia menu path.
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }
}
