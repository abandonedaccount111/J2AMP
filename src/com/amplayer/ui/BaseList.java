package com.amplayer.ui;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import com.amplayer.utils.Settings;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Scrollable dynamic list driven by a JSONArray.
 *
 * Each element must be a JSONObject with at least "type" and "name".
 * Normal items:  { "type": "song"|"album"|"playlist", "name": "", "subname": "", "action": {...} }
 * Section header: { "type": "header", "name": "Section Title" }
 *
 * Headers are rendered as a compact accent-coloured label and are skipped
 * during keyboard navigation / selection.
 */
public class BaseList extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Callbacks
    // -------------------------------------------------------------------------

    public interface SelectionListener {
        void onItemSelected(int index, String type, String name,
                            String subname, BaseAction action);
    }

    /** Called when a registered context command fires on the selected item. */
    public interface ContextListener {
        void onContextAction(int index, String type, String name,
                             String subname, BaseAction action, String contextAction);
    }

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    private static final int COLOR_BG       = 0x000000;
    private static final int COLOR_SELECTED = 0x1C1C1E;
    private static final int COLOR_ACCENT   = 0xFA2D48;
    private static final int COLOR_NAME     = 0xFFFFFF;
    private static final int COLOR_SUBNAME  = 0x8E8E93;
    private static final int COLOR_DIVIDER  = 0x2C2C2E;
    private static final int COLOR_HEADER_BG = 0x111111;

    // -------------------------------------------------------------------------
    // Fonts & layout
    // -------------------------------------------------------------------------

    private static final Font NAME_FONT    = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font SUBNAME_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);
    private static final Font TITLE_FONT   = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_LARGE);
    private static final Font HEADER_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_SMALL);

    private static final int PAD         = 8;
    private static final int ACCENT_W    = 3;
    private static final int TITLE_BAR_H = TITLE_FONT.getHeight() + PAD * 2;
    private static final int ITEM_H      = NAME_FONT.getHeight() + SUBNAME_FONT.getHeight() + PAD * 2;
    private static final int SECTION_H   = HEADER_FONT.getHeight() + PAD;  // compact section header

    // -------------------------------------------------------------------------
    // Data
    // -------------------------------------------------------------------------

    private String[]     types;
    private String[]     names;
    private String[]     subnames;
    private BaseAction[] actions;
    private int[]        itemHeights;  // ITEM_H or SECTION_H per entry
    private int[]        itemYPos;     // cumulative y (itemYPos[i] = top of item i)
    private int          count;

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private int selectedIndex = 0;
    private int scrollPx      = 0;   // pixels scrolled from content top

    private final SelectionListener listener;
    private Runnable                backAction;
    private ContextListener         contextListener;
    /** Maps Command → String contextAction tag. */
    private final Hashtable         contextCommandMap = new Hashtable();

    // ── Marquee (selected row only) ──────────────────────────────────────────
    private static final int MQ_SPEED  = 2;
    private static final int MQ_PAUSE  = 20;
    private int  mqOffset  = 0;
    private int  mqPause   = MQ_PAUSE;
    private int  mqMaxOvf  = 0;
    private volatile boolean mqRunning = false;

    // -------------------------------------------------------------------------
    // Commands  (used only on non-Nokia devices)
    // -------------------------------------------------------------------------

    private static final Command CMD_SELECT = new Command("Select", Command.OK,   1);
    private static final Command CMD_BACK   = new Command("Back",   Command.BACK, 1);

    // -------------------------------------------------------------------------
    // Nokia soft-key menu
    // -------------------------------------------------------------------------

    private final boolean isNokia;
    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;
    /** Ordered context items for nokia menu: each element is String[]{label, tag}. */
    private final Vector nokiaContextItems = new Vector();

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public BaseList(String title, JSONArray items, SelectionListener listener) {
        super();
        setTitle(title);
        this.listener = listener;
        loadItems(items);
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
    // Data loading
    // -------------------------------------------------------------------------

    private void loadItems(JSONArray items) {
        count    = items.size();
        types    = new String[count];
        names    = new String[count];
        subnames = new String[count];
        actions  = new BaseAction[count];
        itemHeights = new int[count];
        itemYPos    = new int[count + 1];

        itemYPos[0] = 0;
        for (int i = 0; i < count; i++) {
            JSONObject item = items.getObject(i);
            types[i]    = item.getString("type",    "");
            names[i]    = item.getString("name",    "");
            subnames[i] = item.getString("subname", "");
            JSONObject actionObj = item.getObject("action", null);
            actions[i]  = BaseAction.fromJSON(actionObj);

            itemHeights[i] = "header".equals(types[i]) ? SECTION_H : ITEM_H;
            itemYPos[i + 1] = itemYPos[i] + itemHeights[i];
        }
    }

    /** Replace the list contents and refresh. */
    public void setItems(JSONArray items) {
        loadItems(items);
        selectedIndex = 0;
        skipHeadersForward();
        scrollPx = 0;
        repaint();
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

        // Title bar
        g.setColor(0x111111);
        g.fillRect(0, 0, w, TITLE_BAR_H);
        g.setFont(TITLE_FONT);
        g.setColor(COLOR_NAME);
        g.drawString(getTitle(), PAD, PAD, Graphics.LEFT | Graphics.TOP);
        g.setColor(COLOR_ACCENT);
        g.fillRect(0, TITLE_BAR_H - 2, w, 2);

        // Clip list area so items cannot overdraw the title bar
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, TITLE_BAR_H, w, listH);

        // Items
        for (int i = 0; i < count; i++) {
            int absY = itemYPos[i];
            int y    = TITLE_BAR_H + absY - scrollPx;
            int ih   = itemHeights[i];

            if (y + ih <= TITLE_BAR_H) continue; // above viewport
            if (y >= h)               break;     // below viewport

            if ("header".equals(types[i])) {
                // Section header
                g.setColor(COLOR_HEADER_BG);
                g.fillRect(0, y, w, ih);
                g.setColor(COLOR_ACCENT);
                g.fillRect(0, y + ih - 1, w, 1);  // bottom accent line
                g.setFont(HEADER_FONT);
                g.setColor(COLOR_ACCENT);
                g.drawString(names[i].toUpperCase(), PAD, y + PAD / 2, Graphics.LEFT | Graphics.TOP);
            } else {
                boolean sel = (i == selectedIndex);
                // Normal item — selected highlight
                if (sel) {
                    g.setColor(COLOR_SELECTED);
                    g.fillRect(0, y, w, ih);
                    g.setColor(COLOR_ACCENT);
                    g.fillRect(0, y, ACCENT_W, ih);
                }
                int textX  = PAD + ACCENT_W + 4;
                int availW = w - textX - PAD - 3; // -3 for scroll bar

                if (sel) {
                    // Compute max overflow for the two text lines
                    int ovf = Math.max(
                        NAME_FONT.stringWidth(names[i])    - availW,
                        SUBNAME_FONT.stringWidth(subnames[i]) - availW);
                    mqMaxOvf = ovf > 0 ? ovf + 16 : 0;
                }

                // Name
                g.setFont(NAME_FONT);
                g.setColor(COLOR_NAME);
                if (sel && NAME_FONT.stringWidth(names[i]) > availW) {
                    int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                    g.setClip(textX, y + PAD, availW, NAME_FONT.getHeight());
                    g.drawString(names[i], textX - mqOffset, y + PAD, Graphics.LEFT | Graphics.TOP);
                    g.setClip(scx, scy, scw, sch);
                } else {
                    g.drawString(clip(names[i], NAME_FONT, availW), textX, y + PAD,
                                 Graphics.LEFT | Graphics.TOP);
                }

                // Subname
                g.setFont(SUBNAME_FONT);
                g.setColor(COLOR_SUBNAME);
                int subY = y + PAD + NAME_FONT.getHeight();
                if (sel && SUBNAME_FONT.stringWidth(subnames[i]) > availW) {
                    int scx = g.getClipX(), scy = g.getClipY(), scw = g.getClipWidth(), sch = g.getClipHeight();
                    g.setClip(textX, subY, availW, SUBNAME_FONT.getHeight());
                    g.drawString(subnames[i], textX - mqOffset, subY, Graphics.LEFT | Graphics.TOP);
                    g.setClip(scx, scy, scw, sch);
                } else {
                    g.drawString(clip(subnames[i], SUBNAME_FONT, availW), textX, subY,
                                 Graphics.LEFT | Graphics.TOP);
                }

                // Divider
                g.setColor(COLOR_DIVIDER);
                g.drawLine(PAD, y + ih - 1, w - PAD, y + ih - 1);
            }
        }

        // Scroll indicator (right edge) — still within clip so no need to change
        int totalH = itemYPos[count];
        if (totalH > listH) {
            int barH = Math.max(8, listH * listH / totalH);
            int barY = TITLE_BAR_H + (listH - barH) * scrollPx / Math.max(1, totalH - listH);
            g.setColor(0x3A3A3C);
            g.fillRect(w - 3, TITLE_BAR_H, 3, listH);
            g.setColor(COLOR_ACCENT);
            g.fillRect(w - 3, barY, 3, barH);
        }

        // Restore clip
        g.setClip(cx, cy, cw, ch);
        if (isNokia) { drawSoftKeyBar(g, w, h, skH); if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH); }
    }

    // -------------------------------------------------------------------------
    // Key handling
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
                } else if (backAction != null) {
                    backAction.run();
                }
                return;
            }
            if (nokiaMenuOpen) {
                int menuSize = 1 + nokiaContextItems.size();
                int action = getGameAction(keyCode);
                if (action == UP && nokiaMenuSel > 0) {
                    nokiaMenuSel--;
                    repaint();
                } else if (action == DOWN && nokiaMenuSel < menuSize - 1) {
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
        int action = getGameAction(keyCode);
        if      (action == UP)   moveUp();
        else if (action == DOWN) moveDown();
        else if (action == FIRE || keyCode == -5) fireSelection();
    }

    private void executeNokiaMenuItem(int index) {
        if (index == 0) {
            fireSelection();
        } else {
            int ci = index - 1;
            if (ci < nokiaContextItems.size() && contextListener != null
                    && selectedIndex >= 0 && selectedIndex < count
                    && !"header".equals(types[selectedIndex])) {
                String tag = ((String[]) nokiaContextItems.elementAt(ci))[1];
                contextListener.onContextAction(
                    selectedIndex, types[selectedIndex],
                    names[selectedIndex], subnames[selectedIndex],
                    actions[selectedIndex], tag);
            }
        }
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(SUBNAME_FONT);
        int labelY = barY + (skH - SUBNAME_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(COLOR_NAME);
            g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_SUBNAME);
            g.drawString("Close", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(COLOR_NAME);
            g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_SUBNAME);
            g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h, int skH) {
        int menuSize = 1 + nokiaContextItems.size();
        int itemH    = SUBNAME_FONT.getHeight() + 6;
        int menuH    = itemH * menuSize + PAD * 2;
        int menuY    = h - skH - menuH;

        g.setColor(COLOR_HEADER_BG);
        g.fillRect(0, menuY, w, menuH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, menuY, w, menuY);

        // Item 0: Select
        paintNokiaMenuItem(g, w, menuY, 0, itemH, "Select");
        // Context items
        for (int i = 0; i < nokiaContextItems.size(); i++) {
            String label = ((String[]) nokiaContextItems.elementAt(i))[0];
            paintNokiaMenuItem(g, w, menuY, i + 1, itemH, label);
        }
    }

    private void paintNokiaMenuItem(Graphics g, int w, int menuY, int idx, int itemH, String label) {
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

    protected void keyRepeated(int keyCode) {
        keyPressed(keyCode);
    }

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
                    mqTick();
                    repaint();
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
        while (prev >= 0 && "header".equals(types[prev])) prev--;
        if (prev >= 0) {
            selectedIndex = prev;
            mqReset();
            ensureVisible();
            repaint();
        }
    }

    private void moveDown() {
        int next = selectedIndex + 1;
        while (next < count && "header".equals(types[next])) next++;
        if (next < count) {
            selectedIndex = next;
            mqReset();
            ensureVisible();
            repaint();
        }
    }

    /** Scroll so selectedIndex is fully in view. */
    private void ensureVisible() {
        int skH   = isNokia ? SUBNAME_FONT.getHeight() + PAD * 2 : 0;
        int listH = getHeight() - TITLE_BAR_H - skH;
        int absY  = itemYPos[selectedIndex];
        int ih    = itemHeights[selectedIndex];
        if (absY < scrollPx) {
            scrollPx = absY;
        } else if (absY + ih > scrollPx + listH) {
            scrollPx = absY + ih - listH;
        }
    }

    /** Advance selectedIndex past any leading header items. */
    private void skipHeadersForward() {
        while (selectedIndex < count - 1 && "header".equals(types[selectedIndex])) {
            selectedIndex++;
        }
    }

    // -------------------------------------------------------------------------
    // Selection
    // -------------------------------------------------------------------------

    private void fireSelection() {
        if (listener != null && selectedIndex >= 0 && selectedIndex < count
                && !"header".equals(types[selectedIndex])) {
            listener.onItemSelected(
                selectedIndex,
                types[selectedIndex],
                names[selectedIndex],
                subnames[selectedIndex],
                actions[selectedIndex]
            );
        }
    }

    /** Set a callback invoked when the Back command is fired. */
    public void setBackAction(Runnable action) {
        this.backAction = action;
    }

    public void setContextListener(ContextListener l) {
        this.contextListener = l;
    }

    /**
     * Register an extra command that, when fired, calls
     * {@link ContextListener#onContextAction} with the given tag for the
     * currently selected item.
     */
    public void addContextCommand(Command cmd, String actionTag) {
        if (isNokia) {
            nokiaContextItems.addElement(new String[]{ cmd.getLabel(), actionTag });
        } else {
            contextCommandMap.put(cmd, actionTag);
            addCommand(cmd);
        }
    }

    private static String clip(String text, Font font, int maxW) {
        if (text == null || text.length() == 0) return "";
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_SELECT) {
            fireSelection();
        } else if (c == CMD_BACK && backAction != null) {
            backAction.run();
        } else {
            String tag = (String) contextCommandMap.get(c);
            if (tag != null && contextListener != null
                    && selectedIndex >= 0 && selectedIndex < count
                    && !"header".equals(types[selectedIndex])) {
                contextListener.onContextAction(
                    selectedIndex,
                    types[selectedIndex],
                    names[selectedIndex],
                    subnames[selectedIndex],
                    actions[selectedIndex],
                    tag);
            }
        }
    }
}
