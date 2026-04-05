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

    // Encode a string for URL
    public static String encode(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if ((c >= 'a' && c <= 'z') ||
                (c >= 'A' && c <= 'Z') ||
                (c >= '0' && c <= '9') ||
                c == '-' || c == '_' || c == '.' || c == '~') {
                sb.append(c); // safe character
            } else if (c == ' ') {
                sb.append('+'); // encode space as +
            } else {
                // encode as %HH
                sb.append('%');
                sb.append(toHex((c >> 4) & 0xF));
                sb.append(toHex(c & 0xF));
            }
        }
        return sb.toString();
    }

    private static char toHex(int i) {
        if (i < 10) return (char) ('0' + i);
        else return (char) ('A' + (i - 10));
    }
}
