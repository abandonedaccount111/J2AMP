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

}
