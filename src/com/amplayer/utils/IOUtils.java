/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.amplayer.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;
import java.util.Enumeration;

/**
 *
 * @author randomaccount
 */
public class IOUtils {
   
   public static byte[] readAll(InputStream in) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        int n;
        while ((n = in.read(buf)) != -1) baos.write(buf, 0, n);
        return baos.toByteArray();
    }

    public static void closeQuietly(InputStream in) {
        if (in != null) try { in.close(); } catch (Exception ignored) {}
    }

    public static void closeQuietly(OutputStream out) {
        if (out != null) try { out.close(); } catch (Exception ignored) {}
    }

    public static void closeConn(HttpConnection conn) {
        if (conn != null) try { conn.close(); } catch (Exception ignored) {}
    }

    public static String getCacheDirectory() {
        String dir = null;
        String mc = System.getProperty("fileconn.dir.memorycard");
        if (mc != null && mc.length() > 0) {
            if (!mc.endsWith("/")) mc += "/";
            dir = mc + "wvj2me/";
        }
        if (dir == null) {
            try {
                Enumeration roots = FileSystemRegistry.listRoots();
                while (roots.hasMoreElements()) {
                    String root = (String) roots.nextElement();
                    if (!root.toUpperCase().startsWith("C")) {
                        dir = "file:///" + root + "wvj2me/"; break;
                    }
                }
            } catch (Exception ignored) {}
        }
        if (dir == null) {
            String priv = System.getProperty("fileconn.dir.private");
            if (priv != null && priv.length() > 0) {
                if (!priv.endsWith("/")) priv += "/";
                dir = priv + "wvj2me/";
            }
        }
        return dir;
    }

}
