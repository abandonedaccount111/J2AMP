package com.amplayer.midlets;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Vector;
import com.amplayer.playback.PlaybackManager;
import com.amplayer.api.AMAPI;
import com.amplayer.ui.ArtistView;
import com.amplayer.ui.BaseAction;
import com.amplayer.ui.BaseList;
import com.amplayer.ui.DetailView;
import com.amplayer.ui.LazyList;
import com.amplayer.ui.LastFmScrobbler;
import com.amplayer.ui.NowPlayingScreen;
import com.amplayer.ui.SearchForm;
import com.amplayer.ui.SettingsForm;
import com.amplayer.ui.TokenSetupForm;
import com.amplayer.ui.VisualizerCanvas;
import com.amplayer.utils.LibraryDb;
import com.amplayer.utils.LibraryDb.DbResult;
import com.amplayer.utils.Settings;
import com.amplayer.utils.TokenStore;
import java.util.Hashtable;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Gauge;
import javax.microedition.lcdui.TextBox;
import javax.microedition.lcdui.TextField;



/**
 * Root MIDlet for the Apple Music J2ME player.
 *
 * Navigation wiring:
 *   MainMenu  →  showNowPlaying / showSearch / showSongs / showPlaylist
 *   SearchForm / LibraryView  →  onSearchItemSelected / onLibraryItemSelected
 *   DetailView  →  playQueue   (track selected inside album or playlist)
 */
public class AppleMusicMIDlet extends MIDlet {

    private Display           display;
    private MainMenu          mainMenu;
    private AMAPI             api;              // lazily initialized
    private PlaybackManager   playbackManager;  // created eagerly in startApp
    private NowPlayingScreen  nowPlayingScreen; // created eagerly in startApp
    private LastFmScrobbler   scrobbler;

    // Source playlist context — set when playing from a playlist DetailView
    private String sourcePlaylistId;
    private String sourcePlaylistName;
    private String sourcePlaylistArtUrl;

    // =========================================================================
    // MIDlet lifecycle
    // =========================================================================

    protected void startApp() throws MIDletStateChangeException {
        display = Display.getDisplay(this);
        
        // if (Settings.getSupportedMp4ContentType() == null) {
        //     final Alert alert = new Alert(
        //         "Unsupported Device", "Your device is missing audio/mp4 playback support required by Apple Music.", 
        //         null, AlertType.ERROR);
        //     alert.setTimeout(Alert.FOREVER);
        //     alert.setCommandListener(new CommandListener() {
        //         public void commandAction(Command c, Displayable d) {
        //             notifyDestroyed();
        //         }
        //     });
        //     display.setCurrent(alert);
        //     return;
        // }

        Settings.load();
        PlaybackManager.setCacheSize(Settings.cacheMb);
        PlaybackManager.clearCache();   // remove any leftover files from a previous session

        String[] tokens = TokenStore.load();
        if (tokens != null) {
            // Tokens already in RMS — go straight to the main menu
            api = new AMAPI(tokens[0], tokens[1]);
            initMainScreens();
            display.setCurrent(mainMenu);
            syncLibrary(false, new Runnable() {
                public void run() { display.setCurrent(mainMenu); }
            });
        } else {
            // First run (or after reset) — show the token setup form
            display.setCurrent(new TokenSetupForm(this, display));
        }
    }

    /** Called by TokenSetupForm once tokens have been validated and saved. */
    public void onTokensReady(final String devToken, final String userToken) {
        display.callSerially(new Runnable() {
            public void run() {
                api = new AMAPI(devToken, userToken);
                initMainScreens();
                display.setCurrent(mainMenu);
                syncLibrary(false, new Runnable() {
                    public void run() { display.setCurrent(mainMenu); }
                });
            }
        });
    }

    private void initMainScreens() {
        mainMenu         = new MainMenu(this, display);
        playbackManager  = new PlaybackManager();
        mainMenu.setPlaybackManager(playbackManager);
        nowPlayingScreen = new NowPlayingScreen(playbackManager, display, mainMenu, this);
        // Chain scrobbler as a listener after NowPlayingScreen if signed into Last.fm
        if (Settings.lastFmSk.length() > 0) {
            initScrobbler();
        }
    }

