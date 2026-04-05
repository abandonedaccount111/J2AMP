package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class MetricData implements Message {
    
    public static class TypeValue implements Message {
      
  
  
    protected int type; // 1
    protected boolean _hasType;
    protected long value; // 2
    protected boolean _hasValue;
    
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
    public long getValue() {
        return value;
    }
    
    public void setValue(long value) {
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
        if(_hasType)
            out.writeInt32(1, type);
        
        if(_hasValue)
            out.writeInt64(2, value);
        
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
                case 16: {
                    value = in.readInt64();
                    _hasValue = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static TypeValue fromBytes(byte[] in) throws EncodingException {
        TypeValue message = new TypeValue();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // MetricType
    public static final int LATENCY = 1;
    public static final int TIMESTAMP = 2;
  
    protected String stageName; // 1
    protected boolean _hasStageName;
    protected Vector metricData = new Vector(); // 2
    
    public String getStageName() {
        return stageName;
    }
    
    public void setStageName(String stageName) {
        this.stageName = stageName;
        this._hasStageName = true;
    }
    
    public void clearStageName() {
        _hasStageName = false;
    }
    
    public boolean hasStageName() {
        return _hasStageName;
    }
    public void addMetricData(MetricData.TypeValue value) {
        this.metricData.addElement(value);
    }

    public int getMetricDataCount() {
        return this.metricData.size();
    }

    public MetricData.TypeValue getMetricData(int index) {
        return (MetricData.TypeValue)this.metricData.elementAt(index);
    }

    public Vector getMetricDataVector() {
        return this.metricData;
    }

    public void setMetricDataVector(Vector value) {
        this.metricData = value;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasStageName)
            out.writeString(1, stageName);
        
        for(int i = 0; i < getMetricDataCount(); i++) {
            out.writeMessage(2, getMetricData(i));
        }
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    stageName = in.readString();
                    _hasStageName = true;
                    break; }
                case 18: {
                    MetricData.TypeValue message = new MetricData.TypeValue();
                    in.readMessage(message);
                    addMetricData(message);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static MetricData fromBytes(byte[] in) throws EncodingException {
        MetricData message = new MetricData();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



