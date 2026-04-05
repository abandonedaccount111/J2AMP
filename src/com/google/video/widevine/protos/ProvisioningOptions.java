package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ProvisioningOptions implements Message {
    
  
    // CertificateType
    public static final int WIDEVINE_DRM = 0;
    public static final int X509 = 1;
    public static final int WIDEVINE_KEYBOX = 2;
  
    protected int certificateType; // 1
    protected boolean _hasCertificateType;
    protected String certificateAuthority; // 2
    protected boolean _hasCertificateAuthority;
    protected int systemId; // 3
    protected boolean _hasSystemId;
    
    public int getCertificateType() {
        return certificateType;
    }
    
    public void setCertificateType(int certificateType) {
        this.certificateType = certificateType;
        this._hasCertificateType = true;
    }
    
    public void clearCertificateType() {
        _hasCertificateType = false;
    }
    
    public boolean hasCertificateType() {
        return _hasCertificateType;
    }
    public String getCertificateAuthority() {
        return certificateAuthority;
    }
    
    public void setCertificateAuthority(String certificateAuthority) {
        this.certificateAuthority = certificateAuthority;
        this._hasCertificateAuthority = true;
    }
    
    public void clearCertificateAuthority() {
        _hasCertificateAuthority = false;
    }
    
    public boolean hasCertificateAuthority() {
        return _hasCertificateAuthority;
    }
    public int getSystemId() {
        return systemId;
    }
    
    public void setSystemId(int systemId) {
        this.systemId = systemId;
        this._hasSystemId = true;
    }
    
    public void clearSystemId() {
        _hasSystemId = false;
    }
    
    public boolean hasSystemId() {
        return _hasSystemId;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasCertificateType)
            out.writeInt32(1, certificateType);
        
        if(_hasCertificateAuthority)
            out.writeString(2, certificateAuthority);
        
        if(_hasSystemId)
            out.writeUInt32(3, systemId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    certificateType = in.readInt32();
                    _hasCertificateType = true;
                    break; }
                case 18: {
                    certificateAuthority = in.readString();
                    _hasCertificateAuthority = true;
                    break; }
                case 24: {
                    systemId = in.readUInt32();
                    _hasSystemId = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ProvisioningOptions fromBytes(byte[] in) throws EncodingException {
        ProvisioningOptions message = new ProvisioningOptions();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



