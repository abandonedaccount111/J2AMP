package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class RemoteAttestation implements Message {
    
  
  
    protected EncryptedClientIdentification certificate; // 1
    protected byte[] salt; // 2
    protected boolean _hasSalt;
    protected byte[] signature; // 3
    protected boolean _hasSignature;
    
    public EncryptedClientIdentification getCertificate() {
        return certificate;
    }
    
    public void setCertificate(EncryptedClientIdentification certificate) {
        this.certificate = certificate;
    }
    
    public void clearCertificate() {
        certificate = null;
    }
    
    public boolean hasCertificate() {
        return certificate != null;
    }
    public byte[] getSalt() {
        return salt;
    }
    
    public void setSalt(byte[] salt) {
        this.salt = salt;
        this._hasSalt = true;
    }
    
    public void clearSalt() {
        _hasSalt = false;
    }
    
    public boolean hasSalt() {
        return _hasSalt;
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        out.writeMessage(1, certificate);
        
        if(_hasSalt)
            out.writeBytes(2, salt);
        
        if(_hasSignature)
            out.writeBytes(3, signature);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    certificate = new EncryptedClientIdentification();
                    in.readMessage(certificate);
                    break; }
                case 18: {
                    salt = in.readBytes();
                    _hasSalt = true;
                    break; }
                case 26: {
                    signature = in.readBytes();
                    _hasSignature = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static RemoteAttestation fromBytes(byte[] in) throws EncodingException {
        RemoteAttestation message = new RemoteAttestation();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



