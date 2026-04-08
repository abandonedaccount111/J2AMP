# **J2AMP - Apple Music Player for J2ME**

J2AMP is a proof-of-concept but also very fully-featured music player for Apple Music (AM)

Technically supports all devices with CLDC 1.1, MIDP 2.0 with JSR-75 and JSR-135 and HE-AACv2 support, however due to Widevine DRM playback, device with large RAM/heap are required.

Currently tested on an Nokia E72 / J2ME Loader emulator.

### Some features:

- Search and extended queuing to play artists, songs, albums and playlists from both the Apple Music catalog and your AM library
- Quick navigation between songs, artists, albums and playlists.
- Autoplay for artists, albums and playlists
- Personal and artist radio stations
- LastFM scrobbling
- Lyrics viewing
- Shuffle, repeat

### Images:

![Main menu](/images/mainmenu.png)
![Now playing screen](/images/nowplaying.png)
![Search results](/images/searchresult.png)
![Lyrics view](/images/lyrics.png)
![Album view](/images/album.png)

### User instruction

- Your phones need to be cracked to install necessary TLS libraries. More information look at [here](https://gtrxac.fi/j2me/proxyless)
- Get your Apple Music credentials and save it as a `.json` file. You can get these from the Apple Music Web Player:
  - 1. Open https://music.apple.com in your PC's browser
  - 2. Log in and open the Console tab in the Web Inspector (F12 or CTRL+SHIFT+I)
  - 3. In the Web Inspector, go to the Console tab and paste:"
       ``JSON.stringify({ devToken: MusicKit.getInstance().developerToken,  userToken: MusicKit.getInstance().musicUserToken }) ``
  - 4. Copy the output JSON and save it as a .json file on your phone
- Install the correct file in [./dist](https://github.com/abandonedaccount111/J2AMP/tree/main/dist) into your phone.
  - For S40/ S80 / Sony Ericsson phones ..., upload the JAR file in the root directory to [https://gtrxac.fi/fh](https://gtrxac.fi/fh) and install the return **signed** file with **your phone's browser**. 
  - For S60v3 FP1 phones or older  / J2ME emulators, just install the JAR file in the root directory with your phone's file manager.
  - For S60v3 FP2 phones (maybe also newer Symbian devices, e.g S60v5, Symbian^3, ...), install the JAR file from the `.\SYMBIAN9_3` folder with your phone's file manager. (this is just to bypass the pesky internet selection dialog)

### Build instructions

- Install NetBeans 7.4 from [here](https://dlc-cdn.sun.com/netbeans/7.4/final/bundles/netbeans-7.4-windows.exe) and the ProGuard library from [here](https://updates.netbeans.org/netbeans/updates/7.4/uc/final/certified/modules/extra/org-netbeans-modules-mobility-proguard.nbm).
- Get the following files and put it into `/res`:

  - client_id.bin and private_key.pem from a Widevine-authorized device. I won't provide it in the source code, but you can look it up how to get them. Convert the private_key.pem to private_key.der:
    ``openssl rsa -in key.pem -outform der -out key.der``
  - LastFM's API token and secret key, formatted as lastfm_token.json.

  ```
  {"apiKey" : "<your api key here>",
  "sharedSecret" : "<your shared secret key>"}
  ```
- Build the binary

### FAQ:

- Will you ever add offline playback: well technically possible, adding them would be very risky legally for me so **NO.**
- Can you add other features such as X, Y, Z: maybe if I have time and if it is feasible; PRs are of course, always welcome :)

### Credits:

- [Frooastside](https://github.com/Frooastside) for the NodeJS implemention of pywidevine, and [rlaphoenix](https://github.com/rlaphoenix) for [pywidevine](https://github.com/devine-dl/pywidevine)
- [ProgrammerIn-wonderland](https://github.com/ProgrammerIn-wonderland) for [ModernConnector](https://github.com/ProgrammerIn-wonderland/ModernConnector)
- [journeyapps](https://github.com/journeyapps) for the J2ME protobuf [library](https://github.com/journeyapps/protobuf-j2me)

### LICENSE

This software is licensed under the terms of GNU General Public License, Version 3.0.

### DISCLAIMER

This software is intended for educational purposes only. The creator assumes no responsibility for any illegal activities carried out with it, and the code is made public for viewing purposes only. Please use this software ethically and responsibly.
