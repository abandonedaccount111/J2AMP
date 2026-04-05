package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class DrmCertificate implements Message {
    
    public static class EncryptionKey implements Message {
      
  
  
    protected byte[] publicKey; // 1
    protected boolean _hasPublicKey;
    protected int algorithm; // 2
    protected boolean _hasAlgorithm;
    
    public byte[] getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
        this._hasPublicKey = true;
    }
    
    public void clearPublicKey() {
        _hasPublicKey = false;
    }
    
    public boolean hasPublicKey() {
        return _hasPublicKey;
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasPublicKey)
            out.writeBytes(1, publicKey);
        
        if(_hasAlgorithm)
            out.writeInt32(2, algorithm);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    publicKey = in.readBytes();
                    _hasPublicKey = true;
                    break; }
                case 16: {
                    algorithm = in.readInt32();
                    _hasAlgorithm = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static EncryptionKey fromBytes(byte[] in) throws EncodingException {
        EncryptionKey message = new EncryptionKey();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // Type
    public static final int ROOT = 0;
    public static final int DEVICE_MODEL = 1;
    public static final int DEVICE = 2;
    public static final int SERVICE = 3;
    public static final int PROVISIONER = 4;
    public static final int DEVICE_EMBEDDED = 5;
    // ServiceType
    public static final int UNKNOWN_SERVICE_TYPE = 0;
    public static final int LICENSE_SERVER_SDK = 1;
    public static final int LICENSE_SERVER_PROXY_SDK = 2;
    public static final int PROVISIONING_SDK = 3;
    public static final int CAS_PROXY_SDK = 4;
    // Algorithm
    public static final int UNKNOWN_ALGORITHM = 0;
    public static final int RSA = 1;
    public static final int ECC_SECP256R1 = 2;
    public static final int ECC_SECP384R1 = 3;
    public static final int ECC_SECP521R1 = 4;
  
    protected int type; // 1
    protected boolean _hasType;
    protected byte[] serialNumber; // 2
    protected boolean _hasSerialNumber;
    protected int creationTimeSeconds; // 3
    protected boolean _hasCreationTimeSeconds;
    protected int expirationTimeSeconds; // 12
    protected boolean _hasExpirationTimeSeconds;
    protected byte[] publicKey; // 4
    protected boolean _hasPublicKey;
    protected int systemId; // 5
    protected boolean _hasSystemId;
    protected boolean testDeviceDeprecated; // 6
    protected boolean _hasTestDeviceDeprecated;
    protected String providerId; // 7
    protected boolean _hasProviderId;
    protected Vector serviceTypes = new Vector(); // 8
    protected int algorithm; // 9
    protected boolean _hasAlgorithm;
    protected RootOfTrustId rotId; // 10
    protected DrmCertificate.EncryptionKey encryptionKey; // 11
    
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
    public byte[] getSerialNumber() {
        return serialNumber;
    }
    
    public void setSerialNumber(byte[] serialNumber) {
        this.serialNumber = serialNumber;
        this._hasSerialNumber = true;
    }
    
    public void clearSerialNumber() {
        _hasSerialNumber = false;
    }
    
    public boolean hasSerialNumber() {
        return _hasSerialNumber;
    }
    public int getCreationTimeSeconds() {
        return creationTimeSeconds;
    }
    
    public void setCreationTimeSeconds(int creationTimeSeconds) {
        this.creationTimeSeconds = creationTimeSeconds;
        this._hasCreationTimeSeconds = true;
    }
    
    public void clearCreationTimeSeconds() {
        _hasCreationTimeSeconds = false;
    }
    
    public boolean hasCreationTimeSeconds() {
        return _hasCreationTimeSeconds;
    }
    public int getExpirationTimeSeconds() {
        return expirationTimeSeconds;
    }
    
    public void setExpirationTimeSeconds(int expirationTimeSeconds) {
        this.expirationTimeSeconds = expirationTimeSeconds;
        this._hasExpirationTimeSeconds = true;
    }
    
    public void clearExpirationTimeSeconds() {
        _hasExpirationTimeSeconds = false;
    }
    
    public boolean hasExpirationTimeSeconds() {
        return _hasExpirationTimeSeconds;
    }
    public byte[] getPublicKey() {
        return publicKey;
    }
    
    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
        this._hasPublicKey = true;
    }
    
    public void clearPublicKey() {
        _hasPublicKey = false;
    }
    
    public boolean hasPublicKey() {
        return _hasPublicKey;
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
    public boolean getTestDeviceDeprecated() {
        return testDeviceDeprecated;
    }
    
    public void setTestDeviceDeprecated(boolean testDeviceDeprecated) {
        this.testDeviceDeprecated = testDeviceDeprecated;
        this._hasTestDeviceDeprecated = true;
    }
    
    public void clearTestDeviceDeprecated() {
        _hasTestDeviceDeprecated = false;
    }
    
    public boolean hasTestDeviceDeprecated() {
        return _hasTestDeviceDeprecated;
    }
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
    public void addServiceTypes(int value) {
        this.serviceTypes.addElement(new Integer(value));
    }

    public int getServiceTypesCount() {
        return this.serviceTypes.size();
    }

    public int getServiceTypes(int index) {
        return ((Integer)this.serviceTypes.elementAt(index)).intValue();
    }

    public Vector getServiceTypesVector() {
        return this.serviceTypes;
    }

    public void setServiceTypesVector(Vector value) {
        this.serviceTypes = value;
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
    public RootOfTrustId getRotId() {
        return rotId;
    }
    
    public void setRotId(RootOfTrustId rotId) {
        this.rotId = rotId;
    }
    
    public void clearRotId() {
        rotId = null;
    }
    
    public boolean hasRotId() {
        return rotId != null;
    }
    public DrmCertificate.EncryptionKey getEncryptionKey() {
        return encryptionKey;
    }
    
    public void setEncryptionKey(DrmCertificate.EncryptionKey encryptionKey) {
        this.encryptionKey = encryptionKey;
    }
    
    public void clearEncryptionKey() {
        encryptionKey = null;
    }
    
    public boolean hasEncryptionKey() {
        return encryptionKey != null;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasType)
            out.writeInt32(1, type);
        
        if(_hasSerialNumber)
            out.writeBytes(2, serialNumber);
        
        if(_hasCreationTimeSeconds)
            out.writeUInt32(3, creationTimeSeconds);
        
        if(_hasExpirationTimeSeconds)
            out.writeUInt32(12, expirationTimeSeconds);
        
        if(_hasPublicKey)
            out.writeBytes(4, publicKey);
        
        if(_hasSystemId)
            out.writeUInt32(5, systemId);
        
        if(_hasTestDeviceDeprecated)
            out.writeBool(6, testDeviceDeprecated);
        
        if(_hasProviderId)
            out.writeString(7, providerId);
        
        for(int i = 0; i < getServiceTypesCount(); i++) {
            out.writeInt32(8, getServiceTypes(i));
        }
        
        if(_hasAlgorithm)
            out.writeInt32(9, algorithm);
        
        out.writeMessage(10, rotId);
        
        out.writeMessage(11, encryptionKey);
        
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
                    serialNumber = in.readBytes();
                    _hasSerialNumber = true;
                    break; }
                case 24: {
                    creationTimeSeconds = in.readUInt32();
                    _hasCreationTimeSeconds = true;
                    break; }
                case 96: {
                    expirationTimeSeconds = in.readUInt32();
                    _hasExpirationTimeSeconds = true;
                    break; }
                case 34: {
                    publicKey = in.readBytes();
                    _hasPublicKey = true;
                    break; }
                case 40: {
                    systemId = in.readUInt32();
                    _hasSystemId = true;
                    break; }
                case 48: {
                    testDeviceDeprecated = in.readBool();
                    _hasTestDeviceDeprecated = true;
                    break; }
                case 58: {
                    providerId = in.readString();
                    _hasProviderId = true;
                    break; }
                case 64: {
                    addServiceTypes(in.readInt32());
                    break; }
                case 72: {
                    algorithm = in.readInt32();
                    _hasAlgorithm = true;
                    break; }
                case 82: {
                    rotId = new RootOfTrustId();
                    in.readMessage(rotId);
                    break; }
                case 90: {
                    encryptionKey = new DrmCertificate.EncryptionKey();
                    in.readMessage(encryptionKey);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static DrmCertificate fromBytes(byte[] in) throws EncodingException {
        DrmCertificate message = new DrmCertificate();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



