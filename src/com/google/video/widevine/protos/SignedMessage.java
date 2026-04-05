package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class SignedMessage implements Message {
    
  
    // MessageType
    public static final int LICENSE_REQUEST = 1;
    public static final int LICENSE = 2;
    public static final int ERROR_RESPONSE = 3;
    public static final int SERVICE_CERTIFICATE_REQUEST = 4;
    public static final int SERVICE_CERTIFICATE = 5;
    public static final int SUB_LICENSE = 6;
    public static final int CAS_LICENSE_REQUEST = 7;
    public static final int CAS_LICENSE = 8;
    public static final int EXTERNAL_LICENSE_REQUEST = 9;
    public static final int EXTERNAL_LICENSE = 10;
    // SessionKeyType
    public static final int UNDEFINED = 0;
    public static final int WRAPPED_AES_KEY = 1;
    public static final int EPHEMERAL_ECC_PUBLIC_KEY = 2;
  
    protected int type; // 1
    protected boolean _hasType;
    protected byte[] msg; // 2
    protected boolean _hasMsg;
    protected byte[] signature; // 3
    protected boolean _hasSignature;
    protected byte[] sessionKey; // 4
    protected boolean _hasSessionKey;
    protected RemoteAttestation remoteAttestation; // 5
    protected Vector metricData = new Vector(); // 6
    protected VersionInfo serviceVersionInfo; // 7
    protected int sessionKeyType; // 8
    protected boolean _hasSessionKeyType;
    protected byte[] oemcryptoCoreMessage; // 9
    protected boolean _hasOemcryptoCoreMessage;
    protected int hashAlgorithm; // 10
    protected boolean _hasHashAlgorithm;
    protected boolean usingSecondaryKey; // 11
    protected boolean _hasUsingSecondaryKey;
    
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
    public byte[] getMsg() {
        return msg;
    }
    
    public void setMsg(byte[] msg) {
        this.msg = msg;
        this._hasMsg = true;
    }
    
    public void clearMsg() {
        _hasMsg = false;
    }
    
    public boolean hasMsg() {
        return _hasMsg;
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
    public void addMetricData(MetricData value) {
        this.metricData.addElement(value);
    }

    public int getMetricDataCount() {
        return this.metricData.size();
    }

    public MetricData getMetricData(int index) {
        return (MetricData)this.metricData.elementAt(index);
    }

    public Vector getMetricDataVector() {
        return this.metricData;
    }

    public void setMetricDataVector(Vector value) {
        this.metricData = value;
    }
    public VersionInfo getServiceVersionInfo() {
        return serviceVersionInfo;
    }
    
    public void setServiceVersionInfo(VersionInfo serviceVersionInfo) {
        this.serviceVersionInfo = serviceVersionInfo;
    }
    
    public void clearServiceVersionInfo() {
        serviceVersionInfo = null;
    }
    
    public boolean hasServiceVersionInfo() {
        return serviceVersionInfo != null;
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
    public boolean getUsingSecondaryKey() {
        return usingSecondaryKey;
    }
    
    public void setUsingSecondaryKey(boolean usingSecondaryKey) {
        this.usingSecondaryKey = usingSecondaryKey;
        this._hasUsingSecondaryKey = true;
    }
    
    public void clearUsingSecondaryKey() {
        _hasUsingSecondaryKey = false;
    }
    
    public boolean hasUsingSecondaryKey() {
        return _hasUsingSecondaryKey;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasType)
            out.writeInt32(1, type);
        
        if(_hasMsg)
            out.writeBytes(2, msg);
        
        if(_hasSignature)
            out.writeBytes(3, signature);
        
        if(_hasSessionKey)
            out.writeBytes(4, sessionKey);
        
        out.writeMessage(5, remoteAttestation);
        
        for(int i = 0; i < getMetricDataCount(); i++) {
            out.writeMessage(6, getMetricData(i));
        }
        
        out.writeMessage(7, serviceVersionInfo);
        
        if(_hasSessionKeyType)
            out.writeInt32(8, sessionKeyType);
        
        if(_hasOemcryptoCoreMessage)
            out.writeBytes(9, oemcryptoCoreMessage);
        
        if(_hasHashAlgorithm)
            out.writeInt32(10, hashAlgorithm);
        
        if(_hasUsingSecondaryKey)
            out.writeBool(11, usingSecondaryKey);
        
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
                    msg = in.readBytes();
                    _hasMsg = true;
                    break; }
                case 26: {
                    signature = in.readBytes();
                    _hasSignature = true;
                    break; }
                case 34: {
                    sessionKey = in.readBytes();
                    _hasSessionKey = true;
                    break; }
                case 42: {
                    remoteAttestation = new RemoteAttestation();
                    in.readMessage(remoteAttestation);
                    break; }
                case 50: {
                    MetricData message = new MetricData();
                    in.readMessage(message);
                    addMetricData(message);
                    break; }
                case 58: {
                    serviceVersionInfo = new VersionInfo();
                    in.readMessage(serviceVersionInfo);
                    break; }
                case 64: {
                    sessionKeyType = in.readInt32();
                    _hasSessionKeyType = true;
                    break; }
                case 74: {
                    oemcryptoCoreMessage = in.readBytes();
                    _hasOemcryptoCoreMessage = true;
                    break; }
                case 80: {
                    hashAlgorithm = in.readInt32();
                    _hasHashAlgorithm = true;
                    break; }
                case 88: {
                    usingSecondaryKey = in.readBool();
                    _hasUsingSecondaryKey = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static SignedMessage fromBytes(byte[] in) throws EncodingException {
        SignedMessage message = new SignedMessage();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



