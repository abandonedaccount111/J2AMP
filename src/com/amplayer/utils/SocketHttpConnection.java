package com.amplayer.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.SocketConnection;

/**
 * HttpConnection backed by a raw socket (or ssl://) so every header —
 * including Origin — is written verbatim with no runtime filtering.
 *
 * Supports:
 *   - HTTP and HTTPS  (uses "ssl://" connector for https)
 *   - GET / POST / any method
 *   - Content-Length and chunked Transfer-Encoding responses
 *
 * Usage (identical to Connector.open for HttpConnection):
 *   SocketHttpConnection conn = SocketHttpConnection.open(url);
 *   conn.setRequestMethod("GET");
 *   conn.setRequestProperty("Origin", "https://beta.music.apple.com");
 *   conn.setRequestProperty("Authorization", "Bearer ...");
 *   int status = conn.getResponseCode();
 *   InputStream in = conn.openInputStream();
 */
public class SocketHttpConnection implements HttpConnection {

    // -------------------------------------------------------------------------
    // Parsed URL parts
    // -------------------------------------------------------------------------

    private final String scheme;
    private final String host;
    private final int    port;
    private final String pathAndQuery;

    // -------------------------------------------------------------------------
    // Request state
    // -------------------------------------------------------------------------

    private String    method  = "GET";
    private Hashtable reqHeaders = new Hashtable();    // preserves caller order not required

    // Body captured from openOutputStream() before execute()
    private ByteArrayOutputStream reqBodyCapture = null;

    // -------------------------------------------------------------------------
    // Response state (populated by execute())
    // -------------------------------------------------------------------------

    private boolean  executed      = false;
    private int      statusCode    = -1;
    private String   statusMessage = "";
    private Hashtable respHeaders  = new Hashtable();   // lower-cased keys
    private Vector   respHeaderKeys = new Vector();     // original-case ordered keys
    private byte[]   respBody      = null;

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    private SocketHttpConnection(String scheme, String host, int port, String pathAndQuery) {
        this.scheme       = scheme;
        this.host         = host;
        this.port         = port;
        this.pathAndQuery = pathAndQuery;
    }

    /**
     * Parses the URL and returns an un-executed SocketHttp Connection.
     * Equivalent to (HttpConnection) Connector.open(url).
     */
    public static SocketHttpConnection open(String url) throws IOException {
        int schemeEnd = url.indexOf("://");
        if (schemeEnd == -1) throw new IOException("Invalid URL: " + url);

        String scheme = url.substring(0, schemeEnd).toLowerCase();
        String rest   = url.substring(schemeEnd + 3);

        int slashIdx  = rest.indexOf('/');
        String authority = slashIdx == -1 ? rest : rest.substring(0, slashIdx);
        String path      = slashIdx == -1 ? "/" : rest.substring(slashIdx);

        int colonIdx = authority.indexOf(':');
        String host;
        int    port;
        if (colonIdx == -1) {
            host = authority;
            port = "https".equals(scheme) ? 443 : 80;
        } else {
            host = authority.substring(0, colonIdx);
            port = Integer.parseInt(authority.substring(colonIdx + 1));
        }

        return new SocketHttpConnection(scheme, host, port, path);
    }

    // -------------------------------------------------------------------------
    // Request setup
    // -------------------------------------------------------------------------

    public void setRequestMethod(String method) throws IOException {
        this.method = method;
    }

    public String getRequestMethod() {
        return method;
    }

    public void setRequestProperty(String field, String value) throws IOException {
        reqHeaders.put(field, value);
    }

    public String getRequestProperty(String field) {
        return (String) reqHeaders.get(field);
    }

    // -------------------------------------------------------------------------
    // Output stream  — captures body bytes; execute() sends them
    // -------------------------------------------------------------------------

    public OutputStream openOutputStream() throws IOException {
        reqBodyCapture = new ByteArrayOutputStream();
        return reqBodyCapture;
    }

