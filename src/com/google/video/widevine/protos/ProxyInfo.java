package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ProxyInfo implements Message {
    
  
  
    protected int sdkType; // 1
    protected boolean _hasSdkType;
    protected String sdkVersion; // 2
    protected boolean _hasSdkVersion;
    
    public int getSdkType() {
        return sdkType;
    }
    
    public void setSdkType(int sdkType) {
        this.sdkType = sdkType;
        this._hasSdkType = true;
    }
    
    public void clearSdkType() {
        _hasSdkType = false;
    }
    
    public boolean hasSdkType() {
        return _hasSdkType;
    }
    public String getSdkVersion() {
        return sdkVersion;
    }
    
    public void setSdkVersion(String sdkVersion) {
        this.sdkVersion = sdkVersion;
        this._hasSdkVersion = true;
    }
    
    public void clearSdkVersion() {
        _hasSdkVersion = false;
    }
    
    public boolean hasSdkVersion() {
        return _hasSdkVersion;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasSdkType)
            out.writeInt32(1, sdkType);
        
        if(_hasSdkVersion)
            out.writeString(2, sdkVersion);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    sdkType = in.readInt32();
                    _hasSdkType = true;
                    break; }
                case 18: {
                    sdkVersion = in.readString();
                    _hasSdkVersion = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ProxyInfo fromBytes(byte[] in) throws EncodingException {
        ProxyInfo message = new ProxyInfo();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



