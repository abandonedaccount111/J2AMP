package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class EncryptedClientIdentification implements Message {
    
  
  
    protected String providerId; // 1
    protected boolean _hasProviderId;
    protected byte[] serviceCertificateSerialNumber; // 2
    protected boolean _hasServiceCertificateSerialNumber;
    protected byte[] encryptedClientId; // 3
    protected boolean _hasEncryptedClientId;
    protected byte[] encryptedClientIdIv; // 4
    protected boolean _hasEncryptedClientIdIv;
    protected byte[] encryptedPrivacyKey; // 5
    protected boolean _hasEncryptedPrivacyKey;
    
    public String getProviderId() {
        return providerId;
    }
    
    public void setProviderId(String providerId) {
        this.providerId = providerId;
        this._hasProviderId = true;
    }
    
    public void clearProviderId() {
        _hasProviderId = false;
    }
    
    public boolean hasProviderId() {
        return _hasProviderId;
    }
    public byte[] getServiceCertificateSerialNumber() {
        return serviceCertificateSerialNumber;
    }
    
    public void setServiceCertificateSerialNumber(byte[] serviceCertificateSerialNumber) {
        this.serviceCertificateSerialNumber = serviceCertificateSerialNumber;
        this._hasServiceCertificateSerialNumber = true;
    }
    
    public void clearServiceCertificateSerialNumber() {
        _hasServiceCertificateSerialNumber = false;
    }
    
    public boolean hasServiceCertificateSerialNumber() {
        return _hasServiceCertificateSerialNumber;
    }
    public byte[] getEncryptedClientId() {
        return encryptedClientId;
    }
    
    public void setEncryptedClientId(byte[] encryptedClientId) {
        this.encryptedClientId = encryptedClientId;
        this._hasEncryptedClientId = true;
    }
    
    public void clearEncryptedClientId() {
        _hasEncryptedClientId = false;
    }
    
    public boolean hasEncryptedClientId() {
        return _hasEncryptedClientId;
    }
    public byte[] getEncryptedClientIdIv() {
        return encryptedClientIdIv;
    }
    
    public void setEncryptedClientIdIv(byte[] encryptedClientIdIv) {
        this.encryptedClientIdIv = encryptedClientIdIv;
        this._hasEncryptedClientIdIv = true;
    }
    
    public void clearEncryptedClientIdIv() {
        _hasEncryptedClientIdIv = false;
    }
    
    public boolean hasEncryptedClientIdIv() {
        return _hasEncryptedClientIdIv;
    }
    public byte[] getEncryptedPrivacyKey() {
        return encryptedPrivacyKey;
    }
    
    public void setEncryptedPrivacyKey(byte[] encryptedPrivacyKey) {
        this.encryptedPrivacyKey = encryptedPrivacyKey;
        this._hasEncryptedPrivacyKey = true;
    }
    
    public void clearEncryptedPrivacyKey() {
        _hasEncryptedPrivacyKey = false;
    }
    
    public boolean hasEncryptedPrivacyKey() {
        return _hasEncryptedPrivacyKey;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasProviderId)
            out.writeString(1, providerId);
        
        if(_hasServiceCertificateSerialNumber)
            out.writeBytes(2, serviceCertificateSerialNumber);
        
        if(_hasEncryptedClientId)
            out.writeBytes(3, encryptedClientId);
        
        if(_hasEncryptedClientIdIv)
            out.writeBytes(4, encryptedClientIdIv);
        
        if(_hasEncryptedPrivacyKey)
            out.writeBytes(5, encryptedPrivacyKey);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    providerId = in.readString();
                    _hasProviderId = true;
                    break; }
                case 18: {
                    serviceCertificateSerialNumber = in.readBytes();
                    _hasServiceCertificateSerialNumber = true;
                    break; }
                case 26: {
                    encryptedClientId = in.readBytes();
                    _hasEncryptedClientId = true;
                    break; }
                case 34: {
                    encryptedClientIdIv = in.readBytes();
                    _hasEncryptedClientIdIv = true;
                    break; }
                case 42: {
                    encryptedPrivacyKey = in.readBytes();
                    _hasEncryptedPrivacyKey = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static EncryptedClientIdentification fromBytes(byte[] in) throws EncodingException {
        EncryptedClientIdentification message = new EncryptedClientIdentification();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



