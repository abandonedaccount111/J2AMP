package com.amplayer.ui;

import cc.nnproject.json.JSONArray;
import cc.nnproject.json.JSONObject;
import com.amplayer.api.AMAPI;
import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.playback.PlaybackManager;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.lcdui.ChoiceGroup;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.lcdui.TextField;

public class SearchForm extends Form implements CommandListener {

    private static final Command CMD_SEARCH = new Command("Search", Command.OK,   1);
    private static final Command CMD_BACK   = new Command("Back",   Command.BACK, 1);

    /** Toggle index: 0 = AM Catalog, 1 = My Library */
    private static final int SRC_CATALOG = 0;
    private static final int SRC_LIBRARY = 1;

    private final AppleMusicMIDlet midlet;
    private final Display          display;
    private final AMAPI            api;
    private final String           storefront;
    private final String           storefrontLanguage;

    private final TextField   searchField;
    private final ChoiceGroup sourceToggle;
    private       StringItem  statusItem;
    private       BaseList    currentResultList;
    /** Flat list of every song item from the last search, used to build a queue. */
    private       Vector      songItems = new Vector();

    // Context commands shown on the search result list
    private static final Command CTX_GO_TO_ARTIST = new Command("Go to Artist", Command.ITEM, 6);
    private static final Command CTX_GO_TO_ALBUM  = new Command("Go to Album",  Command.ITEM, 7);
    private static final Command CTX_PLAY_NEXT    = new Command("Play Next",    Command.ITEM, 8);
    private static final Command CTX_ADD_TO_QUEUE = new Command("Add to Queue", Command.ITEM, 9);

