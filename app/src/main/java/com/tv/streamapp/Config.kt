package com.tv.streamapp

/**
 * ============================================================================
 * ANDROID TV STREAM CONFIGURATION
 * ============================================================================
 * This is the ONLY file you need to modify to point the app to your remote playlist.
 * Changing your remote JSON or M3U on GitHub / hosting updates ALL users instantly!
 */
object Config {
    /**
     * The remote URL containing your channels playlist.
     * Supports BOTH JSON format (channels.json) and standard M3U/M3U8 playlists!
     * The app auto-detects whether the returned content is JSON or M3U.
     */
    const val PLAYLIST_URL = "https://raw.githubusercontent.com/lionelhocine/hcn-tv-apk/main/channels.json"

    /**
     * App display name shown in Top Bar and Splash
     */
    const val APP_NAME = "HoC!Ne TV"

    /** Default accent color (focus ring, buttons). Can be overridden from channels.json branding.primaryColor */
    const val DEFAULT_PRIMARY_COLOR = "#00C8FF"

    /**
     * Timeout in seconds for downloading the playlist
     */
    const val NETWORK_TIMEOUT_SECONDS = 15L

    /**
     * Number of times to retry playing an unavailable stream before skipping
     */
    const val MAX_STREAM_RETRY_COUNT = 3

    /**
     * Local cache file name for offline fallback
     */
    const val CACHE_PLAYLIST_FILE = "cached_playlist.data"

    /**
     * User-Agent sent with video requests (some IPTV streams require standard player UA)
     */
    const val PLAYER_USER_AGENT = "ExoPlayerTV/1.0 (Android TV; Linux)"
}