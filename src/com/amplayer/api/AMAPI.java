/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.amplayer.api;

import java.io.InputStream;
import java.io.OutputStream;
import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.io.HttpsConnection;
import com.amplayer.utils.IAPManager;
import com.amplayer.utils.IOUtils;
import cc.nnproject.json.*;
import com.amplayer.utils.SocketHttpConnection;
import com.amplayer.utils.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;

import tech.alicesworld.ModernConnector.*;

/**
 *
 * @author randomaccount
 */
public class AMAPI {
    // Connect to Apple Music AMP API

    private String developerToken;
    private String userToken;
    private String storefront;
    private String storefrontLanguage;
    // Always use SocketHttpConnection for amp-api requests so that headers like
    // Origin are sent verbatim (native HttpConnection strips them on Symbian).
    // play.itunes.apple.com is exempted below because it is handled separately.
    private boolean httpConnectionWorks = false;
    private String URL_PREFIX = "http://amp-api.music.apple.com";

    public AMAPI(String developerToken, String userToken) {
        this.developerToken = developerToken;
        this.userToken      = userToken;
        this.storefront     = "us";
        this.storefrontLanguage = "en-US";
        getUserStorefront();
    }

    private void getUserStorefront() {
        try {
            JSONObject result = APIRequest("/v1/me/storefront", null, "GET", null, null);
            storefront = result.getArray("data").getObject(0).getString("id");
            storefrontLanguage = result.getArray("data").getObject(0).getObject("attributes").getString("defaultLanguageTag");
            System.out.println("New storefront: " + storefront);
            System.out.println("New storefront language: " + storefrontLanguage);
        } catch (Exception e) {
            // leave default "us"
            e.printStackTrace();
        }
    }

    public interface AMAPIListener {
      void onResponse(JSONObject result);
      void onError(Exception e);
    }

    public String getDeveloperToken() {
        return developerToken;
    }

    public String getUserToken() {
        return userToken;
    }
    
    public String getStorefront() {
        return storefront;
    }
    
    public String getStorefrontLanguage() {
        return storefrontLanguage;
    }

    public void setUserToken(String userToken) {
        this.userToken = userToken;
    }

    public void setDeveloperToken(String developerToken) {
        this.developerToken = developerToken;
    }

    public String APIRequestString(String endpoint, Hashtable queryParameters, String method, String body, String prefix) throws Exception {
        // Make API request to Apple Music AMP API
        // Handle authentication and response parsing
        
        HttpConnection conn      = null;
        InputStream    in        = null;
        OutputStream   out       = null;
        if (prefix == null) {
            prefix = URL_PREFIX;
        }
        String url = prefix + endpoint;
        if (queryParameters != null){
            // Append query parameters to URL (don't forget to URL-encode them)
            StringBuffer  sb = new StringBuffer(url);
            sb.append("?");
            for (Enumeration keys = queryParameters.keys(); keys.hasMoreElements();) {
                String key = (String) keys.nextElement();
                String value = (String) queryParameters.get(key);
                sb.append(URLEncoder.encode(key));
                sb.append("=");
                sb.append(URLEncoder.encode(value));
                if (keys.hasMoreElements()) {
                    sb.append("&");
                }
            }

            url = sb.toString();
        }
        try {
            if (httpConnectionWorks) {
                conn = (HttpConnection) Connector.open(url);
            } else if (prefix.startsWith("https")) {
                conn = (HttpConnection) ModernConnector.open(IAPManager.appendTo(url));
                IAPManager.captureFromSystem();
            } else {
                conn = SocketHttpConnection.open(url);
            }
            conn.setRequestMethod(method);
            conn.setRequestProperty("Authorization",             "Bearer " + developerToken);
            conn.setRequestProperty("x-apple-music-user-token", userToken);
            conn.setRequestProperty("media-user-token", userToken);
            conn.setRequestProperty("Origin",  "https://beta.music.apple.com");
            conn.setRequestProperty("Referer", "https://beta.music.apple.com");
            conn.setRequestProperty("Accept",  "*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            conn.setRequestProperty("User-Agent", "WebKit/525.13 (KHTML, like Gecko) Version/3.1 Safari/525.13");
            if (body != null) {
                byte[] bodyBytes = body.getBytes();
                conn.setRequestProperty("Content-Type",   "application/json");
                conn.setRequestProperty("Content-Length",  String.valueOf(bodyBytes.length));
                out = conn.openOutputStream();
                out.write(bodyBytes);
                out.flush();
            }
            int    status  = conn.getResponseCode();
            in             = conn.openInputStream();
            String response = new String(IOUtils.readAll(in), "UTF-8");
            if (status != HttpConnection.HTTP_OK)
                throw new Exception("HTTP " + status + ": " + response.substring(
                    0, Math.min(120, response.length())));
            return response;
        } finally {
            IOUtils.closeQuietly(out);
            IOUtils.closeQuietly(in);
            IOUtils.closeConn(conn);
        }      
        
    }
    
    public JSONObject APIRequest(String endpoint, Hashtable queryParameters, String method, String body, String prefix) throws Exception {
        String strobj = APIRequestString(endpoint, queryParameters, method, body, prefix);       
        return JSON.getObject(strobj);
    
    }

    public void APIRequestAsync(final String endpoint, final Hashtable params,
                               final String method, final String body,
                               final AMAPIListener listener) {
      new Thread(new Runnable() {
          public void run() {
              try {
                  JSONObject result = APIRequest(endpoint, params, method, body, null);
                  listener.onResponse(result);
              } catch (Exception e) {
                  listener.onError(e);
              }
          }
      }).start();
    }



  
}
