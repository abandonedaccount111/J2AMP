# widevine-j2me

A J2ME (CLDC 1.1 / MIDP 2.0) port of [node-widevine](https://github.com/Frooastside/node-widevine).

Implements a Widevine DRM license client: builds license challenges, sends them to
a license server, decrypts the response, and returns the content decryption keys.

---

## Project layout

```
src/com/widevine/
  WidevineConstants.java   — protocol constants (system ID, key types, …)
  ByteUtils.java           — byte-array helpers (hex, XOR, slice, …)
  AesCmac.java             — AES-CMAC (RFC 4493) implementation
  ProtoEncoder.java        — minimal protobuf binary encoder
  ProtoDecoder.java        — minimal protobuf binary decoder
  WvdParser.java           — pywidevine .wvd file parser
  KeyContainer.java        — decrypted key data-class
  WidevineInfo.java        — device metadata data-class
  LicenseSession.java      — single license-acquisition session
  WidevineDRM.java         — main entry point
  WidevineMIDlet.java      — example MIDlet showing usage
build.xml                  — Ant build
```

---

## Dependencies

| Library                             | Version | Purpose                                     |
| ----------------------------------- | ------- | ------------------------------------------- |
| **Bouncy Castle Lightweight** | 1.70+   | RSA-OAEP, RSA-PSS, AES-CBC, HMAC-SHA256     |
| J2ME CLDC 1.1                       | —      | base platform                               |
| J2ME MIDP 2.0                       | —      | MIDlet / Display (only WidevineMIDlet.java) |

Download `bcprov-jdk15on-*.jar` from [https://www.bouncycastle.org/latest_releases.html](https://www.bouncycastle.org/latest_releases.html)
and place it at `lib/bcprov.jar`.

---

## Building

```bash
# 1. Install Apache Ant
# 2. Place bcprov.jar in lib/
# 3. Edit wtk.home in build.xml if you want to run preverify
ant jar
# → dist/widevine-j2me.jar
```

For deployment to a real device, run `ant preverify` after setting `wtk.home`
to your Sun Wireless Toolkit (WTK 2.5.2) installation path.

---

## Required setup before use

### 1. Widevine Root Public Key

In `WidevineConstants.java` fill in `WIDEVINE_ROOT_PUBLIC_KEY` with the bytes
from the original `WIDEVINE_ROOT_PUBLIC_KEY` constant in
`node-widevine/src/consts.ts`.  This is an RSA-2048 SubjectPublicKeyInfo DER blob
and is used to verify service certificate signatures.

### 2. Common Service Certificate (optional)

If you want the default privacy-mode certificate, copy the bytes from
`COMMON_SERVICE_CERTIFICATE` in `node-widevine/src/consts.ts` into the
corresponding constant in `WidevineConstants.java`.

---

## API usage

```java
// ---- From raw credentials ----
byte[] clientIdBlob  = /* client_id.bin contents */;
byte[] privateKeyDer = /* PKCS#8 DER RSA private key */;

WidevineDRM wv = WidevineDRM.init(clientIdBlob, privateKeyDer);

// ---- Or from a WVD file ----
WidevineDRM wv = WidevineDRM.initWVD(wvdFileBytes);

// ---- Create a session ----
byte[] pssh    = /* raw PSSH box from DASH manifest */;
LicenseSession session = wv.createSession(pssh, WidevineConstants.LICENSE_TYPE_STREAMING);

// ---- (Optional) enable privacy mode ----
session.setDefaultServiceCertificate();
// or: session.setServiceCertificateFromMessage(serverResponse);

// ---- Build challenge and send to license server ----
byte[] challenge = session.generateChallenge();
// POST challenge to https://your-license-server/license  (Content-Type: application/octet-stream)
byte[] licenseResponse = /* HTTP response body */;

// ---- Decrypt and extract content keys ----
Vector keys = session.parseLicense(licenseResponse);
for (int i = 0; i < keys.size(); i++) {
    KeyContainer kc = (KeyContainer) keys.elementAt(i);
    System.out.println("kid=" + kc.kid + "  key=" + kc.key + "  type=" + kc.type);
}
```

---

## J2ME-specific notes

* No `java.nio` — all byte manipulation is done with `byte[]` and `ByteArrayOutputStream`.
* No `java.security` JCE — all crypto goes through Bouncy Castle's **lightweight** API
  (`org.bouncycastle.crypto.*`).  The Bouncy Castle *provider* jar is **not** needed.
* No generics — collections use `Vector` and `Hashtable`.
* `Random` (not `SecureRandom`) is used for nonce/IV generation on CLDC 1.1.
  Replace with a hardware entropy source if your platform offers one.
* Strings are converted to bytes using `"UTF-8"` / `"ASCII"` with a fallback to
  the default platform encoding for maximum compatibility.

---

## License

GPL3 as it is based on pywidevine.  Bouncy Castle is MIT/Apache-2.0.
