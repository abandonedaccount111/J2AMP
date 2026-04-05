package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class SignedDrmCertificate implements Message {
    
  
  
    protected byte[] drmCertificate; // 1
    protected boolean _hasDrmCertificate;
    protected byte[] signature; // 2
    protected boolean _hasSignature;
    protected SignedDrmCertificate signer; // 3
    protected int hashAlgorithm; // 4
    protected boolean _hasHashAlgorithm;
    
    public byte[] getDrmCertificate() {
        return drmCertificate;
    }
    
    public void setDrmCertificate(byte[] drmCertificate) {
        this.drmCertificate = drmCertificate;
        this._hasDrmCertificate = true;
    }
    
    public void clearDrmCertificate() {
        _hasDrmCertificate = false;
    }
    
    public boolean hasDrmCertificate() {
        return _hasDrmCertificate;
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
    public SignedDrmCertificate getSigner() {
        return signer;
    }
    
    public void setSigner(SignedDrmCertificate signer) {
        this.signer = signer;
    }
    
    public void clearSigner() {
        signer = null;
    }
    
    public boolean hasSigner() {
        return signer != null;
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasDrmCertificate)
            out.writeBytes(1, drmCertificate);
        
        if(_hasSignature)
            out.writeBytes(2, signature);
        
        out.writeMessage(3, signer);
        
        if(_hasHashAlgorithm)
            out.writeInt32(4, hashAlgorithm);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    drmCertificate = in.readBytes();
                    _hasDrmCertificate = true;
                    break; }
                case 18: {
                    signature = in.readBytes();
                    _hasSignature = true;
                    break; }
                case 26: {
                    signer = new SignedDrmCertificate();
                    in.readMessage(signer);
                    break; }
                case 32: {
                    hashAlgorithm = in.readInt32();
                    _hasHashAlgorithm = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static SignedDrmCertificate fromBytes(byte[] in) throws EncodingException {
        SignedDrmCertificate message = new SignedDrmCertificate();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



