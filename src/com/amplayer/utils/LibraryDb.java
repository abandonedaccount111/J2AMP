package com.amplayer.utils;

import javax.microedition.io.Connector;
import javax.microedition.io.file.FileConnection;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

public class LibraryDb {

    public static final int DB_VERSION = 1;

    // Helper class to hold items
    public static class DbResult {
        public String[] ids;
        public String[] titles;
        public String[] subnames;
        public int length;
    }

    private static String getFilePath(String type) {
        String dir = IOUtils.getCacheDirectory();
        if (dir == null) return null;
        return dir + "lib_" + type + ".db";
    }

    public static void clearAllDb() {
        String[] types = {"songs", "albums", "playlists"};
        for (int i = 0; i < types.length; i++) {
            deleteFile(getFilePath(types[i]));
        }
    }

    private static void deleteFile(String path) {
        if (path == null) return;
        FileConnection fc = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (fc.exists()) fc.delete();
        } catch (Exception e) {
        } finally {
            if (fc != null) try { fc.close(); } catch (Exception e) {}
        }
    }

    public static boolean isValid(String type) {
        if (Settings.dbReloadInterval == 0) return false; // always reload
        String path = getFilePath(type);
        if (path == null) return false;
        
        FileConnection fc = null;
        InputStream in = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ);
            if (!fc.exists()) return false;
            
            in = fc.openInputStream();
            byte[] header = new byte[7];
            int read = in.read(header);
            if (read < 7) return false;
            
            // Check version
            if ((header[0] & 0xFF) != DB_VERSION) return false;
            
            // Check expiry
            if (Settings.dbReloadInterval > 0) {
                long unixDate = ((header[1] & 0xFFL) << 24) |
                                ((header[2] & 0xFFL) << 16) |
                                ((header[3] & 0xFFL) << 8) |
                                 (header[4] & 0xFFL);
                
                long now = System.currentTimeMillis() / 1000L;
                long maxAge = Settings.dbReloadInterval * 86400L;
                if (now - unixDate > maxAge) return false;
            }
            
            return true;
        } catch (Exception e) {
            return false;
        } finally {
            IOUtils.closeQuietly(in);
            if (fc != null) try { fc.close(); } catch (Exception e) {}
        }
    }

    public static void save(String type, Vector ids, Vector titles, Vector subs) {
        String path = getFilePath(type);
        if (path == null) return;
        
        int max = Settings.getMaxItemSize();
        int count = Math.min(ids.size(), max);
        
        FileConnection fc = null;
        OutputStream out = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ_WRITE);
            if (!fc.exists()) fc.create();
            else fc.truncate(0);
            
            out = fc.openOutputStream();
            
            // 7 bytes header
            byte[] header = new byte[7];
            header[0] = (byte) DB_VERSION;
            long now = System.currentTimeMillis() / 1000L;
            header[1] = (byte) ((now >> 24) & 0xFF);
            header[2] = (byte) ((now >> 16) & 0xFF);
            header[3] = (byte) ((now >> 8) & 0xFF);
            header[4] = (byte) (now & 0xFF);
            header[5] = 0; // redundancy
            header[6] = 0;
            out.write(header);
            
            // Generate items buffer and offsets
            int currentOffset = 7 + 2 + (3 * count);
            byte[] indices = new byte[2 + (3 * count)];
            indices[0] = (byte) ((count >> 8) & 0xFF);
            indices[1] = (byte) (count & 0xFF);
            
            ByteArrayOutputStream itemsBuffer = new ByteArrayOutputStream();
            
            for (int i = 0; i < count; i++) {
                String idStr = (String) ids.elementAt(i);
                String titleStr = (String) titles.elementAt(i);
                String subStr = (String) subs.elementAt(i);
                
                // Write offset to indices
                int idxPos = 2 + (3 * i);
                indices[idxPos] = (byte) ((currentOffset >> 16) & 0xFF);
                indices[idxPos + 1] = (byte) ((currentOffset >> 8) & 0xFF);
                indices[idxPos + 2] = (byte) (currentOffset & 0xFF);
                
                // Item creation
                byte[] paddedId = new byte[18];
                byte[] idBytes = idStr.getBytes("UTF-8");
                for (int j = 0; j < 18; j++) {
                    if (j < idBytes.length) paddedId[j] = idBytes[j];
                    else paddedId[j] = (byte) ' ';
                }
                
                byte[] titleBytes = titleStr.getBytes("UTF-8");
                if (titleBytes.length > 255) titleBytes = substringBytes(titleBytes, 255);
                
                byte[] subBytes = subStr.getBytes("UTF-8");
                if (subBytes.length > 255) subBytes = substringBytes(subBytes, 255);
                
                itemsBuffer.write(paddedId);
                itemsBuffer.write(titleBytes.length);
                itemsBuffer.write(subBytes.length);
                itemsBuffer.write(titleBytes);
                itemsBuffer.write(subBytes);
                
                currentOffset += 18 + 1 + 1 + titleBytes.length + subBytes.length;
            }
            
            out.write(indices);
            out.write(itemsBuffer.toByteArray());
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
            if (fc != null) try { fc.close(); } catch (Exception e) {}
        }
    }

    private static byte[] substringBytes(byte[] source, int length) {
        byte[] res = new byte[length];
        System.arraycopy(source, 0, res, 0, length);
        return res;
    }

    public static DbResult read(String type, String filterQuery) {
        String path = getFilePath(type);
        if (path == null) return null;
        
        FileConnection fc = null;
        InputStream in = null;
        try {
            fc = (FileConnection) Connector.open(path, Connector.READ);
            if (!fc.exists()) return null;
            
            in = fc.openInputStream();
            byte[] fileData = IOUtils.readAll(in);
            
            if (fileData.length < 9) return null;
            
            int count = ((fileData[7] & 0xFF) << 8) | (fileData[8] & 0xFF);
            
            byte[] queryBytes = null;
            if (filterQuery != null && filterQuery.length() > 0) {
                queryBytes = filterQuery.toLowerCase().getBytes("UTF-8");
            }
            
            Vector matchedIds = new Vector();
            Vector matchedTitles = new Vector();
            Vector matchedSubs = new Vector();
            
            int[] offsets = new int[count];
            for (int i = 0; i < count; i++) {
                int pos = 9 + (3 * i);
                offsets[i] = ((fileData[pos] & 0xFF) << 16) | ((fileData[pos+1] & 0xFF) << 8) | (fileData[pos+2] & 0xFF);
            }
            
            for (int i = 0; i < count; i++) {
                int itemStart = offsets[i];
                int itemEnd = (i == count - 1) ? fileData.length : offsets[i+1];
                
                // Read specific sections
                if (itemStart + 20 > fileData.length) break;
                
                String id = new String(fileData, itemStart, 18, "UTF-8").trim();
                int titleLen = fileData[itemStart + 18] & 0xFF;
                int subLen = fileData[itemStart + 19] & 0xFF;
                
                int titleStart = itemStart + 20;
                
                boolean match = true;
                if (queryBytes != null) {
                    match = indexOfIgnoreCase(fileData, titleStart, itemEnd - 1, queryBytes);
                }
                
                if (match) {
                    matchedIds.addElement(id);
                    matchedTitles.addElement(new String(fileData, titleStart, titleLen, "UTF-8"));
                    matchedSubs.addElement(new String(fileData, titleStart + titleLen, subLen, "UTF-8"));
                }
            }
            
            DbResult res = new DbResult();
            res.length = matchedIds.size();
            res.ids = new String[res.length];
            res.titles = new String[res.length];
            res.subnames = new String[res.length];
            for (int i = 0; i < res.length; i++) {
                res.ids[i] = (String) matchedIds.elementAt(i);
                res.titles[i] = (String) matchedTitles.elementAt(i);
                res.subnames[i] = (String) matchedSubs.elementAt(i);
            }
            
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            IOUtils.closeQuietly(in);
            if (fc != null) try { fc.close(); } catch (Exception e) {}
        }
    }
    
    // Very simple byte matching for combining title+subtext filter bounds (itemStart+20 to itemEnd-1)
    private static boolean indexOfIgnoreCase(byte[] data, int startIdx, int maxIdx, byte[] queryBytes) {
        int limit = maxIdx - queryBytes.length + 1;
        for (int i = startIdx; i <= limit; i++) {
            boolean found = true;
            for (int j = 0; j < queryBytes.length; j++) {
                byte dbByte = data[i + j];
                if (dbByte >= 'A' && dbByte <= 'Z') dbByte = (byte)(dbByte + 32); // fast lower
                if (dbByte != queryBytes[j]) {
                    found = false;
                    break;
                }
            }
            if (found) return true;
        }
        return false;
    }
}
