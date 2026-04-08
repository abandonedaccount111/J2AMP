package com.amplayer.ui;

import com.amplayer.utils.Settings;
import java.util.Enumeration;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Font;
import javax.microedition.lcdui.Graphics;

/**
 * Full-screen file browser Canvas.
 *
 * Starts at the filesystem roots, lets the user navigate into directories
 * and select a .json file.  Only directories and .json files are shown.
 *
 * D-pad:  UP/DOWN scroll selection,  FIRE / Select command = enter dir or pick file,
 *         BACK = go up one level (at root → cancelled).
 */
public class FileBrowser extends Canvas implements CommandListener {

    // -------------------------------------------------------------------------
    // Callback
    // -------------------------------------------------------------------------

    public interface Listener {
        /** Called on the calling thread when a file is chosen. */
        void onFileSelected(String fileUrl);
        void onCancelled();
    }

    // -------------------------------------------------------------------------
    // Colors / fonts
    // -------------------------------------------------------------------------

    private static final int COLOR_BG      = 0x000000;
    private static final int COLOR_HEADER  = 0x111111;
    private static final int COLOR_DIVIDER = 0x2C2C2E;
    private static final int COLOR_ACCENT  = 0xFA2D48;
    private static final int COLOR_TEXT1   = 0xFFFFFF;
    private static final int COLOR_TEXT2   = 0x8E8E93;
    private static final int COLOR_SEL_BG  = 0x1C1C1E;
    private static final int COLOR_DIR     = 0x64D2FF;  // teal for directories
    private static final int COLOR_FILE    = 0x30D158;  // green for .json files

