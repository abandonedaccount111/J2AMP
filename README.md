# **J2AMP - Apple Music Player for J2ME**

J2AMP is a proof-of-concept but also very fully-featured music player for Apple Music (AM)

### Some features:

- Search and extended queuing to play artists, songs, albums and playlists from both the Apple Music  catalog and your AM library
- Quick navigation between songs, artists, albums and playlists.
- LastFM scrobbling
- Lyrics viewing
- Shuffle, repeat

### User instruction

- Your phones need to be cracked to install necessary TLS libraries. More information look at [here](https://gtrxac.fi/j2me/proxyless)
- Get your Apple Music credentials and save it as a `.json` file. You can get these from the Apple Music Web Player:
  - 1. Open https://music.apple.com in your PC's browser
  - 2. Log in and open the Console tab in the Web Inspector (F12 or CTRL+SHIFT+I)
  - 3. In the Web Inspector, go to the Console tab and paste:"
       ``JSON.stringify({ devToken: MusicKit.getInstance().developerToken,  userToken: MusicKit.getInstance().musicUserToken }) ``
  - 4. Copy the output JSON and save it as a .json file on your phone
- Install the WV2J2ME.jar file in [./dist](https://github.com/abandonedaccount111/J2AMP/tree/main/dist) into your phone.

### Build instructions

- Install NetBeans 7.4 from [here](https://dlc-cdn.sun.com/netbeans/7.4/final/bundles/netbeans-7.4-windows.exe) and the ProGuard library from [here](https://updates.netbeans.org/netbeans/updates/7.4/uc/final/certified/modules/extra/org-netbeans-modules-mobility-proguard.nbm).
- Get the following files and put it into `/res`:

  - client_id.bin and private_key.pem from a Widevine-authorized device. I wont provide it in the source code but you can look it up how to get them. Convert the private_key.pem to private_key.der:
    ``openssl rsa -in key.pem -outform der -out key.der``
  - LastFM's API token and secret key, formatted as last_fm.json.

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
