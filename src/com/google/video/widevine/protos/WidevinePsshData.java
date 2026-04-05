package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class WidevinePsshData implements Message {
    
    public static class EntitledKey implements Message {
      
  
  
    protected byte[] entitlementKeyId; // 1
    protected boolean _hasEntitlementKeyId;
    protected byte[] keyId; // 2
    protected boolean _hasKeyId;
    protected byte[] key; // 3
    protected boolean _hasKey;
    protected byte[] iv; // 4
    protected boolean _hasIv;
    protected int entitlementKeySizeBytes; // 5
    protected boolean _hasEntitlementKeySizeBytes;
    
    public byte[] getEntitlementKeyId() {
        return entitlementKeyId;
    }
    
    public void setEntitlementKeyId(byte[] entitlementKeyId) {
        this.entitlementKeyId = entitlementKeyId;
        this._hasEntitlementKeyId = true;
    }
    
    public void clearEntitlementKeyId() {
        _hasEntitlementKeyId = false;
    }
    
    public boolean hasEntitlementKeyId() {
        return _hasEntitlementKeyId;
    }
    public byte[] getKeyId() {
        return keyId;
    }
    
    public void setKeyId(byte[] keyId) {
        this.keyId = keyId;
        this._hasKeyId = true;
    }
    
    public void clearKeyId() {
        _hasKeyId = false;
    }
    
    public boolean hasKeyId() {
        return _hasKeyId;
    }
    public byte[] getKey() {
        return key;
    }
    
    public void setKey(byte[] key) {
        this.key = key;
        this._hasKey = true;
    }
    
    public void clearKey() {
        _hasKey = false;
    }
    
    public boolean hasKey() {
        return _hasKey;
    }
    public byte[] getIv() {
        return iv;
    }
    
    public void setIv(byte[] iv) {
        this.iv = iv;
        this._hasIv = true;
    }
    
    public void clearIv() {
        _hasIv = false;
    }
    
    public boolean hasIv() {
        return _hasIv;
    }
    public int getEntitlementKeySizeBytes() {
        return entitlementKeySizeBytes;
    }
    
    public void setEntitlementKeySizeBytes(int entitlementKeySizeBytes) {
        this.entitlementKeySizeBytes = entitlementKeySizeBytes;
        this._hasEntitlementKeySizeBytes = true;
    }
    
    public void clearEntitlementKeySizeBytes() {
        _hasEntitlementKeySizeBytes = false;
    }
    
    public boolean hasEntitlementKeySizeBytes() {
        return _hasEntitlementKeySizeBytes;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasEntitlementKeyId)
            out.writeBytes(1, entitlementKeyId);
        
        if(_hasKeyId)
            out.writeBytes(2, keyId);
        
        if(_hasKey)
            out.writeBytes(3, key);
        
        if(_hasIv)
            out.writeBytes(4, iv);
        
        if(_hasEntitlementKeySizeBytes)
            out.writeUInt32(5, entitlementKeySizeBytes);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    entitlementKeyId = in.readBytes();
                    _hasEntitlementKeyId = true;
                    break; }
                case 18: {
                    keyId = in.readBytes();
                    _hasKeyId = true;
                    break; }
                case 26: {
                    key = in.readBytes();
                    _hasKey = true;
                    break; }
                case 34: {
                    iv = in.readBytes();
                    _hasIv = true;
                    break; }
                case 40: {
                    entitlementKeySizeBytes = in.readUInt32();
                    _hasEntitlementKeySizeBytes = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static EntitledKey fromBytes(byte[] in) throws EncodingException {
        EntitledKey message = new EntitledKey();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // Type
    public static final int SINGLE = 0;
    public static final int ENTITLEMENT = 1;
    public static final int ENTITLED_KEY = 2;
    // Algorithm
    public static final int UNENCRYPTED = 0;
    public static final int AESCTR = 1;
  
    protected Vector keyIds = new Vector(); // 2
    protected byte[] contentId; // 4
    protected boolean _hasContentId;
    protected int cryptoPeriodIndex; // 7
    protected boolean _hasCryptoPeriodIndex;
    protected int protectionScheme; // 9
    protected boolean _hasProtectionScheme;
    protected int cryptoPeriodSeconds; // 10
    protected boolean _hasCryptoPeriodSeconds;
    protected int type; // 11
    protected boolean _hasType;
    protected int keySequence; // 12
    protected boolean _hasKeySequence;
    protected Vector groupIds = new Vector(); // 13
    protected Vector entitledKeys = new Vector(); // 14
    protected String videoFeature; // 15
    protected boolean _hasVideoFeature;
    protected String audioFeature; // 16
    protected boolean _hasAudioFeature;
    protected int entitlementPeriodIndex; // 17
    protected boolean _hasEntitlementPeriodIndex;
    protected int algorithm; // 1
    protected boolean _hasAlgorithm;
    protected String provider; // 3
    protected boolean _hasProvider;
    protected String trackType; // 5
    protected boolean _hasTrackType;
    protected String policy; // 6
    protected boolean _hasPolicy;
    protected byte[] groupedLicense; // 8
    protected boolean _hasGroupedLicense;
    
    public void addKeyIds(byte[] value) {
        this.keyIds.addElement(value);
    }

    public int getKeyIdsCount() {
        return this.keyIds.size();
    }

    public byte[] getKeyIds(int index) {
        return (byte[])this.keyIds.elementAt(index);
    }

    public Vector getKeyIdsVector() {
        return this.keyIds;
    }

    public void setKeyIdsVector(Vector value) {
        this.keyIds = value;
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
    public int getCryptoPeriodIndex() {
        return cryptoPeriodIndex;
    }
    
    public void setCryptoPeriodIndex(int cryptoPeriodIndex) {
        this.cryptoPeriodIndex = cryptoPeriodIndex;
        this._hasCryptoPeriodIndex = true;
    }
    
    public void clearCryptoPeriodIndex() {
        _hasCryptoPeriodIndex = false;
    }
    
    public boolean hasCryptoPeriodIndex() {
        return _hasCryptoPeriodIndex;
    }
    public int getProtectionScheme() {
        return protectionScheme;
    }
    
    public void setProtectionScheme(int protectionScheme) {
        this.protectionScheme = protectionScheme;
        this._hasProtectionScheme = true;
    }
    
    public void clearProtectionScheme() {
        _hasProtectionScheme = false;
    }
    
    public boolean hasProtectionScheme() {
        return _hasProtectionScheme;
    }
    public int getCryptoPeriodSeconds() {
        return cryptoPeriodSeconds;
    }
    
    public void setCryptoPeriodSeconds(int cryptoPeriodSeconds) {
        this.cryptoPeriodSeconds = cryptoPeriodSeconds;
        this._hasCryptoPeriodSeconds = true;
    }
    
    public void clearCryptoPeriodSeconds() {
        _hasCryptoPeriodSeconds = false;
    }
    
    public boolean hasCryptoPeriodSeconds() {
        return _hasCryptoPeriodSeconds;
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
    public int getKeySequence() {
        return keySequence;
    }
    
    public void setKeySequence(int keySequence) {
        this.keySequence = keySequence;
        this._hasKeySequence = true;
    }
    
    public void clearKeySequence() {
        _hasKeySequence = false;
    }
    
    public boolean hasKeySequence() {
        return _hasKeySequence;
    }
    public void addGroupIds(byte[] value) {
        this.groupIds.addElement(value);
    }

    public int getGroupIdsCount() {
        return this.groupIds.size();
    }

    public byte[] getGroupIds(int index) {
        return (byte[])this.groupIds.elementAt(index);
    }

    public Vector getGroupIdsVector() {
        return this.groupIds;
    }

    public void setGroupIdsVector(Vector value) {
        this.groupIds = value;
    }
    public void addEntitledKeys(WidevinePsshData.EntitledKey value) {
        this.entitledKeys.addElement(value);
    }

    public int getEntitledKeysCount() {
        return this.entitledKeys.size();
    }

    public WidevinePsshData.EntitledKey getEntitledKeys(int index) {
        return (WidevinePsshData.EntitledKey)this.entitledKeys.elementAt(index);
    }

    public Vector getEntitledKeysVector() {
        return this.entitledKeys;
    }

    public void setEntitledKeysVector(Vector value) {
        this.entitledKeys = value;
    }
    public String getVideoFeature() {
        return videoFeature;
    }
    
    public void setVideoFeature(String videoFeature) {
        this.videoFeature = videoFeature;
        this._hasVideoFeature = true;
    }
    
    public void clearVideoFeature() {
        _hasVideoFeature = false;
    }
    
    public boolean hasVideoFeature() {
        return _hasVideoFeature;
    }
    public String getAudioFeature() {
        return audioFeature;
    }
    
    public void setAudioFeature(String audioFeature) {
        this.audioFeature = audioFeature;
        this._hasAudioFeature = true;
    }
    
    public void clearAudioFeature() {
        _hasAudioFeature = false;
    }
    
    public boolean hasAudioFeature() {
        return _hasAudioFeature;
    }
    public int getEntitlementPeriodIndex() {
        return entitlementPeriodIndex;
    }
    
    public void setEntitlementPeriodIndex(int entitlementPeriodIndex) {
        this.entitlementPeriodIndex = entitlementPeriodIndex;
        this._hasEntitlementPeriodIndex = true;
    }
    
    public void clearEntitlementPeriodIndex() {
        _hasEntitlementPeriodIndex = false;
    }
    
    public boolean hasEntitlementPeriodIndex() {
        return _hasEntitlementPeriodIndex;
    }
    public int getAlgorithm() {
        return algorithm;
    }
    
    public void setAlgorithm(int algorithm) {
        this.algorithm = algorithm;
        this._hasAlgorithm = true;
    }
    
    public void clearAlgorithm() {
        _hasAlgorithm = false;
    }
    
    public boolean hasAlgorithm() {
        return _hasAlgorithm;
    }
    public String getProvider() {
        return provider;
    }
    
    public void setProvider(String provider) {
        this.provider = provider;
        this._hasProvider = true;
    }
    
    public void clearProvider() {
        _hasProvider = false;
    }
    
    public boolean hasProvider() {
        return _hasProvider;
    }
    public String getTrackType() {
        return trackType;
    }
    
    public void setTrackType(String trackType) {
        this.trackType = trackType;
        this._hasTrackType = true;
    }
    
    public void clearTrackType() {
        _hasTrackType = false;
    }
    
    public boolean hasTrackType() {
        return _hasTrackType;
    }
    public String getPolicy() {
        return policy;
    }
    
    public void setPolicy(String policy) {
        this.policy = policy;
        this._hasPolicy = true;
    }
    
    public void clearPolicy() {
        _hasPolicy = false;
    }
    
    public boolean hasPolicy() {
        return _hasPolicy;
    }
    public byte[] getGroupedLicense() {
        return groupedLicense;
    }
    
    public void setGroupedLicense(byte[] groupedLicense) {
        this.groupedLicense = groupedLicense;
        this._hasGroupedLicense = true;
    }
    
    public void clearGroupedLicense() {
        _hasGroupedLicense = false;
    }
    
    public boolean hasGroupedLicense() {
        return _hasGroupedLicense;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        for(int i = 0; i < getKeyIdsCount(); i++) {
            out.writeBytes(2, getKeyIds(i));
        }
        
        if(_hasContentId)
            out.writeBytes(4, contentId);
        
        if(_hasCryptoPeriodIndex)
            out.writeUInt32(7, cryptoPeriodIndex);
        
        if(_hasProtectionScheme)
            out.writeUInt32(9, protectionScheme);
        
        if(_hasCryptoPeriodSeconds)
            out.writeUInt32(10, cryptoPeriodSeconds);
        
        if(_hasType)
            out.writeInt32(11, type);
        
        if(_hasKeySequence)
            out.writeUInt32(12, keySequence);
        
        for(int i = 0; i < getGroupIdsCount(); i++) {
            out.writeBytes(13, getGroupIds(i));
        }
        
        for(int i = 0; i < getEntitledKeysCount(); i++) {
            out.writeMessage(14, getEntitledKeys(i));
        }
        
        if(_hasVideoFeature)
            out.writeString(15, videoFeature);
        
        if(_hasAudioFeature)
            out.writeString(16, audioFeature);
        
        if(_hasEntitlementPeriodIndex)
            out.writeUInt32(17, entitlementPeriodIndex);
        
        if(_hasAlgorithm)
            out.writeInt32(1, algorithm);
        
        if(_hasProvider)
            out.writeString(3, provider);
        
        if(_hasTrackType)
            out.writeString(5, trackType);
        
        if(_hasPolicy)
            out.writeString(6, policy);
        
        if(_hasGroupedLicense)
            out.writeBytes(8, groupedLicense);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 18: {
                    addKeyIds(in.readBytes());
                    break; }
                case 34: {
                    contentId = in.readBytes();
                    _hasContentId = true;
                    break; }
                case 56: {
                    cryptoPeriodIndex = in.readUInt32();
                    _hasCryptoPeriodIndex = true;
                    break; }
                case 72: {
                    protectionScheme = in.readUInt32();
                    _hasProtectionScheme = true;
                    break; }
                case 80: {
                    cryptoPeriodSeconds = in.readUInt32();
                    _hasCryptoPeriodSeconds = true;
                    break; }
                case 88: {
                    type = in.readInt32();
                    _hasType = true;
                    break; }
                case 96: {
                    keySequence = in.readUInt32();
                    _hasKeySequence = true;
                    break; }
                case 106: {
                    addGroupIds(in.readBytes());
                    break; }
                case 114: {
                    WidevinePsshData.EntitledKey message = new WidevinePsshData.EntitledKey();
                    in.readMessage(message);
                    addEntitledKeys(message);
                    break; }
                case 122: {
                    videoFeature = in.readString();
                    _hasVideoFeature = true;
                    break; }
                case 130: {
                    audioFeature = in.readString();
                    _hasAudioFeature = true;
                    break; }
                case 136: {
                    entitlementPeriodIndex = in.readUInt32();
                    _hasEntitlementPeriodIndex = true;
                    break; }
                case 8: {
                    algorithm = in.readInt32();
                    _hasAlgorithm = true;
                    break; }
                case 26: {
                    provider = in.readString();
                    _hasProvider = true;
                    break; }
                case 42: {
                    trackType = in.readString();
                    _hasTrackType = true;
                    break; }
                case 50: {
                    policy = in.readString();
                    _hasPolicy = true;
                    break; }
                case 66: {
                    groupedLicense = in.readBytes();
                    _hasGroupedLicense = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static WidevinePsshData fromBytes(byte[] in) throws EncodingException {
        WidevinePsshData message = new WidevinePsshData();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