    private static final Font HDR_FONT  = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_BOLD,  Font.SIZE_MEDIUM);
    private static final Font ITEM_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_MEDIUM);
    private static final Font PATH_FONT = Font.getFont(Font.FACE_PROPORTIONAL, Font.STYLE_PLAIN, Font.SIZE_SMALL);

    private static final int PAD = 8;

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    private static final Command CMD_BACK   = new Command("Back",   Command.BACK, 1);
    private static final Command CMD_SELECT = new Command("Select", Command.OK,   1);

    // -------------------------------------------------------------------------
    // State
    // -------------------------------------------------------------------------

    private final Display  display;
    private final Listener listener;

    private boolean nokiaMenuOpen = false;
    private int     nokiaMenuSel  = 0;

    /** Current directory URL (e.g. "file:///E:/tokens/"), null = root listing. */
    private String currentPath  = null;
    private Vector entries      = new Vector();   // String: name; dirs end with "/"
    private int    selectedIdx  = 0;
    private int    scrollOffset = 0;
    private String loadError    = null;

    // ── Touch ──────────────────────────────────────────────────────────────
    private int  startY_T      = -1;
    private int  startOffset_T = 0;
    private boolean isDragging_T = false;
    private long pressTime_T   = 0;

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public FileBrowser(Display display, Listener listener) {
        this.display  = display;
        this.listener = listener;

        setFullScreenMode(true);

        loadDir(null);
    }

    // -------------------------------------------------------------------------
    // Directory loading
    // -------------------------------------------------------------------------

    private void loadDir(String dirUrl) {
        currentPath  = dirUrl;
        entries      = new Vector();
        selectedIdx  = 0;
        scrollOffset = 0;
        loadError    = null;

        if (dirUrl == null) {
            // Enumerate filesystem roots
            try {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements())
                    entries.addElement((String) roots.nextElement()); // e.g. "E:/"
            } catch (Exception e) {
                loadError = e.getMessage();
            }
        } else {
            FileConnection fc = null;
            try {
                fc = (FileConnection) Connector.open(dirUrl, Connector.READ);
                Enumeration list = fc.list();
                Vector dirs  = new Vector();
                Vector files = new Vector();
                while (list.hasMoreElements()) {
                    String name = (String) list.nextElement();
                    if (name.endsWith("/")) {
                        dirs.addElement(name);
                    } else if (name.length() >= 5 &&
                               name.substring(name.length() - 5).toLowerCase().equals(".json")) {
                        files.addElement(name);
                    }
                }
                // Directories first, then .json files, each group alphabetically
                sortVector(dirs);
                sortVector(files);
                for (int i = 0; i < dirs.size();  i++) entries.addElement(dirs.elementAt(i));
                for (int i = 0; i < files.size(); i++) entries.addElement(files.elementAt(i));
            } catch (Exception e) {
                loadError = "Error: " + e.getMessage();
            } finally {
                if (fc != null) try { fc.close(); } catch (Exception ignored) {}
            }
        }
        repaint();
    }

    /** Bubble-sort a Vector<String> in place (small lists only). */
    private static void sortVector(Vector v) {
        int n = v.size();
        for (int i = 0; i < n - 1; i++) {
            for (int j = 0; j < n - i - 1; j++) {
                String a = (String) v.elementAt(j);
                String b = (String) v.elementAt(j + 1);
                if (a.compareTo(b) > 0) {
                    v.setElementAt(b, j);
                    v.setElementAt(a, j + 1);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Navigation
    // -------------------------------------------------------------------------

    private void selectCurrent() {
        if (entries.isEmpty()) return;
        String name = (String) entries.elementAt(selectedIdx);
        if (name.endsWith("/")) {
            // Enter directory
            String newPath = (currentPath == null)
                ? "file:///" + name
                : currentPath + name;
            loadDir(newPath);
        } else {
            // File selected — notify listener
            listener.onFileSelected(currentPath + name);
        }
    }

    private void goUp() {
        if (currentPath == null) {
            listener.onCancelled();
            return;
        }
        // Strip trailing "/"
        String p    = currentPath.substring(0, currentPath.length() - 1);
        int    base = "file:///".length();
        int    last = p.lastIndexOf('/');
        if (last < base) {
            loadDir(null);  // back to roots
        } else {
            loadDir(p.substring(0, last + 1));
        }
    }

    private void ensureVisible() {
        int h       = getHeight();
        int hdrH    = HDR_FONT.getHeight() + PATH_FONT.getHeight() + PAD * 3;
        int listH   = h - hdrH;
        int itemH   = ITEM_FONT.getHeight() + PAD;
        if (itemH <= 0) return;
        int visible = listH / itemH;
        if (visible < 1) visible = 1;
        if (selectedIdx < scrollOffset)
            scrollOffset = selectedIdx;
        else if (selectedIdx >= scrollOffset + visible)
            scrollOffset = selectedIdx - visible + 1;
    }

    // -------------------------------------------------------------------------
    // Key input
    // -------------------------------------------------------------------------

    protected void keyPressed(int keyCode) {
        if (keyCode == -6) { // Options
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
        if (keyCode == -7) { // Back
            if (nokiaMenuOpen) {
                nokiaMenuOpen = false;
                repaint();
            } else {
                goUp();
            }
            return;
        }
        if (nokiaMenuOpen) {
            int action = getGameAction(keyCode);
            if (action == UP && nokiaMenuSel > 0) {
                nokiaMenuSel--;
                repaint();
            } else if (action == DOWN && nokiaMenuSel < 1) { // Only "Select" in nokia menu for now
                nokiaMenuSel++;
                repaint();
            } else if (action == FIRE || keyCode == -5) {
                nokiaMenuOpen = false;
                executeNokiaMenuItem(nokiaMenuSel);
                repaint();
            }
            return;
        }
        int n      = entries.size();
        int action = getGameAction(keyCode);
        if (action == UP && selectedIdx > 0) {
            selectedIdx--;
            ensureVisible();
            repaint();
        } else if (action == DOWN && selectedIdx < n - 1) {
            selectedIdx++;
            ensureVisible();
            repaint();
        } else if (action == FIRE || keyCode == -5) {
            selectCurrent();
        }
    }

    private void executeNokiaMenuItem(int index) {
        if (index == 0) selectCurrent();
    }

    // -------------------------------------------------------------------------
    // Touch events
    // -------------------------------------------------------------------------

    protected void pointerPressed(int x, int y) {
        startY_T = y;
        startOffset_T = scrollOffset;
        isDragging_T = false;
        pressTime_T = System.currentTimeMillis();
    }

    protected void pointerDragged(int x, int y) {
        if (nokiaMenuOpen) return;
        int itemH = ITEM_FONT.getHeight() + PAD;
        if (itemH <= 0) return;
        if (Math.abs(y - startY_T) > 5) {
            isDragging_T = true;
            int deltaIdx = (startY_T - y) / itemH;
            int newScroll = startOffset_T + deltaIdx;
            
            int hdrH = HDR_FONT.getHeight() + PATH_FONT.getHeight() + PAD * 3;
            int h = getHeight();
            int skH = ITEM_FONT.getHeight() + PAD * 2;
            int listH = h - hdrH - skH;
            int visible = listH / itemH;
            int maxScroll = Math.max(0, entries.size() - visible);
            
            if (newScroll < 0) newScroll = 0;
            if (newScroll > maxScroll) newScroll = maxScroll;
            
            if (scrollOffset != newScroll) {
                scrollOffset = newScroll;
                repaint();
            }
        }
    }

    protected void pointerReleased(int x, int y) {
        if (!isDragging_T && (System.currentTimeMillis() - pressTime_T) < 300) {
            int h = getHeight();
            int w = getWidth();
            int hdrH = HDR_FONT.getHeight() + PATH_FONT.getHeight() + PAD * 3;
            int skH = ITEM_FONT.getHeight() + PAD * 2;

            if (nokiaMenuOpen) {
                int itemH = ITEM_FONT.getHeight() + 6;
                int menuH = itemH * 1 + PAD * 2; // only "Select"
                int menuY = h - skH - menuH;
                if (y >= menuY && y <= menuY + menuH) {
                    nokiaMenuOpen = false;
                    executeNokiaMenuItem(0);
                    repaint();
                } else if (y > h - skH) {
                    if (x > w / 2) { nokiaMenuOpen = false; repaint(); }
                    else { nokiaMenuOpen = false; executeNokiaMenuItem(0); repaint(); }
                } else {
                    nokiaMenuOpen = false; repaint();
                }
                return;
            }

            if (y > h - skH) {
                if (x > w / 2) goUp();
                else { nokiaMenuOpen = true; nokiaMenuSel = 0; repaint(); }
                return;
            }

            if (y >= hdrH && y < h - skH) {
                int itemH = ITEM_FONT.getHeight() + PAD;
                int clickedIdx = scrollOffset + (y - hdrH) / itemH;
                if (clickedIdx >= 0 && clickedIdx < entries.size()) {
                    selectedIdx = clickedIdx;
                    repaint();
                    selectCurrent();
                }
            }
        }
    }

    protected void keyRepeated(int keyCode) { keyPressed(keyCode); }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if      (c == CMD_BACK)   goUp();
        else if (c == CMD_SELECT) selectCurrent();
    }

    // -------------------------------------------------------------------------
    // Paint
    // -------------------------------------------------------------------------

    protected void paint(Graphics g) {
        int w = getWidth();
        int h = getHeight();

        // Header
        int hdrH = HDR_FONT.getHeight() + PATH_FONT.getHeight() + PAD * 3;
        int skH  = ITEM_FONT.getHeight() + PAD * 2;
        
        g.setColor(COLOR_BG);
        g.fillRect(0, 0, w, h);

        g.setColor(COLOR_HEADER);
        g.fillRect(0, 0, w, hdrH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, hdrH, w, hdrH);

        g.setFont(HDR_FONT);
        g.setColor(COLOR_TEXT1);
        g.drawString("Select Token File", w / 2, PAD, Graphics.HCENTER | Graphics.TOP);

        // Current path breadcrumb
        String pathLabel = (currentPath == null) ? "/ (roots)" : currentPath.substring("file:///".length());
        g.setFont(PATH_FONT);
        g.setColor(COLOR_TEXT2);
        g.drawString(clipPath(pathLabel, w - PAD * 2),
                     PAD, PAD + HDR_FONT.getHeight() + 2, Graphics.LEFT | Graphics.TOP);

        int listTop = hdrH + 1;
        int listH   = h - listTop;
        int itemH   = ITEM_FONT.getHeight() + PAD;

        // Clip list area
        int cx = g.getClipX(), cy = g.getClipY(), cw = g.getClipWidth(), ch = g.getClipHeight();
        g.setClip(0, listTop, w, listH);

        if (loadError != null) {
            g.setFont(PATH_FONT);
            g.setColor(0xFF3B30);
            g.drawString(loadError, PAD, listTop + PAD, Graphics.LEFT | Graphics.TOP);
        } else if (entries.isEmpty()) {
            g.setFont(ITEM_FONT);
            g.setColor(COLOR_TEXT2);
            g.drawString("(empty)", w / 2, listTop + listH / 2, Graphics.HCENTER | Graphics.BASELINE);
        } else {
            int visible = listH / itemH + 2;
            int end     = Math.min(entries.size(), scrollOffset + visible);

            for (int i = scrollOffset; i < end; i++) {
                String  name  = (String) entries.elementAt(i);
                boolean isDir = name.endsWith("/");
                boolean sel   = (i == selectedIdx);
                int     y     = listTop + (i - scrollOffset) * itemH;

                if (sel) {
                    g.setColor(COLOR_SEL_BG);
                    g.fillRect(0, y, w, itemH);
                    g.setColor(COLOR_ACCENT);
                    g.fillRect(0, y, 3, itemH);
                }

                g.setFont(ITEM_FONT);
                g.setColor(isDir ? COLOR_DIR : COLOR_FILE);
                String prefix = isDir ? "\u25B6 " : "  ";  // right-pointing triangle for dirs
                g.drawString(prefix + clip(name, ITEM_FONT, w - PAD * 2 - 8),
                             PAD + 4, y + PAD / 2, Graphics.LEFT | Graphics.TOP);

                g.setColor(COLOR_DIVIDER);
                g.drawLine(PAD, y + itemH - 1, w - PAD, y + itemH - 1);
            }

            // Scrollbar
            int n = entries.size();
            if (n > 0 && listH > 0) {
                int barH = Math.max(8, listH * Math.min(n, visible) / n);
                int barY = listTop + (listH - barH) * scrollOffset / Math.max(1, n - (listH / itemH));
                g.setColor(COLOR_DIVIDER);
                g.fillRect(w - 3, listTop, 3, listH);
                g.setColor(COLOR_ACCENT);
                g.fillRect(w - 3, barY, 3, barH);
            }
        }

        g.setClip(cx, cy, cw, ch);

        drawSoftKeyBar(g, w, h, skH);
        if (nokiaMenuOpen) drawNokiaMenu(g, w, h, skH);
    }

    private void drawSoftKeyBar(Graphics g, int w, int h, int skH) {
        int barY = h - skH;
        g.setColor(COLOR_HEADER);
        g.fillRect(0, barY, w, skH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, barY, w, barY);
        g.setFont(PATH_FONT);
        int labelY = barY + (skH - PATH_FONT.getHeight()) / 2;
        if (nokiaMenuOpen) {
            g.setColor(COLOR_TEXT1);
            g.drawString("Select", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_TEXT2);
            g.drawString("Close", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        } else {
            g.setColor(COLOR_TEXT1);
            g.drawString("Options", PAD, labelY, Graphics.LEFT | Graphics.TOP);
            g.setColor(COLOR_TEXT2);
            g.drawString("Back", w - PAD, labelY, Graphics.RIGHT | Graphics.TOP);
        }
    }

    private void drawNokiaMenu(Graphics g, int w, int h, int skH) {
        int itemH = ITEM_FONT.getHeight() + 6;
        int menuH = itemH * 1 + PAD * 2;
        int menuY = h - skH - menuH;

        g.setColor(COLOR_HEADER);
        g.fillRect(0, menuY, w, menuH);
        g.setColor(COLOR_DIVIDER);
        g.drawLine(0, menuY, w, menuY);

        int y = menuY + PAD;
        if (nokiaMenuSel == 0) {
            g.setColor(COLOR_ACCENT);
            g.fillRect(0, y - 2, w, itemH);
            g.setColor(COLOR_BG);
        } else {
            g.setColor(COLOR_TEXT1);
        }
        g.setFont(PATH_FONT);
        g.drawString("Select", PAD, y, Graphics.LEFT | Graphics.TOP);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static String clip(String text, Font font, int maxW) {
        if (font.stringWidth(text) <= maxW) return text;
        while (text.length() > 1 && font.stringWidth(text + "...") > maxW)
            text = text.substring(0, text.length() - 1);
        return text + "...";
    }

    /** Clip path from the left (keep the rightmost part). */
    private static String clipPath(String path, int maxW) {
        Font f = PATH_FONT;
        if (f.stringWidth(path) <= maxW) return path;
        while (path.length() > 1 && f.stringWidth("..." + path) > maxW)
            path = path.substring(1);
        return "..." + path;
    }
}
