package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class SignedProvisioningMessage implements Message {
    
  
    // ProvisioningProtocolVersion
    public static final int VERSION_UNSPECIFIED = 0;
    public static final int VERSION_1 = 1;
    public static final int VERSION_1_1 = 2;
    // ProvisioningType
    public static final int PROVISIONING_TYPE_UNSPECIFIED = 0;
    public static final int SERVICE_CERTIFICATE_REQUEST = 1;
    public static final int PROVISIONING_20 = 2;
    public static final int PROVISIONING_30 = 3;
    public static final int PROVISIONING_40 = 5;
    public static final int ARCPP_PROVISIONING = 4;
    public static final int ANDROID_ATTESTATION_KEYBOX_OTA = 6;
    public static final int DRM_REPROVISIONING = 7;
    public static final int INTEL_SIGMA_101 = 101;
    public static final int INTEL_SIGMA_210 = 210;
    // SessionKeyType
    public static final int UNDEFINED = 0;
    public static final int WRAPPED_AES_KEY = 1;
    public static final int EPHEMERAL_ECC_PUBLIC_KEY = 2;
  
    protected byte[] message; // 1
    protected boolean _hasMessage;
    protected byte[] signature; // 2
    protected boolean _hasSignature;
    protected int provisioningType; // 3
    protected boolean _hasProvisioningType;
    protected SignedProvisioningContext signedProvisioningContext; // 4
    protected RemoteAttestation remoteAttestation; // 5
    protected byte[] oemcryptoCoreMessage; // 6
    protected boolean _hasOemcryptoCoreMessage;
    protected int hashAlgorithm; // 7
    protected boolean _hasHashAlgorithm;
    protected int protocolVersion; // 8
    protected boolean _hasProtocolVersion;
    protected byte[] sessionKey; // 9
    protected boolean _hasSessionKey;
    protected int sessionKeyType; // 10
    protected boolean _hasSessionKeyType;
    
    public byte[] getMessage() {
        return message;
    }
    
    public void setMessage(byte[] message) {
        this.message = message;
        this._hasMessage = true;
    }
    
    public void clearMessage() {
        _hasMessage = false;
    }
    
    public boolean hasMessage() {
        return _hasMessage;
    }
    public byte[] getSignature() {
        return signature;
    }
    
    public void setSignature(byte[] signature) {
        this.signature = signature;
        this._hasSignature = true;
    }
    
    public void clearSignature() {
        _hasSignature = false;
    }
    
    public boolean hasSignature() {
        return _hasSignature;
    }
    public int getProvisioningType() {
        return provisioningType;
    }
    
    public void setProvisioningType(int provisioningType) {
        this.provisioningType = provisioningType;
        this._hasProvisioningType = true;
    }
    
    public void clearProvisioningType() {
        _hasProvisioningType = false;
    }
    
    public boolean hasProvisioningType() {
        return _hasProvisioningType;
    }
    public SignedProvisioningContext getSignedProvisioningContext() {
        return signedProvisioningContext;
    }
    
    public void setSignedProvisioningContext(SignedProvisioningContext signedProvisioningContext) {
        this.signedProvisioningContext = signedProvisioningContext;
    }
    
    public void clearSignedProvisioningContext() {
        signedProvisioningContext = null;
    }
    
    public boolean hasSignedProvisioningContext() {
        return signedProvisioningContext != null;
    }
    public RemoteAttestation getRemoteAttestation() {
        return remoteAttestation;
    }
    
    public void setRemoteAttestation(RemoteAttestation remoteAttestation) {
        this.remoteAttestation = remoteAttestation;
    }
    
    public void clearRemoteAttestation() {
        remoteAttestation = null;
    }
    
    public boolean hasRemoteAttestation() {
        return remoteAttestation != null;
    }
    public byte[] getOemcryptoCoreMessage() {
        return oemcryptoCoreMessage;
    }
    
    public void setOemcryptoCoreMessage(byte[] oemcryptoCoreMessage) {
        this.oemcryptoCoreMessage = oemcryptoCoreMessage;
        this._hasOemcryptoCoreMessage = true;
    }
    
    public void clearOemcryptoCoreMessage() {
        _hasOemcryptoCoreMessage = false;
    }
    
    public boolean hasOemcryptoCoreMessage() {
        return _hasOemcryptoCoreMessage;
    }
    public int getHashAlgorithm() {
        return hashAlgorithm;
    }
    
    public void setHashAlgorithm(int hashAlgorithm) {
        this.hashAlgorithm = hashAlgorithm;
        this._hasHashAlgorithm = true;
    }
    
    public void clearHashAlgorithm() {
        _hasHashAlgorithm = false;
    }
    
    public boolean hasHashAlgorithm() {
        return _hasHashAlgorithm;
    }
    public int getProtocolVersion() {
        return protocolVersion;
    }
    
    public void setProtocolVersion(int protocolVersion) {
        this.protocolVersion = protocolVersion;
        this._hasProtocolVersion = true;
    }
    
    public void clearProtocolVersion() {
        _hasProtocolVersion = false;
    }
    
    public boolean hasProtocolVersion() {
        return _hasProtocolVersion;
    }
    public byte[] getSessionKey() {
        return sessionKey;
    }
    
    public void setSessionKey(byte[] sessionKey) {
        this.sessionKey = sessionKey;
        this._hasSessionKey = true;
    }
    
    public void clearSessionKey() {
        _hasSessionKey = false;
    }
    
    public boolean hasSessionKey() {
        return _hasSessionKey;
    }
    public int getSessionKeyType() {
        return sessionKeyType;
    }
    
    public void setSessionKeyType(int sessionKeyType) {
        this.sessionKeyType = sessionKeyType;
        this._hasSessionKeyType = true;
    }
    
    public void clearSessionKeyType() {
        _hasSessionKeyType = false;
    }
    
    public boolean hasSessionKeyType() {
        return _hasSessionKeyType;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasMessage)
            out.writeBytes(1, message);
        
        if(_hasSignature)
            out.writeBytes(2, signature);
        
        if(_hasProvisioningType)
            out.writeInt32(3, provisioningType);
        
        out.writeMessage(4, signedProvisioningContext);
        
        out.writeMessage(5, remoteAttestation);
        
        if(_hasOemcryptoCoreMessage)
            out.writeBytes(6, oemcryptoCoreMessage);
        
        if(_hasHashAlgorithm)
            out.writeInt32(7, hashAlgorithm);
        
        if(_hasProtocolVersion)
            out.writeInt32(8, protocolVersion);
        
        if(_hasSessionKey)
            out.writeBytes(9, sessionKey);
        
        if(_hasSessionKeyType)
            out.writeInt32(10, sessionKeyType);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    message = in.readBytes();
                    _hasMessage = true;
                    break; }
                case 18: {
                    signature = in.readBytes();
                    _hasSignature = true;
                    break; }
                case 24: {
                    provisioningType = in.readInt32();
                    _hasProvisioningType = true;
                    break; }
                case 34: {
                    signedProvisioningContext = new SignedProvisioningContext();
                    in.readMessage(signedProvisioningContext);
                    break; }
                case 42: {
                    remoteAttestation = new RemoteAttestation();
                    in.readMessage(remoteAttestation);
                    break; }
                case 50: {
                    oemcryptoCoreMessage = in.readBytes();
                    _hasOemcryptoCoreMessage = true;
                    break; }
                case 56: {
                    hashAlgorithm = in.readInt32();
                    _hasHashAlgorithm = true;
                    break; }
                case 64: {
                    protocolVersion = in.readInt32();
                    _hasProtocolVersion = true;
                    break; }
                case 74: {
                    sessionKey = in.readBytes();
                    _hasSessionKey = true;
                    break; }
                case 80: {
                    sessionKeyType = in.readInt32();
                    _hasSessionKeyType = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static SignedProvisioningMessage fromBytes(byte[] in) throws EncodingException {
        SignedProvisioningMessage message = new SignedProvisioningMessage();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



