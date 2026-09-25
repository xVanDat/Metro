# Metro

An offline, Material Design music player for Android, based on
[MuntashirAkon/Metro](https://github.com/MuntashirAkon/Metro).

[Tiếng Việt](README_VI.md)

This fork keeps Metro fully offline and libre while adding storage fixes, playback tools and
player designs aimed at modern Android devices.

## What this fork adds over upstream Metro

### Library and storage

- Android 13/14-compatible folder blacklist using the system folder picker.
- Blacklist support for both internal storage and removable SD cards.
- A separate **Hide song** action, with a settings screen for restoring hidden tracks.
- Playlist-file import through Android Files. Supported formats include M3U, M3U8, PLS, XSPF,
  WPL and ASX; MediaStore playlist import remains available.
- Full library rescan across available storage volumes.
- Accent-insensitive search, making Vietnamese and other diacritic-heavy titles easier to find.
- Extended backups for the blacklist, history, play counts and current queue.

### Playback

- Per-track resume positions for long audio, useful for mixes, podcasts and audiobooks.
- Playback bookmarks that can be added, opened and removed from the player.
- Safe ReplayGain normalization with track and album modes.
- Improved synchronized-lyrics rendering in the custom player themes.

### Interface and customization

- A **Home** tab with morning, afternoon and evening greetings.
- An animated album-art gradient player theme.
- Offline player themes inspired by YouTube Music and Spotify.
- Custom TTF/OTF fonts selected through Android Files, with an easy reset option.
- A simplified theme chooser and reorganized settings.
- Complete English and Vietnamese interface support.

## Core Metro features

- Fully offline playback with no Internet permission.
- Browse music by songs, albums, artists, playlists, genres and folders.
- Multiple application and now-playing themes, Material You and dynamic colors.
- Gapless playback, crossfade, sleep timer and driving mode.
- Synced lyrics, tag editing, queue reordering and smart playlists.
- Android Auto, headset/Bluetooth controls, widgets and lock-screen controls.
- Playlist creation, editing, export and MediaStore import.

## Languages

The application ships with English and Vietnamese resources only. **System default** uses
Vietnamese on Vietnamese devices and falls back to English on other system languages. Either
language can also be selected explicitly in Settings.

Every new user-facing string in this fork must be added to both the English and Vietnamese
resource files.

## Building

Android Studio is not required. See [BUILDING.md](BUILDING.md) for PowerShell commands for debug
and signed release APKs, including a low-memory configuration.

## License

Metro is released under the [GNU General Public License v3.0](LICENSE.md).

> Metro is an offline music player. It does not download or stream music.
