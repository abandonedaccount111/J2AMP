package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ClientIdentification implements Message {
    
    public static class NameValue implements Message {
      
  
  
    protected String name; // 1
    protected boolean _hasName;
    protected String value; // 2
    protected boolean _hasValue;
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
        this._hasName = true;
    }
    
    public void clearName() {
        _hasName = false;
    }
    
    public boolean hasName() {
        return _hasName;
    }
    public String getValue() {
        return value;
    }
    
    public void setValue(String value) {
        this.value = value;
        this._hasValue = true;
    }
    
    public void clearValue() {
        _hasValue = false;
    }
    
    public boolean hasValue() {
        return _hasValue;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasName)
            out.writeString(1, name);
        
        if(_hasValue)
            out.writeString(2, value);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    name = in.readString();
                    _hasName = true;
                    break; }
                case 18: {
                    value = in.readString();
                    _hasValue = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static NameValue fromBytes(byte[] in) throws EncodingException {
        NameValue message = new NameValue();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class ClientCapabilities implements Message {
      
  
    // HdcpVersion
    public static final int HDCP_NONE = 0;
    public static final int HDCP_V1 = 1;
    public static final int HDCP_V2 = 2;
    public static final int HDCP_V2_1 = 3;
    public static final int HDCP_V2_2 = 4;
    public static final int HDCP_V2_3 = 5;
    public static final int HDCP_V1_0 = 6;
    public static final int HDCP_V1_1 = 7;
    public static final int HDCP_V1_2 = 8;
    public static final int HDCP_V1_3 = 9;
    public static final int HDCP_V1_4 = 10;
    public static final int HDCP_NO_DIGITAL_OUTPUT = 255;
    // CertificateKeyType
    public static final int RSA_2048 = 0;
    public static final int RSA_3072 = 1;
    public static final int ECC_SECP256R1 = 2;
    public static final int ECC_SECP384R1 = 3;
    public static final int ECC_SECP521R1 = 4;
    // AnalogOutputCapabilities
    public static final int ANALOG_OUTPUT_UNKNOWN = 0;
    public static final int ANALOG_OUTPUT_NONE = 1;
    public static final int ANALOG_OUTPUT_SUPPORTED = 2;
    public static final int ANALOG_OUTPUT_SUPPORTS_CGMS_A = 3;
    // WatermarkingSupport
    public static final int WATERMARKING_SUPPORT_UNKNOWN = 0;
    public static final int WATERMARKING_NOT_SUPPORTED = 1;
    public static final int WATERMARKING_CONFIGURABLE = 2;
    public static final int WATERMARKING_ALWAYS_ON = 3;
  
    protected boolean clientToken; // 1
    protected boolean _hasClientToken;
    protected boolean sessionToken; // 2
    protected boolean _hasSessionToken;
    protected boolean videoResolutionConstraints; // 3
    protected boolean _hasVideoResolutionConstraints;
    protected int maxHdcpVersion; // 4
    protected boolean _hasMaxHdcpVersion;
    protected int oemCryptoApiVersion; // 5
    protected boolean _hasOemCryptoApiVersion;
    protected boolean antiRollbackUsageTable; // 6
    protected boolean _hasAntiRollbackUsageTable;
    protected int srmVersion; // 7
    protected boolean _hasSrmVersion;
    protected boolean canUpdateSrm; // 8
    protected boolean _hasCanUpdateSrm;
    protected Vector supportedCertificateKeyType = new Vector(); // 9
    protected int analogOutputCapabilities; // 10
    protected boolean _hasAnalogOutputCapabilities;
    protected boolean canDisableAnalogOutput; // 11
    protected boolean _hasCanDisableAnalogOutput;
    protected int resourceRatingTier; // 12
    protected boolean _hasResourceRatingTier;
    protected int watermarkingSupport; // 13
    protected boolean _hasWatermarkingSupport;
    protected boolean initialRenewalDelayBase; // 14
    protected boolean _hasInitialRenewalDelayBase;
    
    public boolean getClientToken() {
        return clientToken;
    }
    
    public void setClientToken(boolean clientToken) {
        this.clientToken = clientToken;
        this._hasClientToken = true;
    }
    
    public void clearClientToken() {
        _hasClientToken = false;
    }
    
    public boolean hasClientToken() {
        return _hasClientToken;
    }
    public boolean getSessionToken() {
        return sessionToken;
    }
    
    public void setSessionToken(boolean sessionToken) {
        this.sessionToken = sessionToken;
        this._hasSessionToken = true;
    }
    
    public void clearSessionToken() {
        _hasSessionToken = false;
    }
    
    public boolean hasSessionToken() {
        return _hasSessionToken;
    }
    public boolean getVideoResolutionConstraints() {
        return videoResolutionConstraints;
    }
    
    public void setVideoResolutionConstraints(boolean videoResolutionConstraints) {
        this.videoResolutionConstraints = videoResolutionConstraints;
        this._hasVideoResolutionConstraints = true;
    }
    
    public void clearVideoResolutionConstraints() {
        _hasVideoResolutionConstraints = false;
    }
    
    public boolean hasVideoResolutionConstraints() {
        return _hasVideoResolutionConstraints;
    }
    public int getMaxHdcpVersion() {
        return maxHdcpVersion;
    }
    
    public void setMaxHdcpVersion(int maxHdcpVersion) {
        this.maxHdcpVersion = maxHdcpVersion;
        this._hasMaxHdcpVersion = true;
    }
    
    public void clearMaxHdcpVersion() {
        _hasMaxHdcpVersion = false;
    }
    
    public boolean hasMaxHdcpVersion() {
        return _hasMaxHdcpVersion;
    }
    public int getOemCryptoApiVersion() {
        return oemCryptoApiVersion;
    }
    
    public void setOemCryptoApiVersion(int oemCryptoApiVersion) {
        this.oemCryptoApiVersion = oemCryptoApiVersion;
        this._hasOemCryptoApiVersion = true;
    }
    
    public void clearOemCryptoApiVersion() {
        _hasOemCryptoApiVersion = false;
    }
    
    public boolean hasOemCryptoApiVersion() {
        return _hasOemCryptoApiVersion;
    }
    public boolean getAntiRollbackUsageTable() {
        return antiRollbackUsageTable;
    }
    
    public void setAntiRollbackUsageTable(boolean antiRollbackUsageTable) {
        this.antiRollbackUsageTable = antiRollbackUsageTable;
        this._hasAntiRollbackUsageTable = true;
    }
    
    public void clearAntiRollbackUsageTable() {
        _hasAntiRollbackUsageTable = false;
    }
    
    public boolean hasAntiRollbackUsageTable() {
        return _hasAntiRollbackUsageTable;
    }
    public int getSrmVersion() {
        return srmVersion;
    }
    
    public void setSrmVersion(int srmVersion) {
        this.srmVersion = srmVersion;
        this._hasSrmVersion = true;
    }
    
    public void clearSrmVersion() {
        _hasSrmVersion = false;
    }
    
    public boolean hasSrmVersion() {
        return _hasSrmVersion;
    }
    public boolean getCanUpdateSrm() {
        return canUpdateSrm;
    }
    
    public void setCanUpdateSrm(boolean canUpdateSrm) {
        this.canUpdateSrm = canUpdateSrm;
        this._hasCanUpdateSrm = true;
    }
    
    public void clearCanUpdateSrm() {
        _hasCanUpdateSrm = false;
    }
    
    public boolean hasCanUpdateSrm() {
        return _hasCanUpdateSrm;
    }
    public void addSupportedCertificateKeyType(int value) {
        this.supportedCertificateKeyType.addElement(new Integer(value));
    }

    public int getSupportedCertificateKeyTypeCount() {
        return this.supportedCertificateKeyType.size();
    }

    public int getSupportedCertificateKeyType(int index) {
        return ((Integer)this.supportedCertificateKeyType.elementAt(index)).intValue();
    }

    public Vector getSupportedCertificateKeyTypeVector() {
        return this.supportedCertificateKeyType;
    }

    public void setSupportedCertificateKeyTypeVector(Vector value) {
        this.supportedCertificateKeyType = value;
    }
    public int getAnalogOutputCapabilities() {
        return analogOutputCapabilities;
    }
    
    public void setAnalogOutputCapabilities(int analogOutputCapabilities) {
        this.analogOutputCapabilities = analogOutputCapabilities;
        this._hasAnalogOutputCapabilities = true;
    }
    
    public void clearAnalogOutputCapabilities() {
        _hasAnalogOutputCapabilities = false;
    }
    
    public boolean hasAnalogOutputCapabilities() {
        return _hasAnalogOutputCapabilities;
    }
    public boolean getCanDisableAnalogOutput() {
        return canDisableAnalogOutput;
    }
    
    public void setCanDisableAnalogOutput(boolean canDisableAnalogOutput) {
        this.canDisableAnalogOutput = canDisableAnalogOutput;
        this._hasCanDisableAnalogOutput = true;
    }
    
    public void clearCanDisableAnalogOutput() {
        _hasCanDisableAnalogOutput = false;
    }
    
    public boolean hasCanDisableAnalogOutput() {
        return _hasCanDisableAnalogOutput;
    }
    public int getResourceRatingTier() {
        return resourceRatingTier;
    }
    
    public void setResourceRatingTier(int resourceRatingTier) {
        this.resourceRatingTier = resourceRatingTier;
        this._hasResourceRatingTier = true;
    }
    
    public void clearResourceRatingTier() {
        _hasResourceRatingTier = false;
    }
    
    public boolean hasResourceRatingTier() {
        return _hasResourceRatingTier;
    }
    public int getWatermarkingSupport() {
        return watermarkingSupport;
    }
    
    public void setWatermarkingSupport(int watermarkingSupport) {
        this.watermarkingSupport = watermarkingSupport;
        this._hasWatermarkingSupport = true;
    }
    
    public void clearWatermarkingSupport() {
        _hasWatermarkingSupport = false;
    }
    
    public boolean hasWatermarkingSupport() {
        return _hasWatermarkingSupport;
    }
    public boolean getInitialRenewalDelayBase() {
        return initialRenewalDelayBase;
    }
    
    public void setInitialRenewalDelayBase(boolean initialRenewalDelayBase) {
        this.initialRenewalDelayBase = initialRenewalDelayBase;
        this._hasInitialRenewalDelayBase = true;
    }
    
    public void clearInitialRenewalDelayBase() {
        _hasInitialRenewalDelayBase = false;
    }
    
    public boolean hasInitialRenewalDelayBase() {
        return _hasInitialRenewalDelayBase;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasClientToken)
            out.writeBool(1, clientToken);
        
        if(_hasSessionToken)
            out.writeBool(2, sessionToken);
        
        if(_hasVideoResolutionConstraints)
            out.writeBool(3, videoResolutionConstraints);
        
        if(_hasMaxHdcpVersion)
            out.writeInt32(4, maxHdcpVersion);
        
        if(_hasOemCryptoApiVersion)
            out.writeUInt32(5, oemCryptoApiVersion);
        
        if(_hasAntiRollbackUsageTable)
            out.writeBool(6, antiRollbackUsageTable);
        
        if(_hasSrmVersion)
            out.writeUInt32(7, srmVersion);
        
        if(_hasCanUpdateSrm)
            out.writeBool(8, canUpdateSrm);
        
        for(int i = 0; i < getSupportedCertificateKeyTypeCount(); i++) {
            out.writeInt32(9, getSupportedCertificateKeyType(i));
        }
        
        if(_hasAnalogOutputCapabilities)
            out.writeInt32(10, analogOutputCapabilities);
        
        if(_hasCanDisableAnalogOutput)
            out.writeBool(11, canDisableAnalogOutput);
        
        if(_hasResourceRatingTier)
            out.writeUInt32(12, resourceRatingTier);
        
        if(_hasWatermarkingSupport)
            out.writeInt32(13, watermarkingSupport);
        
        if(_hasInitialRenewalDelayBase)
            out.writeBool(14, initialRenewalDelayBase);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    clientToken = in.readBool();
                    _hasClientToken = true;
                    break; }
                case 16: {
                    sessionToken = in.readBool();
                    _hasSessionToken = true;
                    break; }
                case 24: {
                    videoResolutionConstraints = in.readBool();
                    _hasVideoResolutionConstraints = true;
                    break; }
                case 32: {
                    maxHdcpVersion = in.readInt32();
                    _hasMaxHdcpVersion = true;
                    break; }
                case 40: {
                    oemCryptoApiVersion = in.readUInt32();
                    _hasOemCryptoApiVersion = true;
                    break; }
                case 48: {
                    antiRollbackUsageTable = in.readBool();
                    _hasAntiRollbackUsageTable = true;
                    break; }
                case 56: {
                    srmVersion = in.readUInt32();
                    _hasSrmVersion = true;
                    break; }
                case 64: {
                    canUpdateSrm = in.readBool();
                    _hasCanUpdateSrm = true;
                    break; }
                case 72: {
                    addSupportedCertificateKeyType(in.readInt32());
                    break; }
                case 80: {
                    analogOutputCapabilities = in.readInt32();
                    _hasAnalogOutputCapabilities = true;
                    break; }
                case 88: {
                    canDisableAnalogOutput = in.readBool();
                    _hasCanDisableAnalogOutput = true;
                    break; }
                case 96: {
                    resourceRatingTier = in.readUInt32();
                    _hasResourceRatingTier = true;
                    break; }
                case 104: {
                    watermarkingSupport = in.readInt32();
                    _hasWatermarkingSupport = true;
                    break; }
                case 112: {
                    initialRenewalDelayBase = in.readBool();
                    _hasInitialRenewalDelayBase = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ClientCapabilities fromBytes(byte[] in) throws EncodingException {
        ClientCapabilities message = new ClientCapabilities();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class ClientCredentials implements Message {
      
  
  
    protected int type; // 1
    protected boolean _hasType;
    protected byte[] token; // 2
    protected boolean _hasToken;
    
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
    public byte[] getToken() {
        return token;
    }
    
    public void setToken(byte[] token) {
        this.token = token;
        this._hasToken = true;
    }
    
    public void clearToken() {
        _hasToken = false;
    }
    
    public boolean hasToken() {
        return _hasToken;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasType)
            out.writeInt32(1, type);
        
        if(_hasToken)
            out.writeBytes(2, token);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    type = in.readInt32();
                    _hasType = true;
                    break; }
                case 18: {
                    token = in.readBytes();
                    _hasToken = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ClientCredentials fromBytes(byte[] in) throws EncodingException {
        ClientCredentials message = new ClientCredentials();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // TokenType
    public static final int KEYBOX = 0;
    public static final int DRM_DEVICE_CERTIFICATE = 1;
    public static final int REMOTE_ATTESTATION_CERTIFICATE = 2;
    public static final int OEM_DEVICE_CERTIFICATE = 3;
    public static final int BOOT_CERTIFICATE_CHAIN = 4;
    public static final int BOOT_CERTIFICATE_CHAIN_X509 = 5;
  
    protected int type; // 1
    protected boolean _hasType;
    protected byte[] token; // 2
    protected boolean _hasToken;
    protected Vector clientInfo = new Vector(); // 3
    protected byte[] providerClientToken; // 4
    protected boolean _hasProviderClientToken;
    protected int licenseCounter; // 5
    protected boolean _hasLicenseCounter;
    protected ClientIdentification.ClientCapabilities clientCapabilities; // 6
    protected byte[] vmpData; // 7
    protected boolean _hasVmpData;
    protected ClientIdentification.ClientCredentials deviceCredentials; // 8
    
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
    public byte[] getToken() {
        return token;
    }
    
    public void setToken(byte[] token) {
        this.token = token;
        this._hasToken = true;
    }
    
    public void clearToken() {
        _hasToken = false;
    }
    
    public boolean hasToken() {
        return _hasToken;
    }
    public void addClientInfo(ClientIdentification.NameValue value) {
        this.clientInfo.addElement(value);
    }

    public int getClientInfoCount() {
        return this.clientInfo.size();
    }

    public ClientIdentification.NameValue getClientInfo(int index) {
        return (ClientIdentification.NameValue)this.clientInfo.elementAt(index);
    }

    public Vector getClientInfoVector() {
        return this.clientInfo;
    }

    public void setClientInfoVector(Vector value) {
        this.clientInfo = value;
    }
    public byte[] getProviderClientToken() {
        return providerClientToken;
    }
    
    public void setProviderClientToken(byte[] providerClientToken) {
        this.providerClientToken = providerClientToken;
        this._hasProviderClientToken = true;
    }
    
    public void clearProviderClientToken() {
        _hasProviderClientToken = false;
    }
    
    public boolean hasProviderClientToken() {
        return _hasProviderClientToken;
    }
    public int getLicenseCounter() {
        return licenseCounter;
    }
    
    public void setLicenseCounter(int licenseCounter) {
        this.licenseCounter = licenseCounter;
        this._hasLicenseCounter = true;
    }
    
    public void clearLicenseCounter() {
        _hasLicenseCounter = false;
    }
    
    public boolean hasLicenseCounter() {
        return _hasLicenseCounter;
    }
    public ClientIdentification.ClientCapabilities getClientCapabilities() {
        return clientCapabilities;
    }
    
    public void setClientCapabilities(ClientIdentification.ClientCapabilities clientCapabilities) {
        this.clientCapabilities = clientCapabilities;
    }
    
    public void clearClientCapabilities() {
        clientCapabilities = null;
    }
    
    public boolean hasClientCapabilities() {
        return clientCapabilities != null;
    }
    public byte[] getVmpData() {
        return vmpData;
    }
    
    public void setVmpData(byte[] vmpData) {
        this.vmpData = vmpData;
        this._hasVmpData = true;
    }
    
    public void clearVmpData() {
        _hasVmpData = false;
    }
    
    public boolean hasVmpData() {
        return _hasVmpData;
    }
    public ClientIdentification.ClientCredentials getDeviceCredentials() {
        return deviceCredentials;
    }
    
    public void setDeviceCredentials(ClientIdentification.ClientCredentials deviceCredentials) {
        this.deviceCredentials = deviceCredentials;
    }
    
    public void clearDeviceCredentials() {
        deviceCredentials = null;
    }
    
    public boolean hasDeviceCredentials() {
        return deviceCredentials != null;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasType)
            out.writeInt32(1, type);
        
        if(_hasToken)
            out.writeBytes(2, token);
        
        for(int i = 0; i < getClientInfoCount(); i++) {
            out.writeMessage(3, getClientInfo(i));
        }
        
        if(_hasProviderClientToken)
            out.writeBytes(4, providerClientToken);
        
        if(_hasLicenseCounter)
            out.writeUInt32(5, licenseCounter);
        
        out.writeMessage(6, clientCapabilities);
        
        if(_hasVmpData)
            out.writeBytes(7, vmpData);
        
        out.writeMessage(8, deviceCredentials);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    type = in.readInt32();
                    _hasType = true;
                    break; }
                case 18: {
                    token = in.readBytes();
                    _hasToken = true;
                    break; }
                case 26: {
                    ClientIdentification.NameValue message = new ClientIdentification.NameValue();
                    in.readMessage(message);
                    addClientInfo(message);
                    break; }
                case 34: {
                    providerClientToken = in.readBytes();
                    _hasProviderClientToken = true;
                    break; }
                case 40: {
                    licenseCounter = in.readUInt32();
                    _hasLicenseCounter = true;
                    break; }
                case 50: {
                    clientCapabilities = new ClientIdentification.ClientCapabilities();
                    in.readMessage(clientCapabilities);
                    break; }
                case 58: {
                    vmpData = in.readBytes();
                    _hasVmpData = true;
                    break; }
                case 66: {
                    deviceCredentials = new ClientIdentification.ClientCredentials();
                    in.readMessage(deviceCredentials);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ClientIdentification fromBytes(byte[] in) throws EncodingException {
        ClientIdentification message = new ClientIdentification();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



