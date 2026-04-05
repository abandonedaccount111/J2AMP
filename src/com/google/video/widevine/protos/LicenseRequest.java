package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class LicenseRequest implements Message {
    
    public static class ContentIdentification implements Message {
      
    public static class WidevinePsshData implements Message {
      
  
  
    protected Vector psshData = new Vector(); // 1
    protected int licenseType; // 2
    protected boolean _hasLicenseType;
    protected byte[] requestId; // 3
    protected boolean _hasRequestId;
    
    public void addPsshData(byte[] value) {
        this.psshData.addElement(value);
    }

    public int getPsshDataCount() {
        return this.psshData.size();
    }

    public byte[] getPsshData(int index) {
        return (byte[])this.psshData.elementAt(index);
    }

    public Vector getPsshDataVector() {
        return this.psshData;
    }

    public void setPsshDataVector(Vector value) {
        this.psshData = value;
    }
    public int getLicenseType() {
        return licenseType;
    }
    
    public void setLicenseType(int licenseType) {
        this.licenseType = licenseType;
        this._hasLicenseType = true;
    }
    
    public void clearLicenseType() {
        _hasLicenseType = false;
    }
    
    public boolean hasLicenseType() {
        return _hasLicenseType;
    }
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        for(int i = 0; i < getPsshDataCount(); i++) {
            out.writeBytes(1, getPsshData(i));
        }
        
        if(_hasLicenseType)
            out.writeInt32(2, licenseType);
        
        if(_hasRequestId)
            out.writeBytes(3, requestId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    addPsshData(in.readBytes());
                    break; }
                case 16: {
                    licenseType = in.readInt32();
                    _hasLicenseType = true;
                    break; }
                case 26: {
                    requestId = in.readBytes();
                    _hasRequestId = true;
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
    public static class WebmKeyId implements Message {
      
  
  
    protected byte[] header; // 1
    protected boolean _hasHeader;
    protected int licenseType; // 2
    protected boolean _hasLicenseType;
    protected byte[] requestId; // 3
    protected boolean _hasRequestId;
    
    public byte[] getHeader() {
        return header;
    }
    
    public void setHeader(byte[] header) {
        this.header = header;
        this._hasHeader = true;
    }
    
    public void clearHeader() {
        _hasHeader = false;
    }
    
    public boolean hasHeader() {
        return _hasHeader;
    }
    public int getLicenseType() {
        return licenseType;
    }
    
    public void setLicenseType(int licenseType) {
        this.licenseType = licenseType;
        this._hasLicenseType = true;
    }
    
    public void clearLicenseType() {
        _hasLicenseType = false;
    }
    
    public boolean hasLicenseType() {
        return _hasLicenseType;
    }
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasHeader)
            out.writeBytes(1, header);
        
        if(_hasLicenseType)
            out.writeInt32(2, licenseType);
        
        if(_hasRequestId)
            out.writeBytes(3, requestId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    header = in.readBytes();
                    _hasHeader = true;
                    break; }
                case 16: {
                    licenseType = in.readInt32();
                    _hasLicenseType = true;
                    break; }
                case 26: {
                    requestId = in.readBytes();
                    _hasRequestId = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static WebmKeyId fromBytes(byte[] in) throws EncodingException {
        WebmKeyId message = new WebmKeyId();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class ExistingLicense implements Message {
      
  
  
    protected LicenseIdentification licenseId; // 1
    protected long secondsSinceStarted; // 2
    protected boolean _hasSecondsSinceStarted;
    protected long secondsSinceLastPlayed; // 3
    protected boolean _hasSecondsSinceLastPlayed;
    protected byte[] sessionUsageTableEntry; // 4
    protected boolean _hasSessionUsageTableEntry;
    
    public LicenseIdentification getLicenseId() {
        return licenseId;
    }
    
    public void setLicenseId(LicenseIdentification licenseId) {
        this.licenseId = licenseId;
    }
    
    public void clearLicenseId() {
        licenseId = null;
    }
    
    public boolean hasLicenseId() {
        return licenseId != null;
    }
    public long getSecondsSinceStarted() {
        return secondsSinceStarted;
    }
    
    public void setSecondsSinceStarted(long secondsSinceStarted) {
        this.secondsSinceStarted = secondsSinceStarted;
        this._hasSecondsSinceStarted = true;
    }
    
    public void clearSecondsSinceStarted() {
        _hasSecondsSinceStarted = false;
    }
    
    public boolean hasSecondsSinceStarted() {
        return _hasSecondsSinceStarted;
    }
    public long getSecondsSinceLastPlayed() {
        return secondsSinceLastPlayed;
    }
    
    public void setSecondsSinceLastPlayed(long secondsSinceLastPlayed) {
        this.secondsSinceLastPlayed = secondsSinceLastPlayed;
        this._hasSecondsSinceLastPlayed = true;
    }
    
    public void clearSecondsSinceLastPlayed() {
        _hasSecondsSinceLastPlayed = false;
    }
    
    public boolean hasSecondsSinceLastPlayed() {
        return _hasSecondsSinceLastPlayed;
    }
    public byte[] getSessionUsageTableEntry() {
        return sessionUsageTableEntry;
    }
    
    public void setSessionUsageTableEntry(byte[] sessionUsageTableEntry) {
        this.sessionUsageTableEntry = sessionUsageTableEntry;
        this._hasSessionUsageTableEntry = true;
    }
    
    public void clearSessionUsageTableEntry() {
        _hasSessionUsageTableEntry = false;
    }
    
    public boolean hasSessionUsageTableEntry() {
        return _hasSessionUsageTableEntry;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        out.writeMessage(1, licenseId);
        
        if(_hasSecondsSinceStarted)
            out.writeInt64(2, secondsSinceStarted);
        
        if(_hasSecondsSinceLastPlayed)
            out.writeInt64(3, secondsSinceLastPlayed);
        
        if(_hasSessionUsageTableEntry)
            out.writeBytes(4, sessionUsageTableEntry);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    licenseId = new LicenseIdentification();
                    in.readMessage(licenseId);
                    break; }
                case 16: {
                    secondsSinceStarted = in.readInt64();
                    _hasSecondsSinceStarted = true;
                    break; }
                case 24: {
                    secondsSinceLastPlayed = in.readInt64();
                    _hasSecondsSinceLastPlayed = true;
                    break; }
                case 34: {
                    sessionUsageTableEntry = in.readBytes();
                    _hasSessionUsageTableEntry = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ExistingLicense fromBytes(byte[] in) throws EncodingException {
        ExistingLicense message = new ExistingLicense();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class InitData implements Message {
      
  
    // InitDataType
    public static final int CENC = 1;
    public static final int WEBM = 2;
  
    protected int initDataType; // 1
    protected boolean _hasInitDataType;
    protected byte[] initData; // 2
    protected boolean _hasInitData;
    protected int licenseType; // 3
    protected boolean _hasLicenseType;
    protected byte[] requestId; // 4
    protected boolean _hasRequestId;
    
    public int getInitDataType() {
        return initDataType;
    }
    
    public void setInitDataType(int initDataType) {
        this.initDataType = initDataType;
        this._hasInitDataType = true;
    }
    
    public void clearInitDataType() {
        _hasInitDataType = false;
    }
    
    public boolean hasInitDataType() {
        return _hasInitDataType;
    }
    public byte[] getInitData() {
        return initData;
    }
    
    public void setInitData(byte[] initData) {
        this.initData = initData;
        this._hasInitData = true;
    }
    
    public void clearInitData() {
        _hasInitData = false;
    }
    
    public boolean hasInitData() {
        return _hasInitData;
    }
    public int getLicenseType() {
        return licenseType;
    }
    
    public void setLicenseType(int licenseType) {
        this.licenseType = licenseType;
        this._hasLicenseType = true;
    }
    
    public void clearLicenseType() {
        _hasLicenseType = false;
    }
    
    public boolean hasLicenseType() {
        return _hasLicenseType;
    }
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasInitDataType)
            out.writeInt32(1, initDataType);
        
        if(_hasInitData)
            out.writeBytes(2, initData);
        
        if(_hasLicenseType)
            out.writeInt32(3, licenseType);
        
        if(_hasRequestId)
            out.writeBytes(4, requestId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    initDataType = in.readInt32();
                    _hasInitDataType = true;
                    break; }
                case 18: {
                    initData = in.readBytes();
                    _hasInitData = true;
                    break; }
                case 24: {
                    licenseType = in.readInt32();
                    _hasLicenseType = true;
                    break; }
                case 34: {
                    requestId = in.readBytes();
                    _hasRequestId = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static InitData fromBytes(byte[] in) throws EncodingException {
        InitData message = new InitData();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
  
    protected LicenseRequest.ContentIdentification.WidevinePsshData widevinePsshData; // 1
    protected LicenseRequest.ContentIdentification.WebmKeyId webmKeyId; // 2
    protected LicenseRequest.ContentIdentification.ExistingLicense existingLicense; // 3
    protected LicenseRequest.ContentIdentification.InitData initData; // 4
    
    public LicenseRequest.ContentIdentification.WidevinePsshData getWidevinePsshData() {
        return widevinePsshData;
    }
    
    public void setWidevinePsshData(LicenseRequest.ContentIdentification.WidevinePsshData widevinePsshData) {
        this.widevinePsshData = widevinePsshData;
    }
    
    public void clearWidevinePsshData() {
        widevinePsshData = null;
    }
    
    public boolean hasWidevinePsshData() {
        return widevinePsshData != null;
    }
    public LicenseRequest.ContentIdentification.WebmKeyId getWebmKeyId() {
        return webmKeyId;
    }
    
    public void setWebmKeyId(LicenseRequest.ContentIdentification.WebmKeyId webmKeyId) {
        this.webmKeyId = webmKeyId;
    }
    
    public void clearWebmKeyId() {
        webmKeyId = null;
    }
    
    public boolean hasWebmKeyId() {
        return webmKeyId != null;
    }
    public LicenseRequest.ContentIdentification.ExistingLicense getExistingLicense() {
        return existingLicense;
    }
    
    public void setExistingLicense(LicenseRequest.ContentIdentification.ExistingLicense existingLicense) {
        this.existingLicense = existingLicense;
    }
    
    public void clearExistingLicense() {
        existingLicense = null;
    }
    
    public boolean hasExistingLicense() {
        return existingLicense != null;
    }
    public LicenseRequest.ContentIdentification.InitData getInitData() {
        return initData;
    }
    
    public void setInitData(LicenseRequest.ContentIdentification.InitData initData) {
        this.initData = initData;
    }
    
    public void clearInitData() {
        initData = null;
    }
    
    public boolean hasInitData() {
        return initData != null;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        out.writeMessage(1, widevinePsshData);
        
        out.writeMessage(2, webmKeyId);
        
        out.writeMessage(3, existingLicense);
        
        out.writeMessage(4, initData);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    widevinePsshData = new LicenseRequest.ContentIdentification.WidevinePsshData();
                    in.readMessage(widevinePsshData);
                    break; }
                case 18: {
                    webmKeyId = new LicenseRequest.ContentIdentification.WebmKeyId();
                    in.readMessage(webmKeyId);
                    break; }
                case 26: {
                    existingLicense = new LicenseRequest.ContentIdentification.ExistingLicense();
                    in.readMessage(existingLicense);
                    break; }
                case 34: {
                    initData = new LicenseRequest.ContentIdentification.InitData();
                    in.readMessage(initData);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ContentIdentification fromBytes(byte[] in) throws EncodingException {
        ContentIdentification message = new ContentIdentification();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // RequestType
    public static final int NEW = 1;
    public static final int RENEWAL = 2;
    public static final int RELEASE = 3;
  
    protected ClientIdentification clientId; // 1
    protected LicenseRequest.ContentIdentification contentId; // 2
    protected int type; // 3
    protected boolean _hasType;
    protected long requestTime; // 4
    protected boolean _hasRequestTime;
    protected byte[] keyControlNonceDeprecated; // 5
    protected boolean _hasKeyControlNonceDeprecated;
    protected int protocolVersion; // 6
    protected boolean _hasProtocolVersion;
    protected int keyControlNonce; // 7
    protected boolean _hasKeyControlNonce;
    protected EncryptedClientIdentification encryptedClientId; // 8
    protected String clientVersion; // 9
    protected boolean _hasClientVersion;
    
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
    public LicenseRequest.ContentIdentification getContentId() {
        return contentId;
    }
    
    public void setContentId(LicenseRequest.ContentIdentification contentId) {
        this.contentId = contentId;
    }
    
    public void clearContentId() {
        contentId = null;
    }
    
    public boolean hasContentId() {
        return contentId != null;
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
    public long getRequestTime() {
        return requestTime;
    }
    
    public void setRequestTime(long requestTime) {
        this.requestTime = requestTime;
        this._hasRequestTime = true;
    }
    
    public void clearRequestTime() {
        _hasRequestTime = false;
    }
    
    public boolean hasRequestTime() {
        return _hasRequestTime;
    }
    public byte[] getKeyControlNonceDeprecated() {
        return keyControlNonceDeprecated;
    }
    
    public void setKeyControlNonceDeprecated(byte[] keyControlNonceDeprecated) {
        this.keyControlNonceDeprecated = keyControlNonceDeprecated;
        this._hasKeyControlNonceDeprecated = true;
    }
    
    public void clearKeyControlNonceDeprecated() {
        _hasKeyControlNonceDeprecated = false;
    }
    
    public boolean hasKeyControlNonceDeprecated() {
        return _hasKeyControlNonceDeprecated;
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
    public int getKeyControlNonce() {
        return keyControlNonce;
    }
    
    public void setKeyControlNonce(int keyControlNonce) {
        this.keyControlNonce = keyControlNonce;
        this._hasKeyControlNonce = true;
    }
    
    public void clearKeyControlNonce() {
        _hasKeyControlNonce = false;
    }
    
    public boolean hasKeyControlNonce() {
        return _hasKeyControlNonce;
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
    public String getClientVersion() {
        return clientVersion;
    }
    
    public void setClientVersion(String clientVersion) {
        this.clientVersion = clientVersion;
        this._hasClientVersion = true;
    }
    
    public void clearClientVersion() {
        _hasClientVersion = false;
    }
    
    public boolean hasClientVersion() {
        return _hasClientVersion;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        out.writeMessage(1, clientId);
        
        out.writeMessage(2, contentId);
        
        if(_hasType)
            out.writeInt32(3, type);
        
        if(_hasRequestTime)
            out.writeInt64(4, requestTime);
        
        if(_hasKeyControlNonceDeprecated)
            out.writeBytes(5, keyControlNonceDeprecated);
        
        if(_hasProtocolVersion)
            out.writeInt32(6, protocolVersion);
        
        if(_hasKeyControlNonce)
            out.writeUInt32(7, keyControlNonce);
        
        out.writeMessage(8, encryptedClientId);
        
        if(_hasClientVersion)
            out.writeString(9, clientVersion);
        
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
                case 18: {
                    contentId = new LicenseRequest.ContentIdentification();
                    in.readMessage(contentId);
                    break; }
                case 24: {
                    type = in.readInt32();
                    _hasType = true;
                    break; }
                case 32: {
                    requestTime = in.readInt64();
                    _hasRequestTime = true;
                    break; }
                case 42: {
                    keyControlNonceDeprecated = in.readBytes();
                    _hasKeyControlNonceDeprecated = true;
                    break; }
                case 48: {
                    protocolVersion = in.readInt32();
                    _hasProtocolVersion = true;
                    break; }
                case 56: {
                    keyControlNonce = in.readUInt32();
                    _hasKeyControlNonce = true;
                    break; }
                case 66: {
                    encryptedClientId = new EncryptedClientIdentification();
                    in.readMessage(encryptedClientId);
                    break; }
                case 74: {
                    clientVersion = in.readString();
                    _hasClientVersion = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static LicenseRequest fromBytes(byte[] in) throws EncodingException {
        LicenseRequest message = new LicenseRequest();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