    public SearchForm(AppleMusicMIDlet midlet, Display display,
                      AMAPI api, String storefront) {
        super("Search");
        this.midlet     = midlet;
        this.display    = display;
        this.api        = api;
        this.storefront = storefront;
        this.storefrontLanguage = api.getStorefrontLanguage();

        searchField = new TextField("Query", "", 128, TextField.ANY);
        append(searchField);

        sourceToggle = new ChoiceGroup("Search in:", ChoiceGroup.EXCLUSIVE);
        sourceToggle.append("AM Catalog", null);
        sourceToggle.append("My Library", null);
        sourceToggle.setSelectedIndex(SRC_CATALOG, true);
        append(sourceToggle);

        statusItem = new StringItem("", "");
        append(statusItem);

        addCommand(CMD_SEARCH);
        addCommand(CMD_BACK);
        setCommandListener(this);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BACK) {
            display.setCurrent(midlet.getMainMenu());
        } else if (c == CMD_SEARCH) {
            String query = searchField.getString().trim();
            if (query.length() > 0) {
                doSearch(query, sourceToggle.getSelectedIndex() == SRC_LIBRARY);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Search
    // -------------------------------------------------------------------------

    private void doSearch(final String query, final boolean useLibrary) {
        songItems = new Vector();   // clear previous song queue
        setStatus("Searching...");

        final String endpoint;
        final Hashtable params = new Hashtable();
        params.put("term",     query);
        params.put("limit",    "25");
        params.put("l",        storefrontLanguage);
        params.put("platform", "web");

        if (useLibrary) {
            endpoint = "/v1/me/library/search";
            params.put("types", "library-songs,library-albums,library-playlists");
        } else {
            endpoint = "/v1/catalog/" + storefront + "/search";
            params.put("types", "songs,albums,playlists,artists");
        }

        api.APIRequestAsync(
            endpoint, params, "GET", null,
            new AMAPI.AMAPIListener() {
                public void onResponse(JSONObject result) {
                    try {
                        JSONArray items = parseResults(result, useLibrary);
                        if (items.size() == 0) {
                            setStatus("No results for \"" + query + "\".");
                            return;
                        }
                        showResults(query, items, useLibrary);
                    } catch (Exception e) {
                        setStatus("Parse error: " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                public void onError(Exception e) {
                    setStatus("Error: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        );
    }

    // -------------------------------------------------------------------------
    // Result parsing — returns a sectioned JSONArray (headers + items)
    // -------------------------------------------------------------------------

    private JSONArray parseResults(JSONObject response, boolean useLibrary) {
        JSONObject results = response.getObject("results", null);
        if (results == null) return new JSONArray();

        JSONArray artists   = new JSONArray();
        JSONArray songs     = new JSONArray();
        JSONArray albums    = new JSONArray();
        JSONArray playlists = new JSONArray();

        if (!useLibrary) parseArtists(results, artists);
        parseSongs(results,     songs,     useLibrary);
        parseAlbums(results,    albums,    useLibrary);
        parsePlaylists(results, playlists, useLibrary);

        // Build sectioned output: header + items per non-empty section
        JSONArray out = new JSONArray();
        if (artists.size() > 0) {
            out.add(makeHeader("Artists"));
            appendAll(out, artists);
        }
        if (songs.size() > 0) {
            out.add(makeHeader("Songs"));
            appendAll(out, songs);
        }
        if (albums.size() > 0) {
            out.add(makeHeader("Albums"));
            appendAll(out, albums);
        }
        if (playlists.size() > 0) {
            out.add(makeHeader("Playlists"));
            appendAll(out, playlists);
        }
        return out;
    }

    private void parseArtists(JSONObject results, JSONArray out) {
        JSONObject block = results.getObject("artists", null);
        if (block == null) return;
        JSONArray data = block.getArray("data", null);
        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            JSONObject artist = data.getObject(i);
            JSONObject attrs  = artist.getObject("attributes", null);
            if (attrs == null) continue;

            JSONObject action = new JSONObject();
            action.put("type",    "open_artist");
            action.put("details", artist.getString("id", ""));
            action.put("extra",   extractArtUrl(attrs));  // artwork URL

            JSONObject item = new JSONObject();
            item.put("type",    "artist");
            item.put("name",    attrs.getString("name", "Unknown"));
            String genreStr = "";
            JSONArray genres = attrs.getArray("genreNames", null);
            if (genres != null && genres.size() > 0)
                genreStr = genres.getString(0, "");
            item.put("subname", genreStr);
            item.put("action",  action);
            out.add(item);
        }
    }

    private void parseSongs(JSONObject results, JSONArray out, boolean lib) {
        String key   = lib ? "library-songs" : "songs";
        JSONObject block = results.getObject(key, null);
        if (block == null) return;
        JSONArray data = block.getArray("data", null);
        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            JSONObject song  = data.getObject(i);
            JSONObject attrs = song.getObject("attributes", null);
            if (attrs == null) continue;

            String songId  = song.getString("id", "");
            String name    = attrs.getString("name",       "Unknown");
            String artist  = attrs.getString("artistName", "");
            String artUrl  = extractArtUrl(attrs);

            // Resolve catalog ID for library songs (needed for playback)
            if (lib) {
                JSONObject pp = attrs.getObject("playParams", null);
                if (pp != null) {
                    String catId = pp.getString("catalogId", "");
                    if (catId.length() > 0) songId = catId;
                    else {
                        String ppId = pp.getString("id", "");
                        if (ppId.length() > 0 && !ppId.startsWith("i.")) songId = ppId;
                    }
                }
            }

            JSONObject action = new JSONObject();
            action.put("type",    "play");
            action.put("details", songId);
            action.put("extra",   artUrl);

            JSONObject item = new JSONObject();
            item.put("type",    "song");
            item.put("name",    name);
            item.put("subname", artist);
            item.put("action",  action);
            out.add(item);

            // Track for queue building
            JSONObject si = new JSONObject();
            si.put("id",     songId);
            si.put("name",   name);
            si.put("artist", artist);
            si.put("art",    artUrl);
            songItems.addElement(si);
        }
    }

    private void parseAlbums(JSONObject results, JSONArray out, boolean lib) {
        String key   = lib ? "library-albums" : "albums";
        JSONObject block = results.getObject(key, null);
        if (block == null) return;
        JSONArray data = block.getArray("data", null);
        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            JSONObject album = data.getObject(i);
            JSONObject attrs = album.getObject("attributes", null);
            if (attrs == null) continue;

            JSONObject action = new JSONObject();
            action.put("type",    "open_album");
            action.put("details", album.getString("id", ""));
            action.put("extra",   extractArtUrl(attrs));

            JSONObject item = new JSONObject();
            item.put("type",    "album");
            item.put("name",    attrs.getString("name",       "Unknown"));
            item.put("subname", attrs.getString("artistName", ""));
            item.put("action",  action);
            out.add(item);
        }
    }

    private void parsePlaylists(JSONObject results, JSONArray out, boolean lib) {
        String key   = lib ? "library-playlists" : "playlists";
        JSONObject block = results.getObject(key, null);
        if (block == null) return;
        JSONArray data = block.getArray("data", null);
        if (data == null) return;

        for (int i = 0; i < data.size(); i++) {
            JSONObject pl    = data.getObject(i);
            JSONObject attrs = pl.getObject("attributes", null);
            if (attrs == null) continue;

            String sub = attrs.getString("curatorName", null);
            if (sub == null || sub.length() == 0)
                sub = attrs.getString("description", "");

            JSONObject action = new JSONObject();
            action.put("type",    "open_playlist");
            action.put("details", pl.getString("id", ""));
            action.put("extra",   extractArtUrl(attrs));

            JSONObject item = new JSONObject();
            item.put("type",    "playlist");
            item.put("name",    attrs.getString("name", "Unknown"));
            item.put("subname", sub);
            item.put("action",  action);
            out.add(item);
        }
    }

    private static String extractArtUrl(JSONObject attrs) {
        JSONObject art = attrs.getObject("artwork", null);
        if (art == null) return "";
        return art.getString("url", "");
    }

    // -------------------------------------------------------------------------
    // Results screen
    // -------------------------------------------------------------------------

    private void showResults(final String query, final JSONArray items,
                             final boolean isLibrary) {
        currentResultList = new BaseList(
            "Results: " + query,
            items,
            new BaseList.SelectionListener() {
                public void onItemSelected(int index, String type, String name,
                                           String subname, BaseAction action) {
                    onResultSelected(type, name, subname, action,
                                     currentResultList, isLibrary);
                }
            }
        );

        // Attach context commands for song rows
        currentResultList.addContextCommand(CTX_GO_TO_ARTIST, "go_to_artist");
        currentResultList.addContextCommand(CTX_GO_TO_ALBUM,  "go_to_album");
        currentResultList.addContextCommand(CTX_PLAY_NEXT,    "play_next");
        currentResultList.addContextCommand(CTX_ADD_TO_QUEUE, "add_to_queue");

        currentResultList.setContextListener(new BaseList.ContextListener() {
            public void onContextAction(int index, String type, String name,
                                        String subname, BaseAction action,
                                        String contextAction) {
                onResultContextAction(type, name, subname, action, contextAction);
            }
        });

        currentResultList.setBackAction(new Runnable() {
            public void run() {
                display.setCurrent(SearchForm.this);
            }
        });

        display.setCurrent(currentResultList);
    }

    private void onResultSelected(String type, String name, String subname,
                                   BaseAction action, Displayable fromScreen,
                                   boolean isLibrary) {
        if ("open_album".equals(action.type) || "open_playlist".equals(action.type)) {
            boolean isPlaylist = "open_playlist".equals(action.type);
            DetailView detail  = new DetailView(
                action.details, isPlaylist, isLibrary, name, subname, action.extra,
                api, storefront, display, fromScreen, midlet
            );
            display.setCurrent(detail);
        } else if ("open_artist".equals(action.type)) {
            // action.details = artistId, action.extra = artUrl
            midlet.showArtist(action.details, name, action.extra);
        } else if ("play".equals(action.type)) {
            // Find the clicked song's position within the song list and queue all songs.
            int n          = songItems.size();
            int startIndex = 0;
            String[] ids     = new String[n];
            String[] names   = new String[n];
            String[] artists = new String[n];
            String   artUrl  = action.extra;

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
            midlet.playQueue(ids, names, artists, artUrl, startIndex);
        }
    }

    /**
     * Handle context-menu actions on the currently highlighted search result.
     */
    private void onResultContextAction(String type, String name, String subname,
                                        BaseAction action, String contextAction) {
        if ("go_to_artist".equals(contextAction)) {
            if ("artist".equals(type)) {
                midlet.showArtist(action.details, name);
            } else if ("play".equals(action.type)) {
                // Fetch artist ID for this song on demand
                final String songId     = action.details;
                final String artistName = subname;
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Hashtable params = new Hashtable();
                            params.put("include", "artists");
                            JSONObject resp = api.APIRequest(
                                "/v1/catalog/" + storefront + "/songs/" + songId,
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
                                Display.getDisplay(midlet).callSerially(new Runnable() {
                                    public void run() { midlet.showArtist(artistId, artistName); }
                                });
                            }
                        } catch (Exception ignored) {}
                    }
                }).start();
            }
        } else if ("go_to_album".equals(contextAction)) {
            if ("play".equals(action.type)) {
                final String songId     = action.details;
                final String artistName = subname;
                new Thread(new Runnable() {
                    public void run() {
                        try {
                            Hashtable params = new Hashtable();
                            params.put("include", "albums");
                            JSONObject resp = api.APIRequest(
                                "/v1/catalog/" + storefront + "/songs/" + songId,
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
                            final String albumName = attrs != null
                                ? attrs.getString("albumName", "Album") : "Album";
                            if (albumId.length() > 0) {
                                Display.getDisplay(midlet).callSerially(new Runnable() {
                                    public void run() {
                                        midlet.showAlbumById(albumId, albumName, artistName, "");
                                    }
                                });
                            }
                        } catch (Exception ignored) {}
                    }
                }).start();
            }
        } else if ("play_next".equals(contextAction) || "add_to_queue".equals(contextAction)) {
            if ("play".equals(action.type)) {
                // Find the song in songItems to get name and artist
                String songName   = name;
                String artistName = subname;
                for (int i = 0; i < songItems.size(); i++) {
                    JSONObject si = (JSONObject) songItems.elementAt(i);
                    if (action.details.equals(si.getString("id", ""))) {
                        songName   = si.getString("name",   songName);
                        artistName = si.getString("artist", artistName);
                        break;
                    }
                }
                PlaybackManager pm = midlet.getPlaybackManager();
                if (pm == null) return;
                String[] ids     = new String[]{ action.details };
                String[] names   = new String[]{ songName       };
                String[] artists = new String[]{ artistName     };
                if ("play_next".equals(contextAction)) pm.insertNext(ids, names, artists);
                else                                    pm.appendToQueue(ids, names, artists);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static JSONObject makeHeader(String text) {
        JSONObject h = new JSONObject();
        h.put("type",    "header");
        h.put("name",    text);
        h.put("subname", "");
        return h;
    }

    private static void appendAll(JSONArray dst, JSONArray src) {
        for (int i = 0; i < src.size(); i++) dst.add(src.getObject(i));
    }

    private void setStatus(final String text) {
        Display.getDisplay(midlet).callSerially(new Runnable() {
            public void run() {
                statusItem.setText(text);
            }
        });
    }
}
