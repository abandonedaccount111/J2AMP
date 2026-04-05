/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.amplayer.utils;

import java.io.OutputStream;
import java.util.Enumeration;
import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import javax.microedition.io.file.FileSystemRegistry;

/**
 *
 * @author randomaccount
 */
public class FileUtils {
     public static String findWritableRoot() {
        Enumeration roots = FileSystemRegistry.listRoots();

        while (roots.hasMoreElements()) {
            String root = (String) roots.nextElement();
            String testPath = "file:///" + root + "AMPlayer/";

            FileConnection fc = null;

            try {
                fc = (FileConnection) Connector.open(testPath, Connector.READ_WRITE);

                if (!fc.exists()) {
                    fc.mkdir();
                }

                fc.close();
                return root; // writable root found
            } catch (Exception e) {
                try {
                    if (fc != null) fc.close();
                } catch (Exception ignored) {};
                e.printStackTrace();
            }
        }

        return null; // none writable
    }


    public static void saveBytes(byte[] data, String fileName) throws Exception {

        String root = findWritableRoot();
        if (root == null) {
            throw new Exception("No writable filesystem found");
        }

        String dirPath = "file:///" + root + "AMPlayer/";
        String filePath = dirPath + fileName;

        FileConnection fc = null;
        OutputStream os = null;

        try {
            // ensure directory exists
            fc = (FileConnection) Connector.open(dirPath, Connector.READ_WRITE);
            if (!fc.exists()) {
                fc.mkdir();
            }
            fc.close();

            // open file
            fc = (FileConnection) Connector.open(filePath, Connector.READ_WRITE);

            // overwrite existing file
            if (fc.exists()) {
                fc.delete();
            }

            fc.create();

            os = fc.openOutputStream();
            os.write(data);
            os.flush();

        } finally {
            if (os != null) os.close();
            if (fc != null) fc.close();
        }
    }
}
