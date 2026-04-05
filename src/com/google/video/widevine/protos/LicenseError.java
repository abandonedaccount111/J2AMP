package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class LicenseError implements Message {
    
  
    // Error
    public static final int INVALID_DRM_DEVICE_CERTIFICATE = 1;
    public static final int REVOKED_DRM_DEVICE_CERTIFICATE = 2;
    public static final int SERVICE_UNAVAILABLE = 3;
    public static final int EXPIRED_DRM_DEVICE_CERTIFICATE = 4;
  
    protected int errorCode; // 1
    protected boolean _hasErrorCode;
    
    public int getErrorCode() {
        return errorCode;
    }
    
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
        this._hasErrorCode = true;
    }
    
    public void clearErrorCode() {
        _hasErrorCode = false;
    }
    
    public boolean hasErrorCode() {
        return _hasErrorCode;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasErrorCode)
            out.writeInt32(1, errorCode);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    errorCode = in.readInt32();
                    _hasErrorCode = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static LicenseError fromBytes(byte[] in) throws EncodingException {
        LicenseError message = new LicenseError();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



