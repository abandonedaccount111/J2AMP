package com.amplayer.ui;

import cc.nnproject.json.JSON;
import cc.nnproject.json.JSONObject;
import com.amplayer.midlets.AppleMusicMIDlet;
import com.amplayer.utils.TokenStore;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.lcdui.StringItem;

/**
 * First-run token setup screen.
 *
 * Shown when no tokens are found in RMS.  The user browses for a JSON file
 * containing {"devToken":"...","userToken":"..."}, which is then parsed and
 * persisted to RMS.  On success the MIDlet's onTokensReady() is called.
 */
public class TokenSetupForm extends Form implements CommandListener {

    private static final Command CMD_BROWSE = new Command("Browse", Command.OK,   1);
    private static final Command CMD_RESET  = new Command("Reset",  Command.ITEM, 2);

    private final AppleMusicMIDlet midlet;
    private final Display          display;
    private final StringItem       statusItem;

    public TokenSetupForm(AppleMusicMIDlet midlet, Display display) {
        super("J2AMP Setup");
        this.midlet  = midlet;
        this.display = display;

        append(new StringItem("", "Select a JSON file with your Apple Music tokens.\n\n"
                + "Expected format:\n"
                + "{\"devToken\":\"...\",\n \"userToken\":\"...\"}"));
        
        append(new StringItem("", "You can get these from the Apple Music Web Player:\n\n"
                + "1. Open https://music.apple.com in your PC's browser\n"
                + "2. Log in and open the Console tab in the Web Inspector (F12)\n"
                + "3. In the Web Inspector, go to the Console tab and paste:\n\n"
                + "   JSON.stringify({\n"
                + "     devToken: MusicKit.getInstance().developerToken,\n"
                + "     userToken: MusicKit.getInstance().musicUserToken\n"
                + "   })\n\n"
                + "4. Copy the output JSON and save it as a .json file on your phone"));

        statusItem = new StringItem("", "");
        append(statusItem);

        addCommand(CMD_BROWSE);
        addCommand(CMD_RESET);
        setCommandListener(this);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    public void commandAction(Command c, Displayable d) {
        if (c == CMD_BROWSE) {
            display.setCurrent(new FileBrowser(display, new FileBrowser.Listener() {
                public void onFileSelected(String fileUrl) {
                    display.setCurrent(TokenSetupForm.this);
                    loadFromFile(fileUrl);
                }
                public void onCancelled() {
                    display.setCurrent(TokenSetupForm.this);
                }
            }));
        } else if (c == CMD_RESET) {
            TokenStore.clear();
            setStatus("Tokens cleared.");
        }
    }

    // -------------------------------------------------------------------------
    // File → parse → save → proceed
    // -------------------------------------------------------------------------

    private void loadFromFile(final String fileUrl) {
        setStatus("Reading " + fileName(fileUrl) + " ...");
        new Thread(new Runnable() {
            public void run() {
                FileConnection fc = null;
                InputStream    in = null;
                try {
                    fc = (FileConnection) Connector.open(fileUrl, Connector.READ);
                    if (!fc.exists()) throw new Exception("File not found");
                    in = fc.openInputStream();
                    byte[] data = readAll(in);
                    String json = new String(data, "UTF-8");
                    System.out.println("Unarsed JSON: " + json);
                    JSONObject obj = JSON.getObject(json);
                    System.out.println("Parsed JSON: " + obj.toString());
                    String devToken  = obj.getString("devToken",  null);
                    String userToken = obj.getString("userToken", null);

                    if (devToken == null || devToken.length() == 0)
                        throw new Exception("Missing \"devToken\" field");
                    if (userToken == null || userToken.length() == 0)
                        throw new Exception("Missing \"userToken\" field");

                    TokenStore.save(devToken, userToken);
                    setStatus("Tokens saved. Loading...");
                    midlet.onTokensReady(devToken, userToken);

                } catch (Exception e) {
                    setStatus("Error: " + (e.getMessage() != null ? e.getMessage() : e.toString()));
                } finally {
                    if (in != null) try { in.close();  } catch (Exception ignored) {}
                    if (fc != null) try { fc.close();  } catch (Exception ignored) {}
                }
            }
        }).start();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void setStatus(final String text) {
        display.callSerially(new Runnable() {
            public void run() { statusItem.setText(text); }
        });
    }

    private static String fileName(String url) {
        int slash = url.lastIndexOf('/');
        return slash >= 0 ? url.substring(slash + 1) : url;
    }

    private static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[2048];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }
}
