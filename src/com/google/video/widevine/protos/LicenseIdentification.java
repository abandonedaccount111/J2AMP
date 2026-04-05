package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class LicenseIdentification implements Message {
    
  
  
    protected byte[] requestId; // 1
    protected boolean _hasRequestId;
    protected byte[] sessionId; // 2
    protected boolean _hasSessionId;
    protected byte[] purchaseId; // 3
    protected boolean _hasPurchaseId;
    protected int type; // 4
    protected boolean _hasType;
    protected int version; // 5
    protected boolean _hasVersion;
    protected byte[] providerSessionToken; // 6
    protected boolean _hasProviderSessionToken;
    protected long originalRentalDurationSeconds; // 7
    protected boolean _hasOriginalRentalDurationSeconds;
    protected long originalPlaybackDurationSeconds; // 8
    protected boolean _hasOriginalPlaybackDurationSeconds;
    protected long originalStartTimeSeconds; // 9
    protected boolean _hasOriginalStartTimeSeconds;
    protected long originalRenewalRecoveryDurationSeconds; // 10
    protected boolean _hasOriginalRenewalRecoveryDurationSeconds;
    protected long originalRenewalDelaySeconds; // 11
    protected boolean _hasOriginalRenewalDelaySeconds;
    
    public byte[] getRequestId() {
        return requestId;
    }
    
    public void setRequestId(byte[] requestId) {
        this.requestId = requestId;
        this._hasRequestId = true;
    }
    
    public void clearRequestId() {
        _hasRequestId = false;
    }
    
    public boolean hasRequestId() {
        return _hasRequestId;
    }
    public byte[] getSessionId() {
        return sessionId;
    }
    
    public void setSessionId(byte[] sessionId) {
        this.sessionId = sessionId;
        this._hasSessionId = true;
    }
    
    public void clearSessionId() {
        _hasSessionId = false;
    }
    
    public boolean hasSessionId() {
        return _hasSessionId;
    }
    public byte[] getPurchaseId() {
        return purchaseId;
    }
    
    public void setPurchaseId(byte[] purchaseId) {
        this.purchaseId = purchaseId;
        this._hasPurchaseId = true;
    }
    
    public void clearPurchaseId() {
        _hasPurchaseId = false;
    }
    
    public boolean hasPurchaseId() {
        return _hasPurchaseId;
    }
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
        this._hasType = true;
    }
    
    public void clearType() {
        _hasType = false;
    }
    
    public boolean hasType() {
        return _hasType;
    }
    public int getVersion() {
        return version;
    }
    
    public void setVersion(int version) {
        this.version = version;
        this._hasVersion = true;
    }
    
    public void clearVersion() {
        _hasVersion = false;
    }
    
    public boolean hasVersion() {
        return _hasVersion;
    }
    public byte[] getProviderSessionToken() {
        return providerSessionToken;
    }
    
    public void setProviderSessionToken(byte[] providerSessionToken) {
        this.providerSessionToken = providerSessionToken;
        this._hasProviderSessionToken = true;
    }
    
    public void clearProviderSessionToken() {
        _hasProviderSessionToken = false;
    }
    
    public boolean hasProviderSessionToken() {
        return _hasProviderSessionToken;
    }
    public long getOriginalRentalDurationSeconds() {
        return originalRentalDurationSeconds;
    }
    
    public void setOriginalRentalDurationSeconds(long originalRentalDurationSeconds) {
        this.originalRentalDurationSeconds = originalRentalDurationSeconds;
        this._hasOriginalRentalDurationSeconds = true;
    }
    
    public void clearOriginalRentalDurationSeconds() {
        _hasOriginalRentalDurationSeconds = false;
    }
    
    public boolean hasOriginalRentalDurationSeconds() {
        return _hasOriginalRentalDurationSeconds;
    }
    public long getOriginalPlaybackDurationSeconds() {
        return originalPlaybackDurationSeconds;
    }
    
    public void setOriginalPlaybackDurationSeconds(long originalPlaybackDurationSeconds) {
        this.originalPlaybackDurationSeconds = originalPlaybackDurationSeconds;
        this._hasOriginalPlaybackDurationSeconds = true;
    }
    
    public void clearOriginalPlaybackDurationSeconds() {
        _hasOriginalPlaybackDurationSeconds = false;
    }
    
    public boolean hasOriginalPlaybackDurationSeconds() {
        return _hasOriginalPlaybackDurationSeconds;
    }
    public long getOriginalStartTimeSeconds() {
        return originalStartTimeSeconds;
    }
    
    public void setOriginalStartTimeSeconds(long originalStartTimeSeconds) {
        this.originalStartTimeSeconds = originalStartTimeSeconds;
        this._hasOriginalStartTimeSeconds = true;
    }
    
    public void clearOriginalStartTimeSeconds() {
        _hasOriginalStartTimeSeconds = false;
    }
    
    public boolean hasOriginalStartTimeSeconds() {
        return _hasOriginalStartTimeSeconds;
    }
    public long getOriginalRenewalRecoveryDurationSeconds() {
        return originalRenewalRecoveryDurationSeconds;
    }
    
    public void setOriginalRenewalRecoveryDurationSeconds(long originalRenewalRecoveryDurationSeconds) {
        this.originalRenewalRecoveryDurationSeconds = originalRenewalRecoveryDurationSeconds;
        this._hasOriginalRenewalRecoveryDurationSeconds = true;
    }
    
    public void clearOriginalRenewalRecoveryDurationSeconds() {
        _hasOriginalRenewalRecoveryDurationSeconds = false;
    }
    
    public boolean hasOriginalRenewalRecoveryDurationSeconds() {
        return _hasOriginalRenewalRecoveryDurationSeconds;
    }
    public long getOriginalRenewalDelaySeconds() {
        return originalRenewalDelaySeconds;
    }
    
    public void setOriginalRenewalDelaySeconds(long originalRenewalDelaySeconds) {
        this.originalRenewalDelaySeconds = originalRenewalDelaySeconds;
        this._hasOriginalRenewalDelaySeconds = true;
    }
    
    public void clearOriginalRenewalDelaySeconds() {
        _hasOriginalRenewalDelaySeconds = false;
    }
    
    public boolean hasOriginalRenewalDelaySeconds() {
        return _hasOriginalRenewalDelaySeconds;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasRequestId)
            out.writeBytes(1, requestId);
        
        if(_hasSessionId)
            out.writeBytes(2, sessionId);
        
        if(_hasPurchaseId)
            out.writeBytes(3, purchaseId);
        
        if(_hasType)
            out.writeInt32(4, type);
        
        if(_hasVersion)
            out.writeInt32(5, version);
        
        if(_hasProviderSessionToken)
            out.writeBytes(6, providerSessionToken);
        
        if(_hasOriginalRentalDurationSeconds)
            out.writeInt64(7, originalRentalDurationSeconds);
        
        if(_hasOriginalPlaybackDurationSeconds)
            out.writeInt64(8, originalPlaybackDurationSeconds);
        
        if(_hasOriginalStartTimeSeconds)
            out.writeInt64(9, originalStartTimeSeconds);
        
        if(_hasOriginalRenewalRecoveryDurationSeconds)
            out.writeInt64(10, originalRenewalRecoveryDurationSeconds);
        
        if(_hasOriginalRenewalDelaySeconds)
            out.writeInt64(11, originalRenewalDelaySeconds);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    requestId = in.readBytes();
                    _hasRequestId = true;
                    break; }
                case 18: {
                    sessionId = in.readBytes();
                    _hasSessionId = true;
                    break; }
                case 26: {
                    purchaseId = in.readBytes();
                    _hasPurchaseId = true;
                    break; }
                case 32: {
                    type = in.readInt32();
                    _hasType = true;
                    break; }
                case 40: {
                    version = in.readInt32();
                    _hasVersion = true;
                    break; }
                case 50: {
                    providerSessionToken = in.readBytes();
                    _hasProviderSessionToken = true;
                    break; }
                case 56: {
                    originalRentalDurationSeconds = in.readInt64();
                    _hasOriginalRentalDurationSeconds = true;
                    break; }
                case 64: {
                    originalPlaybackDurationSeconds = in.readInt64();
                    _hasOriginalPlaybackDurationSeconds = true;
                    break; }
                case 72: {
                    originalStartTimeSeconds = in.readInt64();
                    _hasOriginalStartTimeSeconds = true;
                    break; }
                case 80: {
                    originalRenewalRecoveryDurationSeconds = in.readInt64();
                    _hasOriginalRenewalRecoveryDurationSeconds = true;
                    break; }
                case 88: {
                    originalRenewalDelaySeconds = in.readInt64();
                    _hasOriginalRenewalDelaySeconds = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static LicenseIdentification fromBytes(byte[] in) throws EncodingException {
        LicenseIdentification message = new LicenseIdentification();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