    public DataOutputStream openDataOutputStream() throws IOException {
        return new DataOutputStream(openOutputStream());
    }

    // -------------------------------------------------------------------------
    // Execute  — opens socket, sends request, parses response
    // -------------------------------------------------------------------------

    private void execute() throws IOException {
        if (executed) return;
        executed = true;

        String socketScheme = "https".equals(scheme) ? "ssl" : "socket";
        String connUrl = IAPManager.appendTo(socketScheme + "://" + host + ":" + port);
        SocketConnection sc = (SocketConnection) Connector.open(connUrl);
        IAPManager.captureFromSystem();   // no-op after first successful open

        OutputStream sockOut = sc.openOutputStream();
        InputStream  sockIn  = sc.openInputStream();
        try {
            byte[] bodyBytes = reqBodyCapture != null ? reqBodyCapture.toByteArray() : null;

            // --- Request line ---
            StringBuffer req = new StringBuffer();
            req.append(method).append(" ").append(pathAndQuery).append(" HTTP/1.1\r\n");
            req.append("Host: ").append(host).append("\r\n");

            // --- Headers (all caller-supplied, including Origin) ---
            for (Enumeration e = reqHeaders.keys(); e.hasMoreElements();) {
                String key = (String) e.nextElement();
                req.append(key).append(": ")
                   .append((String) reqHeaders.get(key)).append("\r\n");
            }

            req.append("Connection: close\r\n");
            req.append("\r\n");

            sockOut.write(req.toString().getBytes("UTF-8"));
            if (bodyBytes != null && bodyBytes.length > 0) {
                sockOut.write(bodyBytes);
            }
            sockOut.flush();

            // --- Parse response ---
            parseResponse(sockIn);
        } finally {
            IOUtils.closeQuietly(sockIn);
            IOUtils.closeQuietly(sockOut);
            sc.close();
        }
    }

    // -------------------------------------------------------------------------
    // Response parsing
    // -------------------------------------------------------------------------

