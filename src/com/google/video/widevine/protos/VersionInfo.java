package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class VersionInfo implements Message {
    
  
  
    protected String licenseSdkVersion; // 1
    protected boolean _hasLicenseSdkVersion;
    protected String licenseServiceVersion; // 2
    protected boolean _hasLicenseServiceVersion;
    
    public String getLicenseSdkVersion() {
        return licenseSdkVersion;
    }
    
    public void setLicenseSdkVersion(String licenseSdkVersion) {
        this.licenseSdkVersion = licenseSdkVersion;
        this._hasLicenseSdkVersion = true;
    }
    
    public void clearLicenseSdkVersion() {
        _hasLicenseSdkVersion = false;
    }
    
    public boolean hasLicenseSdkVersion() {
        return _hasLicenseSdkVersion;
    }
    public String getLicenseServiceVersion() {
        return licenseServiceVersion;
    }
    
    public void setLicenseServiceVersion(String licenseServiceVersion) {
        this.licenseServiceVersion = licenseServiceVersion;
        this._hasLicenseServiceVersion = true;
    }
    
    public void clearLicenseServiceVersion() {
        _hasLicenseServiceVersion = false;
    }
    
    public boolean hasLicenseServiceVersion() {
        return _hasLicenseServiceVersion;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasLicenseSdkVersion)
            out.writeString(1, licenseSdkVersion);
        
        if(_hasLicenseServiceVersion)
            out.writeString(2, licenseServiceVersion);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    licenseSdkVersion = in.readString();
                    _hasLicenseSdkVersion = true;
                    break; }
                case 18: {
                    licenseServiceVersion = in.readString();
                    _hasLicenseServiceVersion = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static VersionInfo fromBytes(byte[] in) throws EncodingException {
        VersionInfo message = new VersionInfo();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



