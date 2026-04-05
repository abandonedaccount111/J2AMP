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
        showLibraryItems("Songs", "/v1/me/library/songs", false);
    }

    public void showAlbums() {
        final Form loading = new Form("Albums");
        loading.append(new StringItem("", "Loading..."));
        display.setCurrent(loading);

        new Thread(new Runnable() {
            public void run() {
                try {
                    AMAPI a = getAPI();
                    Hashtable params = new Hashtable();
                    params.put("limit", "100");
                    params.put("l",     a.getStorefrontLanguage());
                    JSONObject resp = a.APIRequest("/v1/me/library/albums", params, "GET", null, null);
                    JSONArray  data = resp.getArray("data", null);

                    final JSONArray items = new JSONArray();
                    if (data != null) {
                        for (int i = 0; i < data.size(); i++) {
                            JSONObject resource = data.getObject(i);
                            JSONObject attrs    = resource.getObject("attributes", null);
                            if (attrs == null) continue;

                            String id      = resource.getString("id", "");
                            String name    = attrs.getString("name",       "Unknown");
                            String artist  = attrs.getString("artistName", "");
                            String artUrl  = extractArtUrl(attrs);

                            JSONObject actionObj = new JSONObject();
                            actionObj.put("type",    "open_album");
                            actionObj.put("details", id);
                            actionObj.put("extra",   artUrl);

                            JSONObject itemObj = new JSONObject();
                            itemObj.put("type",    "album");
                            itemObj.put("name",    name);
                            itemObj.put("subname", artist);
                            itemObj.put("action",  actionObj);
                            items.add(itemObj);
                        }
                    }

                    BaseList list = new BaseList("Albums", items,
                        new BaseList.SelectionListener() {
                            public void onItemSelected(int index, String type,
                                    String name, String subname, BaseAction action) {
                                if ("open_album".equals(action.type)) {
                                    onLibraryItemSelected(name, subname, action, true);
                                }
                            }
                        });
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

    public void showPlaylist() {
        showLibraryItems("Playlists", "/v1/me/library/playlists", true);
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
    // Library list loader (songs or playlists)
    // =========================================================================

    private void showLibraryItems(final String title,
                                  final String endpoint,
                                  final boolean isPlaylists) {
        final Form loading = new Form(title);
        loading.append(new StringItem("", "Loading..."));
        display.setCurrent(loading);

        new Thread(new Runnable() {
            public void run() {
                try {
                    AMAPI a = getAPI();
                    Hashtable params = new Hashtable();
                    params.put("limit", "100");
                    params.put("l",     a.getStorefrontLanguage());
                    JSONObject resp = a.APIRequest(endpoint, params, "GET", null, null);
                    JSONArray  data = resp.getArray("data", null);

                    final JSONArray items     = new JSONArray();
                    // Parallel song arrays for queue-all-songs behaviour
                    final java.util.Vector songItems = new java.util.Vector();

                    if (data != null) {
                        for (int i = 0; i < data.size(); i++) {
                            JSONObject resource = data.getObject(i);
                            String id   = resource.getString("id", "");
                            JSONObject attrs = resource.getObject("attributes", null);
                            if (attrs == null) continue;

                            String name   = attrs.getString("name", "Unknown");
                            String artUrl = extractArtUrl(attrs);

                            JSONObject actionObj = new JSONObject();
                            JSONObject itemObj   = new JSONObject();

                            if (isPlaylists) {
                                actionObj.put("type",    "open_playlist");
                                actionObj.put("details", id);
                                actionObj.put("extra",   artUrl);
                                itemObj.put("type",    "playlist");
                                itemObj.put("name",    name);
                                itemObj.put("subname", "");
                                itemObj.put("action",  actionObj);
                            } else {
                                // Resolve the salable catalog ID from playParams
                                String playId = id;
                                JSONObject pp = attrs.getObject("playParams", null);
                                if (pp != null) {
                                    String catId = pp.getString("catalogId", "");
                                    if (catId.length() > 0) {
                                        playId = catId;
                                    } else {
                                        String ppId = pp.getString("id", "");
                                        if (ppId.length() > 0 && !ppId.startsWith("i."))
                                            playId = ppId;
                                    }
                                }
                                actionObj.put("type",    "play");
                                actionObj.put("details", playId);
                                actionObj.put("extra",   artUrl);
                                itemObj.put("type",    "song");
                                itemObj.put("name",    name);
                                itemObj.put("subname", attrs.getString("artistName", ""));
                                itemObj.put("action",  actionObj);

                                // Track for queue-all
                                JSONObject si = new JSONObject();
                                si.put("id",     playId);
                                si.put("name",   name);
                                si.put("artist", attrs.getString("artistName", ""));
                                si.put("art",    artUrl);
                                songItems.addElement(si);
                            }
                            items.add(itemObj);
                        }
                    }

                    final boolean lib = isPlaylists; // effectively final for inner class
                    BaseList list = new BaseList(title, items,
                        new BaseList.SelectionListener() {
                            public void onItemSelected(int index, String type,
                                    String name, String subname, BaseAction action) {
                                if ("play".equals(action.type)) {
                                    int n = songItems.size();
                                    String[] ids     = new String[n];
                                    String[] names   = new String[n];
                                    String[] artists = new String[n];
                                    int startIndex   = 0;
                                    String artUrl    = action.extra;
                                    for (int i = 0; i < n; i++) {
                                        JSONObject si = (JSONObject) songItems.elementAt(i);
                                        ids[i]     = si.getString("id",     "");
                                        names[i]   = si.getString("name",   "");
                                        artists[i] = si.getString("artist", "");
                                        if (ids[i].equals(action.details)) {
                                            startIndex = i;
                                            artUrl     = si.getString("art", artUrl);
                                        }
                                    }
                                    playQueue(ids, names, artists, artUrl, startIndex);
                                } else {
                                    onLibraryItemSelected(name, subname, action, lib);
                                }
                            }
                        });
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
