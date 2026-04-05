package com.widevine;

import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import com.widevine.utils.ByteUtils;
import com.widevine.types.KeyContainer;
import com.widevine.types.WidevineInfo;
import javax.microedition.io.HttpsConnection;
import org.bouncycastle.util.encoders.Base64;

import tech.alicesworld.ModernConnectorSym93.*;

import com.amplayer.utils.IAPManager;

public class WidevineMIDlet extends MIDlet {

    private static final String LICENSE_URL =
        "https://cwip-shaka-proxy.appspot.com/no_auth";

    // PSSH from the DASH manifest (base64-encoded PSSH box)
    private static final String PSSH_B64 =
        "AAAAW3Bzc2gAAAAA7e+LqXnWSs6jyCfc1R0h7QAAADsIARIQ62dqu8s0Xpa7z2FmMPGj2hoN" +
        "d2lkZXZpbmVfdGVzdCIQZmtqM2xqYVNkZmFsa3IzaioCSEQyAA==";

    private Form    form;
    private Display display;

    protected void startApp() throws MIDletStateChangeException {
        form    = new Form("Widevine DRM Demo");
        display = Display.getDisplay(this);
        display.setCurrent(form);

        // Run on a background thread — network is forbidden on the UI thread in J2ME.
        new Thread(new Runnable() {
            public void run() {
                try {
                    runDemo();
                } catch (Exception e) {
                    appendItem("Error", e.getMessage() != null ? e.getMessage() : e.toString());
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void runDemo() throws Exception {
        // 1. Load device credentials from JAR resources
        byte[] clientIdBlob  = loadResource("/client_id.bin");
        byte[] privateKeyDer = loadResource("/private_key.der");

        // 2. Initialise WidevineDRM and show device info
        WidevineDRM wv = WidevineDRM.init(clientIdBlob, privateKeyDer,
                                          WidevineConstants.DEVICE_TYPE_ANDROID);
        WidevineInfo info = wv.getInfo();
        appendItem("System ID", String.valueOf(info.systemId));
        appendItem("Device type", String.valueOf(info.deviceType));

        // 3. Decode PSSH and create a streaming session
        byte[] psshBytes = Base64.decode(PSSH_B64);
        LicenseSession session = wv.createSession(psshBytes,
                                                  WidevineConstants.LICENSE_TYPE_STREAMING);

        // 4. Fetch and apply the service certificate (privacy mode)
        byte[] svcCertChallenge = LicenseSession.getServiceCertificateChallenge();
        byte[] svcCertResponse  = post(LICENSE_URL, svcCertChallenge);
        session.setServiceCertificateFromMessage(svcCertResponse);
        appendItem("Service cert", "set (" + svcCertResponse.length + " bytes)");

        // 5. Generate signed license challenge and POST it
        byte[] challenge        = session.generateChallenge();
        System.out.println("Challenge size: " + challenge.length + " bytes");
        byte[] licenseResponse  = post(LICENSE_URL, challenge);

        // 6. Parse the license and display every content key
        Vector keys = session.parseLicense(licenseResponse);
        if (keys.size() == 0) {
            appendItem("Result", "no keys returned");
        } else {
            for (int i = 0; i < keys.size(); i++) {
                KeyContainer kc = (KeyContainer) keys.elementAt(i);
                appendItem("Key " + i + " (type " + kc.type + ")", kc.kid + ":" + kc.key);
            }
        }
    }

    /** POST raw bytes and return the response body. */
    private byte[] post(String url, byte[] body) throws Exception {
        HttpConnection conn = null;
        InputStream    in   = null;
        OutputStream   out  = null;
        try {
            conn = (HttpConnection) ModernConnector.open(IAPManager.appendTo(url));
            IAPManager.captureFromSystem();
            conn.setRequestMethod(HttpConnection.POST);
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setRequestProperty("Content-Length", String.valueOf(body.length));

            out = conn.openOutputStream();
            out.write(body);
            out.flush();

            int status = conn.getResponseCode();
            in = conn.openInputStream();
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            byte[] buf = new byte[512];
            int n;
            while ((n = in.read(buf)) != -1) {
                baos.write(buf, 0, n);
            }
            byte[] responseBytes = baos.toByteArray();

            if (status != HttpConnection.HTTP_OK) {
                throw new Exception("HTTP " + status + ": " +
                    new String(responseBytes, 0, Math.min(200, responseBytes.length)));
            }
            return responseBytes;
        } finally {
            if (out  != null) try { out.close();  } catch (Exception ignored) {}
            if (in   != null) try { in.close();   } catch (Exception ignored) {}
            if (conn != null) try { conn.close(); } catch (Exception ignored) {}
        }
    }

    /** Load a JAR-bundled resource; throws if missing. */
    private byte[] loadResource(String path) throws Exception {
        InputStream is = getClass().getResourceAsStream(path);
        if (is == null) {
            throw new Exception("Resource not found: " + path);
        }
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[512];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        is.close();
        return baos.toByteArray();
    }

    /** Append a StringItem to the form from any thread. */
    private void appendItem(final String label, final String value) {
        form.append(new StringItem(label + ": ", value));
    }

    protected void pauseApp() {}

    protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {}
}
