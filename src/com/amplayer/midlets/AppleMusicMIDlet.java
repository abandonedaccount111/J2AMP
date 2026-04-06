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
import com.amplayer.utils.Settings;
import com.amplayer.utils.TokenStore;
import java.util.Hashtable;
import javax.microedition.lcdui.Displayable;

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
        Settings.load();
        PlaybackManager.setCacheSize(Settings.cacheMb);
        PlaybackManager.clearCache();   // remove any leftover files from a previous session

        String[] tokens = TokenStore.load();
        if (tokens != null) {
            // Tokens already in RMS — go straight to the main menu
            api = new AMAPI(tokens[0], tokens[1]);
            initMainScreens();
            display.setCurrent(mainMenu);
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
        final Form loading = new Form("Search");
        loading.append(new StringItem("", "Loading..."));
        display.setCurrent(loading);
        new Thread(new Runnable() {
            public void run() {
                try {
                    AMAPI a = getAPI();
                    display.setCurrent(new SearchForm(
                        AppleMusicMIDlet.this, display, a, a.getStorefront()));
                } catch (Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    loading.append(new StringItem("Error: ", msg));
                }
            }
        }).start();
    }

    public void showSongs() {
        showLibraryItems("Songs", "/v1/me/library/songs", "song");
    }

    public void showAlbums() {
        showLibraryItems("Albums", "/v1/me/library/albums", "album");
    }

    public void showPlaylist() {
        showLibraryItems("Playlists", "/v1/me/library/playlists", "playlist");
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

    private void showLibraryItems(final String title,
                                  final String endpoint,
                                  final String itemType) {
        final Form loading = new Form(title);
        loading.append(new StringItem("", "Loading..."));
        display.setCurrent(loading);

        new Thread(new Runnable() {
            public void run() {
                try {
                    final AMAPI a = getAPI();
                    Hashtable   params = new Hashtable();
                    params.put("limit", String.valueOf(Settings.queryLimit));
                    params.put("l",     a.getStorefrontLanguage());
                    JSONObject   resp = a.APIRequest(endpoint, params, "GET", null, null);
                    final PageData first = parseLibraryPage(resp, itemType);

                    // holder[0] is set after LazyList construction so the
                    // SelectionListener can reference the list object.
                    final LazyList[] holder = new LazyList[1];

                    LazyList.DataSource ds = new LazyList.DataSource() {
                        public void loadNextPage(LazyList list, String nextUrl) {
                            try {
                                JSONObject r  = a.APIRequest(nextUrl, null, "GET", null, null);
                                PageData   pd = parseLibraryPage(r, itemType);
                                list.appendItems(pd.types, pd.names, pd.subs, pd.actions, pd.next);
                            } catch (Exception e) {
                                list.setLoadError();
                            }
                        }
                    };

                    LazyList.SelectionListener sl = new LazyList.SelectionListener() {
                        public void onItemSelected(int index, String type,
                                                   String name, String sub,
                                                   BaseAction action) {
                            if ("play".equals(action.type)) {
                                // Build queue from all currently loaded songs
                                LazyList l = holder[0];
                                int n = (l != null) ? l.getLoadedCount() : 0;
                                String[] ids     = new String[n];
                                String[] qNames  = new String[n];
                                String[] artists = new String[n];
                                int startIndex   = 0;
                                String artUrl    = action.extra;
                                for (int i = 0; i < n; i++) {
                                    BaseAction ai = (l != null) ? l.getLoadedAction(i) : null;
                                    ids[i]     = (ai != null) ? ai.details : "";
                                    qNames[i]  = (l != null) ? l.getLoadedName(i) : "";
                                    artists[i] = (l != null) ? l.getLoadedSub(i)  : "";
                                    if (i == index) {
                                        startIndex = i;
                                        if (ai != null && ai.extra.length() > 0) artUrl = ai.extra;
                                    }
                                }
                                playQueue(ids, qNames, artists, artUrl, startIndex);
                            } else {
                                onLibraryItemSelected(name, sub, action, true);
                            }
                        }
                    };

                    final LazyList list = new LazyList(title, ds, sl);
                    holder[0] = list;

                    list.appendItems(first.types, first.names, first.subs,
                                     first.actions, first.next);
                    list.setBackAction(new Runnable() {
                        public void run() { display.setCurrent(mainMenu); }
                    });
                    display.setCurrent(list);

                } catch (Exception e) {
                    final String msg = e.getMessage() != null ? e.getMessage() : e.toString();
                    loading.append(new StringItem("Error: ", msg));
                }
            }
        }).start();
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
