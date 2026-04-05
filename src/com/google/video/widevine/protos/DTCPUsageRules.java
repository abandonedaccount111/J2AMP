package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class DTCPUsageRules implements Message {
    
  
    // RetentionState
    public static final int RETENTION_STATE_FOREVER = 0;
    public static final int RETENTION_STATE_1_WEEK = 1;
    public static final int RETENTION_STATE_2_DAYS = 2;
    public static final int RETENTION_STATE_1_DAY = 3;
    public static final int RETENTION_STATE_12_HOURS = 4;
    public static final int RETENTION_STATE_6_HOURS = 5;
    public static final int RETENTION_STATE_3_HOURS = 6;
    public static final int RETENTION_STATE_90_MINUTES = 7;
    // CopyControlInfo
    public static final int COPY_FREE = 0;
    public static final int COPY_NO_MORE = 1;
    public static final int COPY_ONE = 2;
    public static final int COPY_NEVER = 3;
    // AnalogProtectionSystem
    public static final int APS_OFF = 0;
    public static final int APS_TYPE1 = 1;
    public static final int APS_TYPE2 = 2;
    public static final int APS_TYPE3 = 3;
    // ImageConstraintToken
    public static final int ICT_CONSTRAINED = 0;
    public static final int ICT_HD_ANALOG = 1;
    // AnalogSunsetToken
    public static final int AST_ASSERTED = 0;
    public static final int AST_UNASERTED = 1;
    // DigitalOnlyToken
    public static final int DOT_ASSERTED = 0;
    public static final int DOT_UNASSERTED = 1;
    // AudioEnhancedToken
    public static final int AET_ASSERTED = 0;
    public static final int AET_UNASSERTED = 1;
    // StandardDigitalOutputToken
    public static final int SDO_UNASSEERTED = 0;
    public static final int SDO_ASSEERTED = 1;
    // HighDynamicRangeToken
    public static final int HDR_UNASSERTED = 0;
    public static final int HDR_ASSERTED = 1;
    // L2ProtectionOnlyToken
    public static final int L2_ONLY_UNASSERTED = 0;
    public static final int L2_ONLY_ASSERTED = 1;
    // EnhancedImageToken
    public static final int EI_UNASSERTED = 0;
    public static final int EI_ASSERTED = 1;
    // FurtherBoundCopy
    public static final int FBC_PROHIBITED = 0;
    public static final int FBC_PERMITTED = 1;
  
    protected boolean requireDtcp2; // 1
    protected boolean _hasRequireDtcp2;
    protected int copyControl; // 2
    protected boolean _hasCopyControl;
    protected boolean encryptionPlus; // 3
    protected boolean _hasEncryptionPlus;
    protected int retentionState; // 4
    protected boolean _hasRetentionState;
    protected int analogProtectionSystem; // 5
    protected boolean _hasAnalogProtectionSystem;
    protected int imageConstraintToken; // 6
    protected boolean _hasImageConstraintToken;
    protected int analogSunsetToken; // 7
    protected boolean _hasAnalogSunsetToken;
    protected int digitalOnlyToken; // 8
    protected boolean _hasDigitalOnlyToken;
    protected int audioEnhancedToken; // 9
    protected boolean _hasAudioEnhancedToken;
    protected int copyCount; // 10
    protected boolean _hasCopyCount;
    protected int standardDigitalToken; // 11
    protected boolean _hasStandardDigitalToken;
    protected int highDynamicToken; // 12
    protected boolean _hasHighDynamicToken;
    protected int l2OnlyToken; // 13
    protected boolean _hasL2OnlyToken;
    protected int enhanedImageToken; // 14
    protected boolean _hasEnhanedImageToken;
    protected int retentionTime; // 15
    protected boolean _hasRetentionTime;
    protected int furtherCopy; // 16
    protected boolean _hasFurtherCopy;
    
    public boolean getRequireDtcp2() {
        return requireDtcp2;
    }
    
    public void setRequireDtcp2(boolean requireDtcp2) {
        this.requireDtcp2 = requireDtcp2;
        this._hasRequireDtcp2 = true;
    }
    
    public void clearRequireDtcp2() {
        _hasRequireDtcp2 = false;
    }
    
    public boolean hasRequireDtcp2() {
        return _hasRequireDtcp2;
    }
    public int getCopyControl() {
        return copyControl;
    }
    
    public void setCopyControl(int copyControl) {
        this.copyControl = copyControl;
        this._hasCopyControl = true;
    }
    
    public void clearCopyControl() {
        _hasCopyControl = false;
    }
    
    public boolean hasCopyControl() {
        return _hasCopyControl;
    }
    public boolean getEncryptionPlus() {
        return encryptionPlus;
    }
    
    public void setEncryptionPlus(boolean encryptionPlus) {
        this.encryptionPlus = encryptionPlus;
        this._hasEncryptionPlus = true;
    }
    
    public void clearEncryptionPlus() {
        _hasEncryptionPlus = false;
    }
    
    public boolean hasEncryptionPlus() {
        return _hasEncryptionPlus;
    }
    public int getRetentionState() {
        return retentionState;
    }
    
    public void setRetentionState(int retentionState) {
        this.retentionState = retentionState;
        this._hasRetentionState = true;
    }
    
    public void clearRetentionState() {
        _hasRetentionState = false;
    }
    
    public boolean hasRetentionState() {
        return _hasRetentionState;
    }
    public int getAnalogProtectionSystem() {
        return analogProtectionSystem;
    }
    
    public void setAnalogProtectionSystem(int analogProtectionSystem) {
        this.analogProtectionSystem = analogProtectionSystem;
        this._hasAnalogProtectionSystem = true;
    }
    
    public void clearAnalogProtectionSystem() {
        _hasAnalogProtectionSystem = false;
    }
    
    public boolean hasAnalogProtectionSystem() {
        return _hasAnalogProtectionSystem;
    }
    public int getImageConstraintToken() {
        return imageConstraintToken;
    }
    
    public void setImageConstraintToken(int imageConstraintToken) {
        this.imageConstraintToken = imageConstraintToken;
        this._hasImageConstraintToken = true;
    }
    
    public void clearImageConstraintToken() {
        _hasImageConstraintToken = false;
    }
    
    public boolean hasImageConstraintToken() {
        return _hasImageConstraintToken;
    }
    public int getAnalogSunsetToken() {
        return analogSunsetToken;
    }
    
    public void setAnalogSunsetToken(int analogSunsetToken) {
        this.analogSunsetToken = analogSunsetToken;
        this._hasAnalogSunsetToken = true;
    }
    
    public void clearAnalogSunsetToken() {
        _hasAnalogSunsetToken = false;
    }
    
    public boolean hasAnalogSunsetToken() {
        return _hasAnalogSunsetToken;
    }
    public int getDigitalOnlyToken() {
        return digitalOnlyToken;
    }
    
    public void setDigitalOnlyToken(int digitalOnlyToken) {
        this.digitalOnlyToken = digitalOnlyToken;
        this._hasDigitalOnlyToken = true;
    }
    
    public void clearDigitalOnlyToken() {
        _hasDigitalOnlyToken = false;
    }
    
    public boolean hasDigitalOnlyToken() {
        return _hasDigitalOnlyToken;
    }
    public int getAudioEnhancedToken() {
        return audioEnhancedToken;
    }
    
    public void setAudioEnhancedToken(int audioEnhancedToken) {
        this.audioEnhancedToken = audioEnhancedToken;
        this._hasAudioEnhancedToken = true;
    }
    
    public void clearAudioEnhancedToken() {
        _hasAudioEnhancedToken = false;
    }
    
    public boolean hasAudioEnhancedToken() {
        return _hasAudioEnhancedToken;
    }
    public int getCopyCount() {
        return copyCount;
    }
    
    public void setCopyCount(int copyCount) {
        this.copyCount = copyCount;
        this._hasCopyCount = true;
    }
    
    public void clearCopyCount() {
        _hasCopyCount = false;
    }
    
    public boolean hasCopyCount() {
        return _hasCopyCount;
    }
    public int getStandardDigitalToken() {
        return standardDigitalToken;
    }
    
    public void setStandardDigitalToken(int standardDigitalToken) {
        this.standardDigitalToken = standardDigitalToken;
        this._hasStandardDigitalToken = true;
    }
    
    public void clearStandardDigitalToken() {
        _hasStandardDigitalToken = false;
    }
    
    public boolean hasStandardDigitalToken() {
        return _hasStandardDigitalToken;
    }
    public int getHighDynamicToken() {
        return highDynamicToken;
    }
    
    public void setHighDynamicToken(int highDynamicToken) {
        this.highDynamicToken = highDynamicToken;
        this._hasHighDynamicToken = true;
    }
    
    public void clearHighDynamicToken() {
        _hasHighDynamicToken = false;
    }
    
    public boolean hasHighDynamicToken() {
        return _hasHighDynamicToken;
    }
    public int getL2OnlyToken() {
        return l2OnlyToken;
    }
    
    public void setL2OnlyToken(int l2OnlyToken) {
        this.l2OnlyToken = l2OnlyToken;
        this._hasL2OnlyToken = true;
    }
    
    public void clearL2OnlyToken() {
        _hasL2OnlyToken = false;
    }
    
    public boolean hasL2OnlyToken() {
        return _hasL2OnlyToken;
    }
    public int getEnhanedImageToken() {
        return enhanedImageToken;
    }
    
    public void setEnhanedImageToken(int enhanedImageToken) {
        this.enhanedImageToken = enhanedImageToken;
        this._hasEnhanedImageToken = true;
    }
    
    public void clearEnhanedImageToken() {
        _hasEnhanedImageToken = false;
    }
    
    public boolean hasEnhanedImageToken() {
        return _hasEnhanedImageToken;
    }
    public int getRetentionTime() {
        return retentionTime;
    }
    
    public void setRetentionTime(int retentionTime) {
        this.retentionTime = retentionTime;
        this._hasRetentionTime = true;
    }
    
    public void clearRetentionTime() {
        _hasRetentionTime = false;
    }
    
    public boolean hasRetentionTime() {
        return _hasRetentionTime;
    }
    public int getFurtherCopy() {
        return furtherCopy;
    }
    
    public void setFurtherCopy(int furtherCopy) {
        this.furtherCopy = furtherCopy;
        this._hasFurtherCopy = true;
    }
    
    public void clearFurtherCopy() {
        _hasFurtherCopy = false;
    }
    
    public boolean hasFurtherCopy() {
        return _hasFurtherCopy;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasRequireDtcp2)
            out.writeBool(1, requireDtcp2);
        
        if(_hasCopyControl)
            out.writeInt32(2, copyControl);
        
        if(_hasEncryptionPlus)
            out.writeBool(3, encryptionPlus);
        
        if(_hasRetentionState)
            out.writeInt32(4, retentionState);
        
        if(_hasAnalogProtectionSystem)
            out.writeInt32(5, analogProtectionSystem);
        
        if(_hasImageConstraintToken)
            out.writeInt32(6, imageConstraintToken);
        
        if(_hasAnalogSunsetToken)
            out.writeInt32(7, analogSunsetToken);
        
        if(_hasDigitalOnlyToken)
            out.writeInt32(8, digitalOnlyToken);
        
        if(_hasAudioEnhancedToken)
            out.writeInt32(9, audioEnhancedToken);
        
        if(_hasCopyCount)
            out.writeUInt32(10, copyCount);
        
        if(_hasStandardDigitalToken)
            out.writeInt32(11, standardDigitalToken);
        
        if(_hasHighDynamicToken)
            out.writeInt32(12, highDynamicToken);
        
        if(_hasL2OnlyToken)
            out.writeInt32(13, l2OnlyToken);
        
        if(_hasEnhanedImageToken)
            out.writeInt32(14, enhanedImageToken);
        
        if(_hasRetentionTime)
            out.writeUInt32(15, retentionTime);
        
        if(_hasFurtherCopy)
            out.writeInt32(16, furtherCopy);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    requireDtcp2 = in.readBool();
                    _hasRequireDtcp2 = true;
                    break; }
                case 16: {
                    copyControl = in.readInt32();
                    _hasCopyControl = true;
                    break; }
                case 24: {
                    encryptionPlus = in.readBool();
                    _hasEncryptionPlus = true;
                    break; }
                case 32: {
                    retentionState = in.readInt32();
                    _hasRetentionState = true;
                    break; }
                case 40: {
                    analogProtectionSystem = in.readInt32();
                    _hasAnalogProtectionSystem = true;
                    break; }
                case 48: {
                    imageConstraintToken = in.readInt32();
                    _hasImageConstraintToken = true;
                    break; }
                case 56: {
                    analogSunsetToken = in.readInt32();
                    _hasAnalogSunsetToken = true;
                    break; }
                case 64: {
                    digitalOnlyToken = in.readInt32();
                    _hasDigitalOnlyToken = true;
                    break; }
                case 72: {
                    audioEnhancedToken = in.readInt32();
                    _hasAudioEnhancedToken = true;
                    break; }
                case 80: {
                    copyCount = in.readUInt32();
                    _hasCopyCount = true;
                    break; }
                case 88: {
                    standardDigitalToken = in.readInt32();
                    _hasStandardDigitalToken = true;
                    break; }
                case 96: {
                    highDynamicToken = in.readInt32();
                    _hasHighDynamicToken = true;
                    break; }
                case 104: {
                    l2OnlyToken = in.readInt32();
                    _hasL2OnlyToken = true;
                    break; }
                case 112: {
                    enhanedImageToken = in.readInt32();
                    _hasEnhanedImageToken = true;
                    break; }
                case 120: {
                    retentionTime = in.readUInt32();
                    _hasRetentionTime = true;
                    break; }
                case 128: {
                    furtherCopy = in.readInt32();
                    _hasFurtherCopy = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static DTCPUsageRules fromBytes(byte[] in) throws EncodingException {
        DTCPUsageRules message = new DTCPUsageRules();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



