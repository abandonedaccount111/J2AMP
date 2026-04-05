package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ProvisioningRequest implements Message {
    
    public static class EncryptedSessionKeys implements Message {
      
    public static class SessionKeys implements Message {
      
  
  
    protected byte[] encryptionKey; // 1
    protected boolean _hasEncryptionKey;
    protected byte[] macKey; // 2
    protected boolean _hasMacKey;
    
    public byte[] getEncryptionKey() {
        return encryptionKey;
    }
    
    public void setEncryptionKey(byte[] encryptionKey) {
        this.encryptionKey = encryptionKey;
        this._hasEncryptionKey = true;
    }
    
    public void clearEncryptionKey() {
        _hasEncryptionKey = false;
    }
    
    public boolean hasEncryptionKey() {
        return _hasEncryptionKey;
    }
    public byte[] getMacKey() {
        return macKey;
    }
    
    public void setMacKey(byte[] macKey) {
        this.macKey = macKey;
        this._hasMacKey = true;
    }
    
    public void clearMacKey() {
        _hasMacKey = false;
    }
    
    public boolean hasMacKey() {
        return _hasMacKey;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasEncryptionKey)
            out.writeBytes(1, encryptionKey);
        
        if(_hasMacKey)
            out.writeBytes(2, macKey);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    encryptionKey = in.readBytes();
                    _hasEncryptionKey = true;
                    break; }
                case 18: {
                    macKey = in.readBytes();
                    _hasMacKey = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static SessionKeys fromBytes(byte[] in) throws EncodingException {
        SessionKeys message = new SessionKeys();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
  
    protected byte[] certificateSerialNumber; // 1
    protected boolean _hasCertificateSerialNumber;
    protected byte[] encryptedSessionKeys; // 2
    protected boolean _hasEncryptedSessionKeys;
    
    public byte[] getCertificateSerialNumber() {
        return certificateSerialNumber;
    }
    
    public void setCertificateSerialNumber(byte[] certificateSerialNumber) {
        this.certificateSerialNumber = certificateSerialNumber;
        this._hasCertificateSerialNumber = true;
    }
    
    public void clearCertificateSerialNumber() {
        _hasCertificateSerialNumber = false;
    }
    
    public boolean hasCertificateSerialNumber() {
        return _hasCertificateSerialNumber;
    }
    public byte[] getEncryptedSessionKeys() {
        return encryptedSessionKeys;
    }
    
    public void setEncryptedSessionKeys(byte[] encryptedSessionKeys) {
        this.encryptedSessionKeys = encryptedSessionKeys;
        this._hasEncryptedSessionKeys = true;
    }
    
    public void clearEncryptedSessionKeys() {
        _hasEncryptedSessionKeys = false;
    }
    
    public boolean hasEncryptedSessionKeys() {
        return _hasEncryptedSessionKeys;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasCertificateSerialNumber)
            out.writeBytes(1, certificateSerialNumber);
        
        if(_hasEncryptedSessionKeys)
            out.writeBytes(2, encryptedSessionKeys);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    certificateSerialNumber = in.readBytes();
                    _hasCertificateSerialNumber = true;
                    break; }
                case 18: {
                    encryptedSessionKeys = in.readBytes();
                    _hasEncryptedSessionKeys = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static EncryptedSessionKeys fromBytes(byte[] in) throws EncodingException {
        EncryptedSessionKeys message = new EncryptedSessionKeys();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class AndroidAttestationOtaKeyboxRequest implements Message {
      
  
  
    protected byte[] otaRequest; // 1
    protected boolean _hasOtaRequest;
    
    public byte[] getOtaRequest() {
        return otaRequest;
    }
    
    public void setOtaRequest(byte[] otaRequest) {
        this.otaRequest = otaRequest;
        this._hasOtaRequest = true;
    }
    
    public void clearOtaRequest() {
        _hasOtaRequest = false;
    }
    
    public boolean hasOtaRequest() {
        return _hasOtaRequest;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasOtaRequest)
            out.writeBytes(1, otaRequest);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    otaRequest = in.readBytes();
                    _hasOtaRequest = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static AndroidAttestationOtaKeyboxRequest fromBytes(byte[] in) throws EncodingException {
        AndroidAttestationOtaKeyboxRequest message = new AndroidAttestationOtaKeyboxRequest();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
  
    protected ClientIdentification clientId; // 1
    protected EncryptedClientIdentification encryptedClientId; // 5
    protected byte[] nonce; // 2
    protected boolean _hasNonce;
    protected ProvisioningOptions options; // 3
    protected byte[] stableId; // 4
    protected boolean _hasStableId;
    protected byte[] providerId; // 6
    protected boolean _hasProviderId;
    protected byte[] spoid; // 7
    protected boolean _hasSpoid;
    protected ProvisioningRequest.EncryptedSessionKeys encryptedSessionKeys; // 8
    protected ProvisioningRequest.AndroidAttestationOtaKeyboxRequest androidOtaKeyboxRequest; // 9
    protected PublicKeyToCertify certificatePublicKey; // 10
    
    public ClientIdentification getClientId() {
        return clientId;
    }
    
    public void setClientId(ClientIdentification clientId) {
        this.clientId = clientId;
    }
    
    public void clearClientId() {
        clientId = null;
    }
    
    public boolean hasClientId() {
        return clientId != null;
    }
    public EncryptedClientIdentification getEncryptedClientId() {
        return encryptedClientId;
    }
    
    public void setEncryptedClientId(EncryptedClientIdentification encryptedClientId) {
        this.encryptedClientId = encryptedClientId;
    }
    
    public void clearEncryptedClientId() {
        encryptedClientId = null;
    }
    
    public boolean hasEncryptedClientId() {
        return encryptedClientId != null;
    }
    public byte[] getNonce() {
        return nonce;
    }
    
    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
        this._hasNonce = true;
    }
    
    public void clearNonce() {
        _hasNonce = false;
    }
    
    public boolean hasNonce() {
        return _hasNonce;
    }
    public ProvisioningOptions getOptions() {
        return options;
    }
    
    public void setOptions(ProvisioningOptions options) {
        this.options = options;
    }
    
    public void clearOptions() {
        options = null;
    }
    
    public boolean hasOptions() {
        return options != null;
    }
    public byte[] getStableId() {
        return stableId;
    }
    
    public void setStableId(byte[] stableId) {
        this.stableId = stableId;
        this._hasStableId = true;
    }
    
    public void clearStableId() {
        _hasStableId = false;
    }
    
    public boolean hasStableId() {
        return _hasStableId;
    }
    public byte[] getProviderId() {
        return providerId;
    }
    
    public void setProviderId(byte[] providerId) {
        this.providerId = providerId;
        this._hasProviderId = true;
    }
    
    public void clearProviderId() {
        _hasProviderId = false;
    }
    
    public boolean hasProviderId() {
        return _hasProviderId;
    }
    public byte[] getSpoid() {
        return spoid;
    }
    
    public void setSpoid(byte[] spoid) {
        this.spoid = spoid;
        this._hasSpoid = true;
    }
    
    public void clearSpoid() {
        _hasSpoid = false;
    }
    
    public boolean hasSpoid() {
        return _hasSpoid;
    }
    public ProvisioningRequest.EncryptedSessionKeys getEncryptedSessionKeys() {
        return encryptedSessionKeys;
    }
    
    public void setEncryptedSessionKeys(ProvisioningRequest.EncryptedSessionKeys encryptedSessionKeys) {
        this.encryptedSessionKeys = encryptedSessionKeys;
    }
    
    public void clearEncryptedSessionKeys() {
        encryptedSessionKeys = null;
    }
    
    public boolean hasEncryptedSessionKeys() {
        return encryptedSessionKeys != null;
    }
    public ProvisioningRequest.AndroidAttestationOtaKeyboxRequest getAndroidOtaKeyboxRequest() {
        return androidOtaKeyboxRequest;
    }
    
    public void setAndroidOtaKeyboxRequest(ProvisioningRequest.AndroidAttestationOtaKeyboxRequest androidOtaKeyboxRequest) {
        this.androidOtaKeyboxRequest = androidOtaKeyboxRequest;
    }
    
    public void clearAndroidOtaKeyboxRequest() {
        androidOtaKeyboxRequest = null;
    }
    
    public boolean hasAndroidOtaKeyboxRequest() {
        return androidOtaKeyboxRequest != null;
    }
    public PublicKeyToCertify getCertificatePublicKey() {
        return certificatePublicKey;
    }
    
    public void setCertificatePublicKey(PublicKeyToCertify certificatePublicKey) {
        this.certificatePublicKey = certificatePublicKey;
    }
    
    public void clearCertificatePublicKey() {
        certificatePublicKey = null;
    }
    
    public boolean hasCertificatePublicKey() {
        return certificatePublicKey != null;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        out.writeMessage(1, clientId);
        
        out.writeMessage(5, encryptedClientId);
        
        if(_hasNonce)
            out.writeBytes(2, nonce);
        
        out.writeMessage(3, options);
        
        if(_hasStableId)
            out.writeBytes(4, stableId);
        
        if(_hasProviderId)
            out.writeBytes(6, providerId);
        
        if(_hasSpoid)
            out.writeBytes(7, spoid);
        
        out.writeMessage(8, encryptedSessionKeys);
        
        out.writeMessage(9, androidOtaKeyboxRequest);
        
        out.writeMessage(10, certificatePublicKey);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    clientId = new ClientIdentification();
                    in.readMessage(clientId);
                    break; }
                case 42: {
                    encryptedClientId = new EncryptedClientIdentification();
                    in.readMessage(encryptedClientId);
                    break; }
                case 18: {
                    nonce = in.readBytes();
                    _hasNonce = true;
                    break; }
                case 26: {
                    options = new ProvisioningOptions();
                    in.readMessage(options);
                    break; }
                case 34: {
                    stableId = in.readBytes();
                    _hasStableId = true;
                    break; }
                case 50: {
                    providerId = in.readBytes();
                    _hasProviderId = true;
                    break; }
                case 58: {
                    spoid = in.readBytes();
                    _hasSpoid = true;
                    break; }
                case 66: {
                    encryptedSessionKeys = new ProvisioningRequest.EncryptedSessionKeys();
                    in.readMessage(encryptedSessionKeys);
                    break; }
                case 74: {
                    androidOtaKeyboxRequest = new ProvisioningRequest.AndroidAttestationOtaKeyboxRequest();
                    in.readMessage(androidOtaKeyboxRequest);
                    break; }
                case 82: {
                    certificatePublicKey = new PublicKeyToCertify();
                    in.readMessage(certificatePublicKey);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ProvisioningRequest fromBytes(byte[] in) throws EncodingException {
        ProvisioningRequest message = new ProvisioningRequest();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



