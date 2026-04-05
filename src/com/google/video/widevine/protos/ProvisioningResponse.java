package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ProvisioningResponse implements Message {
    
    public static class OtaKeybox implements Message {
      
  
  
    protected byte[] deviceKeyEncryptionIv; // 1
    protected boolean _hasDeviceKeyEncryptionIv;
    protected byte[] encryptedDeviceKey; // 2
    protected boolean _hasEncryptedDeviceKey;
    protected byte[] deviceCaToken; // 3
    protected boolean _hasDeviceCaToken;
    
    public byte[] getDeviceKeyEncryptionIv() {
        return deviceKeyEncryptionIv;
    }
    
    public void setDeviceKeyEncryptionIv(byte[] deviceKeyEncryptionIv) {
        this.deviceKeyEncryptionIv = deviceKeyEncryptionIv;
        this._hasDeviceKeyEncryptionIv = true;
    }
    
    public void clearDeviceKeyEncryptionIv() {
        _hasDeviceKeyEncryptionIv = false;
    }
    
    public boolean hasDeviceKeyEncryptionIv() {
        return _hasDeviceKeyEncryptionIv;
    }
    public byte[] getEncryptedDeviceKey() {
        return encryptedDeviceKey;
    }
    
    public void setEncryptedDeviceKey(byte[] encryptedDeviceKey) {
        this.encryptedDeviceKey = encryptedDeviceKey;
        this._hasEncryptedDeviceKey = true;
    }
    
    public void clearEncryptedDeviceKey() {
        _hasEncryptedDeviceKey = false;
    }
    
    public boolean hasEncryptedDeviceKey() {
        return _hasEncryptedDeviceKey;
    }
    public byte[] getDeviceCaToken() {
        return deviceCaToken;
    }
    
    public void setDeviceCaToken(byte[] deviceCaToken) {
        this.deviceCaToken = deviceCaToken;
        this._hasDeviceCaToken = true;
    }
    
    public void clearDeviceCaToken() {
        _hasDeviceCaToken = false;
    }
    
    public boolean hasDeviceCaToken() {
        return _hasDeviceCaToken;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasDeviceKeyEncryptionIv)
            out.writeBytes(1, deviceKeyEncryptionIv);
        
        if(_hasEncryptedDeviceKey)
            out.writeBytes(2, encryptedDeviceKey);
        
        if(_hasDeviceCaToken)
            out.writeBytes(3, deviceCaToken);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    deviceKeyEncryptionIv = in.readBytes();
                    _hasDeviceKeyEncryptionIv = true;
                    break; }
                case 18: {
                    encryptedDeviceKey = in.readBytes();
                    _hasEncryptedDeviceKey = true;
                    break; }
                case 26: {
                    deviceCaToken = in.readBytes();
                    _hasDeviceCaToken = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static OtaKeybox fromBytes(byte[] in) throws EncodingException {
        OtaKeybox message = new OtaKeybox();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class AndroidAttestationOtaKeyboxResponse implements Message {
      
  
  
    protected byte[] otaResponse; // 1
    protected boolean _hasOtaResponse;
    
    public byte[] getOtaResponse() {
        return otaResponse;
    }
    
    public void setOtaResponse(byte[] otaResponse) {
        this.otaResponse = otaResponse;
        this._hasOtaResponse = true;
    }
    
    public void clearOtaResponse() {
        _hasOtaResponse = false;
    }
    
    public boolean hasOtaResponse() {
        return _hasOtaResponse;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasOtaResponse)
            out.writeBytes(1, otaResponse);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    otaResponse = in.readBytes();
                    _hasOtaResponse = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static AndroidAttestationOtaKeyboxResponse fromBytes(byte[] in) throws EncodingException {
        AndroidAttestationOtaKeyboxResponse message = new AndroidAttestationOtaKeyboxResponse();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // ProvisioningStatus
    public static final int NO_ERROR = 0;
    public static final int REVOKED_DEVICE_CREDENTIALS = 1;
    public static final int REVOKED_DEVICE_SERIES = 2;
  
    protected byte[] deviceRsaKey; // 1
    protected boolean _hasDeviceRsaKey;
    protected byte[] deviceRsaKeyIv; // 2
    protected boolean _hasDeviceRsaKeyIv;
    protected byte[] deviceCertificate; // 3
    protected boolean _hasDeviceCertificate;
    protected byte[] nonce; // 4
    protected boolean _hasNonce;
    protected byte[] wrappingKey; // 5
    protected boolean _hasWrappingKey;
    protected ProvisioningResponse.OtaKeybox otaKeybox; // 6
    protected int status; // 7
    protected boolean _hasStatus;
    protected ProvisioningResponse.AndroidAttestationOtaKeyboxResponse androidOtaKeyboxResponse; // 8
    
    public byte[] getDeviceRsaKey() {
        return deviceRsaKey;
    }
    
    public void setDeviceRsaKey(byte[] deviceRsaKey) {
        this.deviceRsaKey = deviceRsaKey;
        this._hasDeviceRsaKey = true;
    }
    
    public void clearDeviceRsaKey() {
        _hasDeviceRsaKey = false;
    }
    
    public boolean hasDeviceRsaKey() {
        return _hasDeviceRsaKey;
    }
    public byte[] getDeviceRsaKeyIv() {
        return deviceRsaKeyIv;
    }
    
    public void setDeviceRsaKeyIv(byte[] deviceRsaKeyIv) {
        this.deviceRsaKeyIv = deviceRsaKeyIv;
        this._hasDeviceRsaKeyIv = true;
    }
    
    public void clearDeviceRsaKeyIv() {
        _hasDeviceRsaKeyIv = false;
    }
    
    public boolean hasDeviceRsaKeyIv() {
        return _hasDeviceRsaKeyIv;
    }
    public byte[] getDeviceCertificate() {
        return deviceCertificate;
    }
    
    public void setDeviceCertificate(byte[] deviceCertificate) {
        this.deviceCertificate = deviceCertificate;
        this._hasDeviceCertificate = true;
    }
    
    public void clearDeviceCertificate() {
        _hasDeviceCertificate = false;
    }
    
    public boolean hasDeviceCertificate() {
        return _hasDeviceCertificate;
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
    public byte[] getWrappingKey() {
        return wrappingKey;
    }
    
    public void setWrappingKey(byte[] wrappingKey) {
        this.wrappingKey = wrappingKey;
        this._hasWrappingKey = true;
    }
    
    public void clearWrappingKey() {
        _hasWrappingKey = false;
    }
    
    public boolean hasWrappingKey() {
        return _hasWrappingKey;
    }
    public ProvisioningResponse.OtaKeybox getOtaKeybox() {
        return otaKeybox;
    }
    
    public void setOtaKeybox(ProvisioningResponse.OtaKeybox otaKeybox) {
        this.otaKeybox = otaKeybox;
    }
    
    public void clearOtaKeybox() {
        otaKeybox = null;
    }
    
    public boolean hasOtaKeybox() {
        return otaKeybox != null;
    }
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
        this._hasStatus = true;
    }
    
    public void clearStatus() {
        _hasStatus = false;
    }
    
    public boolean hasStatus() {
        return _hasStatus;
    }
    public ProvisioningResponse.AndroidAttestationOtaKeyboxResponse getAndroidOtaKeyboxResponse() {
        return androidOtaKeyboxResponse;
    }
    
    public void setAndroidOtaKeyboxResponse(ProvisioningResponse.AndroidAttestationOtaKeyboxResponse androidOtaKeyboxResponse) {
        this.androidOtaKeyboxResponse = androidOtaKeyboxResponse;
    }
    
    public void clearAndroidOtaKeyboxResponse() {
        androidOtaKeyboxResponse = null;
    }
    
    public boolean hasAndroidOtaKeyboxResponse() {
        return androidOtaKeyboxResponse != null;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasDeviceRsaKey)
            out.writeBytes(1, deviceRsaKey);
        
        if(_hasDeviceRsaKeyIv)
            out.writeBytes(2, deviceRsaKeyIv);
        
        if(_hasDeviceCertificate)
            out.writeBytes(3, deviceCertificate);
        
        if(_hasNonce)
            out.writeBytes(4, nonce);
        
        if(_hasWrappingKey)
            out.writeBytes(5, wrappingKey);
        
        out.writeMessage(6, otaKeybox);
        
        if(_hasStatus)
            out.writeInt32(7, status);
        
        out.writeMessage(8, androidOtaKeyboxResponse);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    deviceRsaKey = in.readBytes();
                    _hasDeviceRsaKey = true;
                    break; }
                case 18: {
                    deviceRsaKeyIv = in.readBytes();
                    _hasDeviceRsaKeyIv = true;
                    break; }
                case 26: {
                    deviceCertificate = in.readBytes();
                    _hasDeviceCertificate = true;
                    break; }
                case 34: {
                    nonce = in.readBytes();
                    _hasNonce = true;
                    break; }
                case 42: {
                    wrappingKey = in.readBytes();
                    _hasWrappingKey = true;
                    break; }
                case 50: {
                    otaKeybox = new ProvisioningResponse.OtaKeybox();
                    in.readMessage(otaKeybox);
                    break; }
                case 56: {
                    status = in.readInt32();
                    _hasStatus = true;
                    break; }
                case 66: {
                    androidOtaKeyboxResponse = new ProvisioningResponse.AndroidAttestationOtaKeyboxResponse();
                    in.readMessage(androidOtaKeyboxResponse);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ProvisioningResponse fromBytes(byte[] in) throws EncodingException {
        ProvisioningResponse message = new ProvisioningResponse();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



