package com.amplayer.ui;

import com.amplayer.playback.PlaybackManager;
import com.amplayer.utils.Settings;

/**
 * Last.fm scrobbler wired into PlaybackManager events.
 *
 * Rules (matching Last.fm spec):
 *   - updateNowPlaying is sent when a new track starts.
 *   - scrobble is submitted once the track has been played for at least
 *     50% of its duration OR 4 minutes, whichever comes first.
 *     Since J2ME gives us no reliable duration, we use 1.5 minutes here.
 *
 * Usage: create one instance and set it as the chain listener after
 * NowPlayingScreen:
 *
 *   scrobbler = new LastFmScrobbler(pm);
 *   // NowPlayingScreen wraps pm.setListener internally, so pass scrobbler
 *   // via AppleMusicMIDlet which chains it from PlaybackManager directly.
 *
 * This class wraps any existing listener (chain pattern).
 */
public class LastFmScrobbler implements PlaybackManager.Listener {

    // -------------------------------------------------------------------------
    // Timing: scrobble after 4 minutes of playback (no duration available)
    // -------------------------------------------------------------------------

    private static final long SCROBBLE_DELAY_MS = 1 * 90 * 1000L;

    private final PlaybackManager pm;
    private final PlaybackManager.Listener chain;

    private String currentTrack  = null;
    private String currentArtist = null;
    private String currentAlbum  = null;
    private long   startTime     = 0L;
    private boolean scrobbled    = false;

    private volatile boolean timerRunning = false;
    private LastFmHelper lfmHelper = null;

    public LastFmScrobbler(PlaybackManager pm, PlaybackManager.Listener chain) {
        this.pm    = pm;
        this.chain = chain;
        setUpLfm();
    }
    
    private void setUpLfm(){
        lfmHelper = new LastFmHelper();
        
    };

    // -------------------------------------------------------------------------
    // PlaybackManager.Listener
    // -------------------------------------------------------------------------

    public void onTrackChanged(int index) {
        if (chain != null) chain.onTrackChanged(index);
        String name   = pm.getTrackName(index);
        String artist = pm.getTrackArtist(index);
        String album  = "";  // album not stored in PlaybackManager queue
        onNewTrack(name, artist, album);
    }

    public void onPlayStateChanged(boolean playing) {
        if (chain != null) chain.onPlayStateChanged(playing);
    }

    public void onError(String msg) {
        if (chain != null) chain.onError(msg);
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void onNewTrack(final String name, final String artist, final String album) {
        currentTrack  = name;
        currentArtist = artist;
        currentAlbum  = album;
        startTime     = System.currentTimeMillis();
        scrobbled     = false;

        // Update Now Playing (non-blocking)
        new Thread(new Runnable() {
            public void run() {
                String sk = Settings.lastFmSk;
                if (sk == null || sk.length() == 0) return;
                lfmHelper.updateNowPlaying(sk, artist, name, album);
            }
        }).start();

        // Start scrobble timer
        startScrobbleTimer(name, artist, album);
    }

    private void startScrobbleTimer(final String name, final String artist, final String album) {
        timerRunning = true;
        new Thread(new Runnable() {
            public void run() {
                try { Thread.sleep(SCROBBLE_DELAY_MS); }
                catch (InterruptedException e) { return; }
                // Still the same track and not yet scrobbled?
                if (timerRunning && !scrobbled
                        && name.equals(currentTrack)
                        && artist.equals(currentArtist)) {
                    String sk = Settings.lastFmSk;
                    if (sk != null && sk.length() > 0) {
                        scrobbled = true;
                        long ts = startTime / 1000L;
                        lfmHelper.scrobble(sk, artist, name, album, ts);
                    }
                }
            }
        }).start();
    }

    public void stop() {
        timerRunning = false;
    }

    // -------------------------------------------------------------------------
    // Album name update — called from NowPlayingScreen when it resolves the album
    // -------------------------------------------------------------------------

    public void setCurrentAlbum(String album) {
        this.currentAlbum = album;
    }
}