    private void parseResponse(InputStream in) throws IOException {
        StringBuffer lineBuf  = new StringBuffer();
        boolean      firstLine = true;

        // Read status line + headers
        int b;
        while ((b = in.read()) != -1) {
            if (b == '\r') continue;
            if (b == '\n') {
                String line = lineBuf.toString();
                lineBuf.setLength(0);

                if (firstLine) {
                    parseStatusLine(line);
                    firstLine = false;
                } else if (line.length() == 0) {
                    break;  // blank line = end of headers
                } else {
                    parseHeaderLine(line);
                }
                continue;
            }
            lineBuf.append((char) b);
        }

        // Read body
        String te = respHeader("transfer-encoding");
        String cl = respHeader("content-length");

        if ("chunked".equalsIgnoreCase(te)) {
            respBody = readChunked(in);
        } else if (cl != null) {
            int len    = Integer.parseInt(cl.trim());
            respBody   = new byte[len];
            int offset = 0;
            while (offset < len) {
                int n = in.read(respBody, offset, len - offset);
                if (n == -1) break;
                offset += n;
            }
        } else {
            try {
            respBody = IOUtils.readAll(in);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void parseStatusLine(String line) throws IOException {
        // "HTTP/1.1 200 OK"
        int s1 = line.indexOf(' ');
        if (s1 == -1) throw new IOException("Bad status line: " + line);
        int s2 = line.indexOf(' ', s1 + 1);
        statusCode    = Integer.parseInt((s2 == -1 ? line.substring(s1 + 1)
                                                   : line.substring(s1 + 1, s2)).trim());
        statusMessage = s2 == -1 ? "" : line.substring(s2 + 1);
    }

    private void parseHeaderLine(String line) {
        int colon = line.indexOf(':');
        if (colon == -1) return;
        String key   = line.substring(0, colon).trim();
        String value = line.substring(colon + 1).trim();
        respHeaders.put(key.toLowerCase(), value);
        respHeaderKeys.addElement(key);
    }

    private byte[] readChunked(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        StringBuffer          sb   = new StringBuffer();

        while (true) {
            // Read chunk-size line (hex, optional extensions after ";")
            sb.setLength(0);
            int b;
            while ((b = in.read()) != -1) {
                if (b == '\r') continue;
                if (b == '\n') break;
                sb.append((char) b);
            }
            String sizeStr = sb.toString().trim();
            int semi = sizeStr.indexOf(';');
            if (semi != -1) sizeStr = sizeStr.substring(0, semi).trim();

            int chunkLen = Integer.parseInt(sizeStr, 16);
            if (chunkLen == 0) break;

            byte[] chunk  = new byte[chunkLen];
            int    offset = 0;
            while (offset < chunkLen) {
                int n = in.read(chunk, offset, chunkLen - offset);
                if (n == -1) break;
                offset += n;
            }
            baos.write(chunk, 0, offset);

            in.read(); // consume trailing \r
            in.read(); // consume trailing \n
        }
        return baos.toByteArray();
    }

    // -------------------------------------------------------------------------
    // Response accessors
    // -------------------------------------------------------------------------

    public int    getResponseCode()    throws IOException { execute(); return statusCode;    }
    public String getResponseMessage() throws IOException { execute(); return statusMessage; }

    public String getHeaderField(String name) throws IOException {
        execute();
        return respHeader(name);
    }

    public int getHeaderFieldInt(String name, int def) throws IOException {
        String v = getHeaderField(name);
        if (v == null) return def;
        try { return Integer.parseInt(v.trim()); } catch (NumberFormatException e) { return def; }
    }

    public long getHeaderFieldDate(String name, long def) throws IOException { return def; }
    public long getExpiration()  throws IOException { return 0; }
    public long getDate()        throws IOException { return 0; }
    public long getLastModified()throws IOException { return 0; }

    public String getHeaderField(int n) throws IOException {
        execute();
        if (n < 0 || n >= respHeaderKeys.size()) return null;
        return respHeader((String) respHeaderKeys.elementAt(n));
    }

    public String getHeaderFieldKey(int n) throws IOException {
        execute();
        if (n < 0 || n >= respHeaderKeys.size()) return null;
        return (String) respHeaderKeys.elementAt(n);
    }

    // -------------------------------------------------------------------------
    // Input stream
    // -------------------------------------------------------------------------

    public InputStream openInputStream() throws IOException {
        execute();
        return new ByteArrayInputStream(respBody);
    }

    public DataInputStream openDataInputStream() throws IOException {
        return new DataInputStream(openInputStream());
    }

    // -------------------------------------------------------------------------
    // URL info
    // -------------------------------------------------------------------------

    public String getURL()      { return scheme + "://" + host + ":" + port + pathAndQuery; }
    public String getProtocol() { return scheme;       }
    public String getHost()     { return host;         }
    public String getFile()     { return pathAndQuery; }
    public String getRef()      { return null;         }
    public int    getPort()     { return port;         }

    public String getType() {
        try { return getHeaderField("Content-Type"); } catch (Exception e) { return null; }
    }
    public String getEncoding() {
        try { return getHeaderField("Content-Encoding"); } catch (Exception e) { return null; }
    }
    public long getLength() {
        try { return getHeaderFieldInt("Content-Length", -1); } catch (Exception e) { return -1; }
    }

    // -------------------------------------------------------------------------
    // Close
    // -------------------------------------------------------------------------

    public void close() throws IOException {
        // Socket already closed after execute(); nothing to do.
    }

    // -------------------------------------------------------------------------
    // Internal helper
    // -------------------------------------------------------------------------

    private String respHeader(String name) {
        return (String) respHeaders.get(name.toLowerCase());
    }

    public String getQuery() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
