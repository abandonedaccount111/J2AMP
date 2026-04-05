package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class ProvisioningContext implements Message {
    
  
  
    protected byte[] keyData; // 1
    protected boolean _hasKeyData;
    protected byte[] contextData; // 2
    protected boolean _hasContextData;
    
    public byte[] getKeyData() {
        return keyData;
    }
    
    public void setKeyData(byte[] keyData) {
        this.keyData = keyData;
        this._hasKeyData = true;
    }
    
    public void clearKeyData() {
        _hasKeyData = false;
    }
    
    public boolean hasKeyData() {
        return _hasKeyData;
    }
    public byte[] getContextData() {
        return contextData;
    }
    
    public void setContextData(byte[] contextData) {
        this.contextData = contextData;
        this._hasContextData = true;
    }
    
    public void clearContextData() {
        _hasContextData = false;
    }
    
    public boolean hasContextData() {
        return _hasContextData;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasKeyData)
            out.writeBytes(1, keyData);
        
        if(_hasContextData)
            out.writeBytes(2, contextData);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    keyData = in.readBytes();
                    _hasKeyData = true;
                    break; }
                case 18: {
                    contextData = in.readBytes();
                    _hasContextData = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static ProvisioningContext fromBytes(byte[] in) throws EncodingException {
        ProvisioningContext message = new ProvisioningContext();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



