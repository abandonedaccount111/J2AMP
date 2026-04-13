/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.amplayer.utils;

/**
 *
 * @author randomaccount
 */
public class URLEncoder {

    public static String encode(String s) {
        try {
            byte[] bytes = s.getBytes("UTF-8");
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bytes.length; i++) {
                int c = bytes[i] & 0xFF;
                if ((c >= 'a' && c <= 'z') ||
                    (c >= 'A' && c <= 'Z') ||
                    (c >= '0' && c <= '9') ||
                    c == '-' || c == '_' || c == '.' || c == '~') {
                    sb.append((char) c);
                } else if (c == ' ') {
                    sb.append("%20");
                } else {
                    sb.append('%');
                    sb.append(toHex((c >> 4) & 0xF));
                    sb.append(toHex(c & 0xF));
                }
            }
            return sb.toString();
        } catch (Exception e) {
            return s;
        }
    }

    private static char toHex(int i) {
        if (i < 10) return (char) ('0' + i);
        else return (char) ('A' + (i - 10));
    }
}
