package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class LicenseCategorySpec implements Message {
    
  
    // LicenseCategory
    public static final int SINGLE_CONTENT_LICENSE_DEFAULT = 0;
    public static final int MULTI_CONTENT_LICENSE = 1;
    public static final int GROUP_LICENSE = 2;
  
    protected int licenseCategory; // 1
    protected boolean _hasLicenseCategory;
    protected byte[] contentId; // 2
    protected boolean _hasContentId;
    protected byte[] groupId; // 3
    protected boolean _hasGroupId;
    
    public int getLicenseCategory() {
        return licenseCategory;
    }
    
    public void setLicenseCategory(int licenseCategory) {
        this.licenseCategory = licenseCategory;
        this._hasLicenseCategory = true;
    }
    
    public void clearLicenseCategory() {
        _hasLicenseCategory = false;
    }
    
    public boolean hasLicenseCategory() {
        return _hasLicenseCategory;
    }
    public byte[] getContentId() {
        return contentId;
    }
    
    public void setContentId(byte[] contentId) {
        this.contentId = contentId;
        this._hasContentId = true;
    }
    
    public void clearContentId() {
        _hasContentId = false;
    }
    
    public boolean hasContentId() {
        return _hasContentId;
    }
    public byte[] getGroupId() {
        return groupId;
    }
    
    public void setGroupId(byte[] groupId) {
        this.groupId = groupId;
        this._hasGroupId = true;
    }
    
    public void clearGroupId() {
        _hasGroupId = false;
    }
    
    public boolean hasGroupId() {
        return _hasGroupId;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasLicenseCategory)
            out.writeInt32(1, licenseCategory);
        
        if(_hasContentId)
            out.writeBytes(2, contentId);
        
        if(_hasGroupId)
            out.writeBytes(3, groupId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    licenseCategory = in.readInt32();
                    _hasLicenseCategory = true;
                    break; }
                case 18: {
                    contentId = in.readBytes();
                    _hasContentId = true;
                    break; }
                case 26: {
                    groupId = in.readBytes();
                    _hasGroupId = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static LicenseCategorySpec fromBytes(byte[] in) throws EncodingException {
        LicenseCategorySpec message = new LicenseCategorySpec();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