    private void initScrobbler() {
        // NowPlayingScreen registered itself as the PM listener in its constructor.
        // Wrap it so scrobbler events arrive too.
        PlaybackManager.Listener current = playbackManager.getListener();
        scrobbler = new LastFmScrobbler(playbackManager, current);
        playbackManager.setListener(scrobbler);
    }

    protected void pauseApp() {
        playbackManager.pause();
    }

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
        playbackManager.close();
    }

    // =========================================================================
    // Navigation — called by MainMenu
    // =========================================================================

    public void showNowPlaying() {
        display.setCurrent(nowPlayingScreen);
    }

    public void showSearch() {
        final javax.microedition.lcdui.Alert loading = new javax.microedition.lcdui.Alert("Search", "Loading...", null, javax.microedition.lcdui.AlertType.INFO);
        loading.setTimeout(javax.microedition.lcdui.Alert.FOREVER);
        loading.setIndicator(new javax.microedition.lcdui.Gauge(null, false, javax.microedition.lcdui.Gauge.INDEFINITE, javax.microedition.lcdui.Gauge.CONTINUOUS_RUNNING));
        final javax.microedition.lcdui.Command searchCancel = new javax.microedition.lcdui.Command("Cancel", javax.microedition.lcdui.Command.CANCEL, 1);
        loading.addCommand(searchCancel);
        loading.setCommandListener(new javax.microedition.lcdui.CommandListener() {
            public void commandAction(javax.microedition.lcdui.Command c, javax.microedition.lcdui.Displayable d) {
                if (c == searchCancel) display.setCurrent(mainMenu);
            }
        });
        display.setCurrent(loading);
        new Thread(new Runnable() {
            public void run() {
                try {
                    AMAPI a = getAPI();
                    display.setCurrent(new SearchForm(
                        AppleMusicMIDlet.this, display, a, a.getStorefront()));
                } catch (Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    loading.setString("Error: " + msg);
                    loading.setType(javax.microedition.lcdui.AlertType.ERROR);
                }
            }
        }).start();
    }

    public void syncLibrary(final boolean force, final Runnable onComplete) {
        if (!force) {
            boolean hasSongs = com.amplayer.utils.LibraryDb.isValid("songs");
            boolean hasAlbums = com.amplayer.utils.LibraryDb.isValid("albums");
            boolean hasPlaylists = com.amplayer.utils.LibraryDb.isValid("playlists");
            if (hasSongs && hasAlbums && hasPlaylists) {
                if (onComplete != null) display.callSerially(onComplete);
                return;
            }
        }

        final Alert loading = new Alert("Library Sync", "Downloading library...", null, AlertType.INFO);
        loading.setTimeout(Alert.FOREVER);
        loading.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
        
        final Command cancelCmd = new Command("Cancel", Command.CANCEL, 1);
        loading.addCommand(cancelCmd);
        loading.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c == cancelCmd) {
                    if (onComplete != null) display.callSerially(onComplete);
                }
            }
        });
        display.setCurrent(loading);

        new Thread(new Runnable() {
            public void run() {
                try {
                    String[] endpoints = {"/v1/me/library/songs", "/v1/me/library/albums", "/v1/me/library/playlists"};
                    String[] itemTypes = {"song", "album", "playlist"};
                    String[] dbTypes = {"songs", "albums", "playlists"};
                    
                    AMAPI a = getAPI();
                    java.util.Hashtable params = new java.util.Hashtable();
                    int qLimit = Settings.queryLimit > 0 ? Settings.queryLimit : 100;
                    params.put("limit", String.valueOf(Math.min(qLimit, 100)));
                    params.put("l", a.getStorefrontLanguage());
                    
                    for (int dbIdx = 0; dbIdx < 3; dbIdx++) {
                        loading.setString("Downloading " + dbTypes[dbIdx] + "...");
                        int max = Settings.getMaxItemSize();
                        java.util.Vector ids = new java.util.Vector();
                        java.util.Vector names = new java.util.Vector();
                        java.util.Vector subs = new java.util.Vector();
                        
                        String nextUrl = endpoints[dbIdx];
                        while (nextUrl != null && ids.size() < max) {
                            cc.nnproject.json.JSONObject r = a.APIRequest(nextUrl, nextUrl.equals(endpoints[dbIdx]) ? params : null, "GET", null, null);
                            PageData pd = parseLibraryPage(r, itemTypes[dbIdx]);
                            if (pd.names.length == 0) break;
                            for (int i = 0; i < pd.names.length; i++) {
                                if (ids.size() >= max) break;
                                ids.addElement(pd.playIds[i]);
                                names.addElement(pd.names[i]);
                                subs.addElement(pd.subs[i]);
                            }
                            nextUrl = pd.next;
                        }
                        com.amplayer.utils.LibraryDb.save(dbTypes[dbIdx], ids, names, subs);
                    }
                    
                    loading.setString("Sync Complete!");
                    loading.setType(AlertType.INFO);
                    loading.setIndicator(null);
                    loading.removeCommand(cancelCmd);
                    loading.setTimeout(2000);
                    new Thread(new Runnable() {
                        public void run() {
                            try { Thread.sleep(2000); } catch (Exception ignored) {}
                            if (onComplete != null) display.callSerially(onComplete);
                        }
                    }).start();
                } catch (Exception e) {
                    loading.setString("Sync Error: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                    loading.setType(AlertType.ERROR);
                    loading.setIndicator(null);
                    loading.removeCommand(cancelCmd);
                    loading.setTimeout(3000);
                    new Thread(new Runnable() {
                        public void run() {
                            try { Thread.sleep(3000); } catch (Exception ignored) {}
                            if (onComplete != null) display.callSerially(onComplete);
                        }
                    }).start();
                }
            }
        }).start();
    }

    public void showSongs() {
        showLibraryItems("Songs", "/v1/me/library/songs", "song", "songs", false);
    }

    public void showAlbums() {
        showLibraryItems("Albums", "/v1/me/library/albums", "album", "albums", false);
    }

    public void showPlaylist() {
        showLibraryItems("Playlists", "/v1/me/library/playlists", "playlist", "playlists", false);
    }

    public void showSettings() {
        display.setCurrent(new SettingsForm(this, display, mainMenu, playbackManager));
    }

    public void showVisualizer(Displayable backScreen) {
        VisualizerCanvas viz = new VisualizerCanvas(display, backScreen);
        viz.setTrackInfo(playbackManager.getCurrentName(), playbackManager.getCurrentArtist());
        display.setCurrent(viz);
    }

    /** Called by SettingsForm after successful Last.fm login. */
    public void onLastFmSignedIn(String sessionKey) {
        if (scrobbler == null) initScrobbler();
    }

    /** Called by SettingsForm after Last.fm logout. */
    public void onLastFmSignedOut() {
        if (scrobbler != null) {
            scrobbler.stop();
            scrobbler = null;
            // Restore NowPlayingScreen as the direct PM listener
            playbackManager.setListener(nowPlayingScreen);
        }
    }

    public MainMenu getMainMenu()          { return mainMenu; }
    public PlaybackManager getPlaybackManager() { return playbackManager; }

    // Source playlist accessors (read by NowPlayingScreen)
    public String getSourcePlaylistId()     { return sourcePlaylistId;     }
    public String getSourcePlaylistName()   { return sourcePlaylistName;   }
    public String getSourcePlaylistArtUrl() { return sourcePlaylistArtUrl; }

    // =========================================================================
    // Library list loader — songs, albums, or playlists
    // itemType: "song" | "album" | "playlist"
    // =========================================================================

    /**
     * Parsed data for one page of library items.
     * All arrays are parallel and the same length.
     */
    private static final class PageData {
        String[]     types;
        String[]     names;
        String[]     subs;      // artist name (songs/albums) or "" (playlists)
        String[]     playIds;   // catalog/play ID — only meaningful for songs
        BaseAction[] actions;
        String       next;      // next-page URL from response, null if last page
    }

    /**
     * Parse one page of Apple Music library response into flat arrays.
     * Works for songs, albums, and playlists.
     */
    private PageData parseLibraryPage(JSONObject resp, String itemType) {
        JSONArray data = resp != null ? resp.getArray("data", null) : null;
        String    next = resp != null ? resp.getString("next", null) : null;
        int n = (data != null) ? data.size() : 0;

        PageData pd    = new PageData();
        pd.types   = new String[n];
        pd.names   = new String[n];
        pd.subs    = new String[n];
        pd.playIds = new String[n];
        pd.actions = new BaseAction[n];
        pd.next    = next;

        for (int i = 0; i < n; i++) {
            JSONObject resource = data.getObject(i);
            String     id       = resource.getString("id", "");
            JSONObject attrs    = resource.getObject("attributes", null);
            String     name     = (attrs != null) ? attrs.getString("name", "Unknown") : "Unknown";
            String     artUrl   = extractArtUrl(attrs);

            if ("song".equals(itemType)) {
                // Resolve the salable catalog ID from playParams
                String playId = id;
                if (attrs != null) {
                    JSONObject pp = attrs.getObject("playParams", null);
                    if (pp != null) {
                        String catId = pp.getString("catalogId", "");
                        if (catId.length() > 0) {
                            playId = catId;
                        } else {
                            String ppId = pp.getString("id", "");
                            if (ppId.length() > 0 && !ppId.startsWith("i.")) playId = ppId;
                        }
                    }
                }
                String artist  = (attrs != null) ? attrs.getString("artistName", "") : "";
                pd.types[i]   = "song";
                pd.names[i]   = name;
                pd.subs[i]    = artist;
                pd.playIds[i] = playId;
                pd.actions[i] = new BaseAction("play", playId, artUrl);

            } else if ("album".equals(itemType)) {
                String artist  = (attrs != null) ? attrs.getString("artistName", "") : "";
                pd.types[i]   = "album";
                pd.names[i]   = name;
                pd.subs[i]    = artist;
                pd.playIds[i] = id;
                pd.actions[i] = new BaseAction("open_album", id, artUrl);

            } else { // playlist
                pd.types[i]   = "playlist";
                pd.names[i]   = name;
                pd.subs[i]    = "";
                pd.playIds[i] = id;
                pd.actions[i] = new BaseAction("open_playlist", id, artUrl);
            }
        }
        return pd;
    }

    private void showLibraryItems(final String title, final String endpoint, final String itemType, final String dbType, final boolean forceReload) {
        if (!forceReload && com.amplayer.utils.LibraryDb.isValid(dbType)) {
            new Thread(new Runnable() {
                public void run() {
                    com.amplayer.utils.LibraryDb.DbResult res = com.amplayer.utils.LibraryDb.read(dbType, null);
                    if (res != null) {
                        displayCachedLibraryItems(title, itemType, res, dbType, endpoint);
                    } else {
                        showLibraryItems(title, endpoint, itemType, dbType, true);
                    }
                }
            }).start();
            return;
        }

        final Alert loading = new Alert(
            title, 
            forceReload ? "Refreshing Database..." : "Loading...", 
            null, 
            AlertType.INFO);
        loading.setTimeout(Alert.FOREVER);
        loading.setIndicator(new Gauge(null, false, Gauge.INDEFINITE, Gauge.CONTINUOUS_RUNNING));
        final Command libCancel = new Command("Cancel", Command.CANCEL, 1);
        loading.addCommand(libCancel);
        loading.setCommandListener(new CommandListener() {
            public void commandAction(javax.microedition.lcdui.Command c, javax.microedition.lcdui.Displayable d) {
                if (c == libCancel) display.setCurrent(mainMenu);
            }
        });
        display.setCurrent(loading);

        new Thread(new Runnable() {
            public void run() {
                try {
                    int max = Settings.getMaxItemSize();
                    Vector ids = new Vector();
                    Vector names = new Vector();
                    Vector subs = new Vector();
                    Vector actions = new Vector();
                    
                    final AMAPI a = getAPI();
                    Hashtable params = new Hashtable();
                    int qLimit = Settings.queryLimit > 0 ? Settings.queryLimit : 100;
                    params.put("limit", String.valueOf(Math.min(qLimit, 100)));
                    params.put("l", a.getStorefrontLanguage());
                    
                    String nextUrl = endpoint;
                    while (nextUrl != null && ids.size() < max) {
                        JSONObject r = a.APIRequest(nextUrl, nextUrl.equals(endpoint) ? params : null, "GET", null, null);
                        PageData pd = parseLibraryPage(r, itemType);
                        if (pd.names.length == 0) break;
                        for (int i = 0; i < pd.names.length; i++) {
                            if (ids.size() >= max) break;
                            ids.addElement(pd.playIds[i]);
                            names.addElement(pd.names[i]);
                            subs.addElement(pd.subs[i]);
                        }
                        nextUrl = pd.next;
                    }
                    
                    com.amplayer.utils.LibraryDb.save(dbType, ids, names, subs);
                    
                    com.amplayer.utils.LibraryDb.DbResult res = new com.amplayer.utils.LibraryDb.DbResult();
                    res.length = ids.size();
                    res.ids = new String[res.length];
                    res.titles = new String[res.length];
                    res.subnames = new String[res.length];
                    for (int i = 0; i < res.length; i++) {
                        res.ids[i] = (String) ids.elementAt(i);
                        res.titles[i] = (String) names.elementAt(i);
                        res.subnames[i] = (String) subs.elementAt(i);
                    }
                    displayCachedLibraryItems(title, itemType, res, dbType, endpoint);
                } catch (Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    loading.setString("Error: " + msg);
                    loading.setType(javax.microedition.lcdui.AlertType.ERROR);
                }
            }
        }).start();
    }

    private void displayCachedLibraryItems(final String title, final String itemType, final com.amplayer.utils.LibraryDb.DbResult res, final String dbType, final String endpoint) {
        final LazyList[] holder = new LazyList[1];
        
        LazyList.SelectionListener sl = new LazyList.SelectionListener() {
            public void onItemSelected(int index, String type, String name, String sub, BaseAction action) {
                if ("play".equals(action.type)) {
                    LazyList l = holder[0];
                    int n = l.getLoadedCount();
                    String[] ids = new String[n];
                    String[] qNames = new String[n];
                    String[] artists = new String[n];
                    String artUrl = action.extra;
                    for (int i = 0; i < n; i++) {
                        BaseAction ai = l.getLoadedAction(i);
                        ids[i] = ai != null ? ai.details : "";
                        qNames[i] = l.getLoadedName(i);
                        artists[i] = l.getLoadedSub(i);
                    }
                    playQueue(ids, qNames, artists, artUrl, index);
                } else {
                    onLibraryItemSelected(name, sub, action, true);
                }
            }
        };
        
        final LazyList list = new LazyList(title, new LazyList.DataSource() {
            public void loadNextPage(LazyList l, String n) {} // no-op since all loaded
        }, sl);
        holder[0] = list;
        
        String[] types = new String[res.length];
        BaseAction[] actionsArr = new BaseAction[res.length];
        for (int i = 0; i < res.length; i++) {
            types[i] = itemType;
            if ("song".equals(itemType)) {
                actionsArr[i] = new BaseAction("play", res.ids[i], "");
            } else if ("album".equals(itemType)) {
                actionsArr[i] = new BaseAction("open_album", res.ids[i], "");
            } else {
                actionsArr[i] = new BaseAction("open_playlist", res.ids[i], "");
            }
        }
        
        list.appendItems(types, res.titles, res.subnames, actionsArr, null);
        list.setBackAction(new Runnable() {
            public void run() { display.setCurrent(mainMenu); }
        });
        list.setRefreshAction(new Runnable() {
            public void run() { showLibraryItems(title, endpoint, itemType, dbType, true); }
        });
        list.setSearchListener(new LazyList.SearchListener() {
            public void onSearchChanged(final String query) {
                new Thread(new Runnable() {
                    public void run() {
                        String q = query.trim();
                        if (q.length() == 0) q = null;
                        final DbResult res2 = LibraryDb.read(dbType, q);
                        display.callSerially(new Runnable() {
                            public void run() {
                                if (res2 != null) {
                                    String[] types2 = new String[res2.length];
                                    BaseAction[] actionsArr2 = new BaseAction[res2.length];
                                    for (int i = 0; i < res2.length; i++) {
                                        types2[i] = itemType;
                                        if ("song".equals(itemType)) actionsArr2[i] = new BaseAction("play", res2.ids[i], "");
                                        else if ("album".equals(itemType)) actionsArr2[i] = new BaseAction("open_album", res2.ids[i], "");
                                        else actionsArr2[i] = new BaseAction("open_playlist", res2.ids[i], "");
                                    }
                                    list.setItems(types2, res2.titles, res2.subnames, actionsArr2);
                                }
                            }
                        });
                    }
                }).start();
            }
        });

        final TextBox searchBox = new TextBox("Filter " + title, "", 64, TextField.ANY);
        searchBox.addCommand(new Command("Filter", Command.OK, 1));
        searchBox.addCommand(new Command("Cancel", Command.BACK, 2));
        searchBox.setCommandListener(new CommandListener() {
            public void commandAction(Command c, Displayable d) {
                if (c.getCommandType() == Command.OK) {
                    final String userQ = searchBox.getString().trim();
                    list.setInlineSearchQuery(userQ);
                    list.setTitle(title + " (Filtering...)");
                    display.setCurrent(list);
                    new Thread(new Runnable() {
                        public void run() {
                            String q = userQ.length() == 0 ? null : userQ;
                            final com.amplayer.utils.LibraryDb.DbResult res2 = com.amplayer.utils.LibraryDb.read(dbType, q);
                            display.callSerially(new Runnable() {
                                public void run() {
                                    if (res2 != null) {
                                        String[] types2 = new String[res2.length];
                                        BaseAction[] actionsArr2 = new BaseAction[res2.length];
                                        for (int i = 0; i < res2.length; i++) {
                                            types2[i] = itemType;
                                            if ("song".equals(itemType)) actionsArr2[i] = new BaseAction("play", res2.ids[i], "");
                                            else if ("album".equals(itemType)) actionsArr2[i] = new BaseAction("open_album", res2.ids[i], "");
                                            else actionsArr2[i] = new BaseAction("open_playlist", res2.ids[i], "");
                                        }
                                        list.setTitle(title);
                                        list.setItems(types2, res2.titles, res2.subnames, actionsArr2);
                                    } else {
                                        list.setTitle(title);
                                        display.setCurrent(list);
                                    }
                                }
                            });
                        }
                    }).start();
                } else {
                    display.setCurrent(list);
                }
            }
        });
        
        list.setSearchAction(new Runnable() {
            public void run() {
                searchBox.setString(list.getInlineSearchQuery());
                display.setCurrent(searchBox);
            }
        });
        display.setCurrent(list);
    }

    private void onLibraryItemSelected(final String name, final String subname,
                                        final BaseAction action, final boolean isLibrary) {
        boolean isPlaylist = "open_playlist".equals(action.type);
        boolean isAlbum    = "open_album".equals(action.type);
        if (isPlaylist || isAlbum) {
            final boolean playlist = isPlaylist;
            new Thread(new Runnable() {
                public void run() {
                    try {
                        AMAPI a = getAPI();
                        DetailView detail = new DetailView(
                            action.details, playlist, isLibrary,
                            name, subname, action.extra,
                            a, a.getStorefront(), display, display.getCurrent(),
                            AppleMusicMIDlet.this);
                        display.setCurrent(detail);
                    } catch (Exception ignored) {}
                }
            }).start();
        }
    }

    // =========================================================================
    // Search callback — called by SearchForm for songs; albums/playlists open
    // DetailView directly inside SearchForm itself.
    // =========================================================================

    public void onSearchItemSelected(String type, String name,
                                     String subname, BaseAction action) {
        if ("play".equals(action.type)) {
            // Single song selected from search results — play it directly.
            playQueue(
                new String[]{ action.details },
                new String[]{ name },
                new String[]{ subname },
                action.extra, 0);
        }
    }

    // =========================================================================
    // Playback queue — called by DetailView or song selections
    // =========================================================================

    /**
     * Shows Now Playing immediately, then loads credentials (once) and starts
     * the queue on a background thread so the UI is never blocked.
     * Clears any source-playlist context.
     */
    public void playQueue(final String[] trackIds, final String[] trackNames,
                          final String[] trackArtists, final String artUrlTemplate,
                          final int startIndex) {
        sourcePlaylistId     = null;
        sourcePlaylistName   = null;
        sourcePlaylistArtUrl = null;
        startPlayQueueInternal(trackIds, trackNames, trackArtists, artUrlTemplate, startIndex);
    }

    /**
     * Like {@link #playQueue} but remembers which playlist the tracks came from,
     * so Now Playing can offer a "Go to Playlist" shortcut.
     */
    public void playQueueFromPlaylist(final String[] trackIds, final String[] trackNames,
                                      final String[] trackArtists, final String artUrlTemplate,
                                      final int startIndex,
                                      String playlistId, String playlistName,
                                      String playlistArtUrl) {
        sourcePlaylistId     = playlistId;
        sourcePlaylistName   = playlistName;
        sourcePlaylistArtUrl = playlistArtUrl;
        startPlayQueueInternal(trackIds, trackNames, trackArtists, artUrlTemplate, startIndex);
    }

    private void startPlayQueueInternal(final String[] trackIds, final String[] trackNames,
                                        final String[] trackArtists, final String artUrlTemplate,
                                        final int startIndex) {
        display.setCurrent(nowPlayingScreen);
        new Thread(new Runnable() {
            public void run() {
                try {
                    if (!playbackManager.hasCredentials()) {
                        byte[] clientId = loadResource("/client_id.bin");
                        byte[] privKey  = loadResource("/private_key.der");
                        AMAPI a = getAPI();
                        playbackManager.setCredentials(clientId, privKey, a);
                        nowPlayingScreen.setAPI(a);  // unlock Lyrics once creds are ready
                    }
                    playbackManager.setQueue(
                        trackIds, trackNames, trackArtists, artUrlTemplate, startIndex);
                } catch (final Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    display.callSerially(new Runnable() {
                        public void run() {
                            Form f = new Form("Playback Error");
                            f.append(new StringItem("Error: ", msg));
                            display.setCurrent(f);
                        }
                    });
                }
            }
        }).start();
    }

    // =========================================================================
    // Navigation helpers — "Go to Artist / Album"
    // =========================================================================

    /**
     * Open an ArtistView for the given artist.
     * The calling screen becomes the back-target.
     */
    public void showArtist(final String artistId, final String artistName) {
        showArtist(artistId, artistName, "");
    }

    public void showArtist(final String artistId, final String artistName,
                           final String artUrl) {
        final Displayable back = display.getCurrent();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final AMAPI a = getAPI();
                    display.callSerially(new Runnable() {
                        public void run() {
                            display.setCurrent(new ArtistView(
                                artistId, artistName, artUrl,
                                a, a.getStorefront(),
                                display, back, AppleMusicMIDlet.this));
                        }
                    });
                } catch (Exception ignored) {}
            }
        }).start();
    }

    /**
     * Open a DetailView for the given catalog album ID.
     * The calling screen becomes the back-target.
     */
    public void showAlbumById(final String albumId, final String albumName,
                              final String artistName, final String artUrl) {
        final Displayable back = display.getCurrent();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final AMAPI a = getAPI();
                    display.callSerially(new Runnable() {
                        public void run() {
                            display.setCurrent(new DetailView(
                                albumId, false, false,
                                albumName, artistName, artUrl,
                                a, a.getStorefront(),
                                display, back, AppleMusicMIDlet.this));
                        }
                    });
                } catch (Exception ignored) {}
            }
        }).start();
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private synchronized AMAPI getAPI() throws Exception {
        if (api == null) {
            String[] tokens = TokenStore.load();
            if (tokens == null) throw new Exception("No tokens stored — please set up tokens first");
            api = new AMAPI(tokens[0], tokens[1]);
        }
        return api;
    }

    private static String extractArtUrl(JSONObject attrs) {
        if (attrs == null) return "";
        JSONObject art = attrs.getObject("artwork", null);
        if (art == null) return "";
        return art.getString("url", "");
    }

    private byte[] loadResource(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) throw new Exception("Resource not found: " + path);
        try { return readAll(is); } finally { closeQuietly(is); }
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    private static void closeQuietly(InputStream in) {
        if (in != null) try { in.close(); } catch (Exception ignored) {}
    }
}
