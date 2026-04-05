package com.google.video.widevine.protos;

import java.util.Vector;
import java.io.IOException;

import com.ponderingpanda.protobuf.*;


public class License implements Message {
    
    public static class Policy implements Message {
      
  
    // WatermarkingControl
    public static final int WATERMARKING_CONTROL_UNSPECIFIED = 0;
    public static final int WATERMARKING_FORBIDDEN = 1;
    public static final int WATERMARKING_REQUIRED = 2;
    // TimerDelayBase
    public static final int TIMER_DELAY_BASE_UNSPECIFIED = 0;
    public static final int LICENSE_START = 1;
    public static final int LICENSE_LOAD = 2;
    public static final int FIRST_DECRYPT = 3;
  
    protected boolean canPlay; // 1
    protected boolean _hasCanPlay;
    protected boolean canPersist; // 2
    protected boolean _hasCanPersist;
    protected boolean canRenew; // 3
    protected boolean _hasCanRenew;
    protected long rentalDurationSeconds; // 4
    protected boolean _hasRentalDurationSeconds;
    protected long playbackDurationSeconds; // 5
    protected boolean _hasPlaybackDurationSeconds;
    protected long licenseDurationSeconds; // 6
    protected boolean _hasLicenseDurationSeconds;
    protected long renewalRecoveryDurationSeconds; // 7
    protected boolean _hasRenewalRecoveryDurationSeconds;
    protected String renewalServerUrl; // 8
    protected boolean _hasRenewalServerUrl;
    protected long renewalDelaySeconds; // 9
    protected boolean _hasRenewalDelaySeconds;
    protected long renewalRetryIntervalSeconds; // 10
    protected boolean _hasRenewalRetryIntervalSeconds;
    protected boolean renewWithUsage; // 11
    protected boolean _hasRenewWithUsage;
    protected boolean alwaysIncludeClientId; // 12
    protected boolean _hasAlwaysIncludeClientId;
    protected long playStartGracePeriodSeconds; // 13
    protected boolean _hasPlayStartGracePeriodSeconds;
    protected boolean softEnforcePlaybackDuration; // 14
    protected boolean _hasSoftEnforcePlaybackDuration;
    protected boolean softEnforceRentalDuration; // 15
    protected boolean _hasSoftEnforceRentalDuration;
    protected int watermarkingControl; // 16
    protected boolean _hasWatermarkingControl;
    protected DTCPUsageRules dtcp2; // 17
    protected int initialRenewalDelayBase; // 18
    protected boolean _hasInitialRenewalDelayBase;
    
    public boolean getCanPlay() {
        return canPlay;
    }
    
    public void setCanPlay(boolean canPlay) {
        this.canPlay = canPlay;
        this._hasCanPlay = true;
    }
    
    public void clearCanPlay() {
        _hasCanPlay = false;
    }
    
    public boolean hasCanPlay() {
        return _hasCanPlay;
    }
    public boolean getCanPersist() {
        return canPersist;
    }
    
    public void setCanPersist(boolean canPersist) {
        this.canPersist = canPersist;
        this._hasCanPersist = true;
    }
    
    public void clearCanPersist() {
        _hasCanPersist = false;
    }
    
    public boolean hasCanPersist() {
        return _hasCanPersist;
    }
    public boolean getCanRenew() {
        return canRenew;
    }
    
    public void setCanRenew(boolean canRenew) {
        this.canRenew = canRenew;
        this._hasCanRenew = true;
    }
    
    public void clearCanRenew() {
        _hasCanRenew = false;
    }
    
    public boolean hasCanRenew() {
        return _hasCanRenew;
    }
    public long getRentalDurationSeconds() {
        return rentalDurationSeconds;
    }
    
    public void setRentalDurationSeconds(long rentalDurationSeconds) {
        this.rentalDurationSeconds = rentalDurationSeconds;
        this._hasRentalDurationSeconds = true;
    }
    
    public void clearRentalDurationSeconds() {
        _hasRentalDurationSeconds = false;
    }
    
    public boolean hasRentalDurationSeconds() {
        return _hasRentalDurationSeconds;
    }
    public long getPlaybackDurationSeconds() {
        return playbackDurationSeconds;
    }
    
    public void setPlaybackDurationSeconds(long playbackDurationSeconds) {
        this.playbackDurationSeconds = playbackDurationSeconds;
        this._hasPlaybackDurationSeconds = true;
    }
    
    public void clearPlaybackDurationSeconds() {
        _hasPlaybackDurationSeconds = false;
    }
    
    public boolean hasPlaybackDurationSeconds() {
        return _hasPlaybackDurationSeconds;
    }
    public long getLicenseDurationSeconds() {
        return licenseDurationSeconds;
    }
    
    public void setLicenseDurationSeconds(long licenseDurationSeconds) {
        this.licenseDurationSeconds = licenseDurationSeconds;
        this._hasLicenseDurationSeconds = true;
    }
    
    public void clearLicenseDurationSeconds() {
        _hasLicenseDurationSeconds = false;
    }
    
    public boolean hasLicenseDurationSeconds() {
        return _hasLicenseDurationSeconds;
    }
    public long getRenewalRecoveryDurationSeconds() {
        return renewalRecoveryDurationSeconds;
    }
    
    public void setRenewalRecoveryDurationSeconds(long renewalRecoveryDurationSeconds) {
        this.renewalRecoveryDurationSeconds = renewalRecoveryDurationSeconds;
        this._hasRenewalRecoveryDurationSeconds = true;
    }
    
    public void clearRenewalRecoveryDurationSeconds() {
        _hasRenewalRecoveryDurationSeconds = false;
    }
    
    public boolean hasRenewalRecoveryDurationSeconds() {
        return _hasRenewalRecoveryDurationSeconds;
    }
    public String getRenewalServerUrl() {
        return renewalServerUrl;
    }
    
    public void setRenewalServerUrl(String renewalServerUrl) {
        this.renewalServerUrl = renewalServerUrl;
        this._hasRenewalServerUrl = true;
    }
    
    public void clearRenewalServerUrl() {
        _hasRenewalServerUrl = false;
    }
    
    public boolean hasRenewalServerUrl() {
        return _hasRenewalServerUrl;
    }
    public long getRenewalDelaySeconds() {
        return renewalDelaySeconds;
    }
    
    public void setRenewalDelaySeconds(long renewalDelaySeconds) {
        this.renewalDelaySeconds = renewalDelaySeconds;
        this._hasRenewalDelaySeconds = true;
    }
    
    public void clearRenewalDelaySeconds() {
        _hasRenewalDelaySeconds = false;
    }
    
    public boolean hasRenewalDelaySeconds() {
        return _hasRenewalDelaySeconds;
    }
    public long getRenewalRetryIntervalSeconds() {
        return renewalRetryIntervalSeconds;
    }
    
    public void setRenewalRetryIntervalSeconds(long renewalRetryIntervalSeconds) {
        this.renewalRetryIntervalSeconds = renewalRetryIntervalSeconds;
        this._hasRenewalRetryIntervalSeconds = true;
    }
    
    public void clearRenewalRetryIntervalSeconds() {
        _hasRenewalRetryIntervalSeconds = false;
    }
    
    public boolean hasRenewalRetryIntervalSeconds() {
        return _hasRenewalRetryIntervalSeconds;
    }
    public boolean getRenewWithUsage() {
        return renewWithUsage;
    }
    
    public void setRenewWithUsage(boolean renewWithUsage) {
        this.renewWithUsage = renewWithUsage;
        this._hasRenewWithUsage = true;
    }
    
    public void clearRenewWithUsage() {
        _hasRenewWithUsage = false;
    }
    
    public boolean hasRenewWithUsage() {
        return _hasRenewWithUsage;
    }
    public boolean getAlwaysIncludeClientId() {
        return alwaysIncludeClientId;
    }
    
    public void setAlwaysIncludeClientId(boolean alwaysIncludeClientId) {
        this.alwaysIncludeClientId = alwaysIncludeClientId;
        this._hasAlwaysIncludeClientId = true;
    }
    
    public void clearAlwaysIncludeClientId() {
        _hasAlwaysIncludeClientId = false;
    }
    
    public boolean hasAlwaysIncludeClientId() {
        return _hasAlwaysIncludeClientId;
    }
    public long getPlayStartGracePeriodSeconds() {
        return playStartGracePeriodSeconds;
    }
    
    public void setPlayStartGracePeriodSeconds(long playStartGracePeriodSeconds) {
        this.playStartGracePeriodSeconds = playStartGracePeriodSeconds;
        this._hasPlayStartGracePeriodSeconds = true;
    }
    
    public void clearPlayStartGracePeriodSeconds() {
        _hasPlayStartGracePeriodSeconds = false;
    }
    
    public boolean hasPlayStartGracePeriodSeconds() {
        return _hasPlayStartGracePeriodSeconds;
    }
    public boolean getSoftEnforcePlaybackDuration() {
        return softEnforcePlaybackDuration;
    }
    
    public void setSoftEnforcePlaybackDuration(boolean softEnforcePlaybackDuration) {
        this.softEnforcePlaybackDuration = softEnforcePlaybackDuration;
        this._hasSoftEnforcePlaybackDuration = true;
    }
    
    public void clearSoftEnforcePlaybackDuration() {
        _hasSoftEnforcePlaybackDuration = false;
    }
    
    public boolean hasSoftEnforcePlaybackDuration() {
        return _hasSoftEnforcePlaybackDuration;
    }
    public boolean getSoftEnforceRentalDuration() {
        return softEnforceRentalDuration;
    }
    
    public void setSoftEnforceRentalDuration(boolean softEnforceRentalDuration) {
        this.softEnforceRentalDuration = softEnforceRentalDuration;
        this._hasSoftEnforceRentalDuration = true;
    }
    
    public void clearSoftEnforceRentalDuration() {
        _hasSoftEnforceRentalDuration = false;
    }
    
    public boolean hasSoftEnforceRentalDuration() {
        return _hasSoftEnforceRentalDuration;
    }
    public int getWatermarkingControl() {
        return watermarkingControl;
    }
    
    public void setWatermarkingControl(int watermarkingControl) {
        this.watermarkingControl = watermarkingControl;
        this._hasWatermarkingControl = true;
    }
    
    public void clearWatermarkingControl() {
        _hasWatermarkingControl = false;
    }
    
    public boolean hasWatermarkingControl() {
        return _hasWatermarkingControl;
    }
    public DTCPUsageRules getDtcp2() {
        return dtcp2;
    }
    
    public void setDtcp2(DTCPUsageRules dtcp2) {
        this.dtcp2 = dtcp2;
    }
    
    public void clearDtcp2() {
        dtcp2 = null;
    }
    
    public boolean hasDtcp2() {
        return dtcp2 != null;
    }
    public int getInitialRenewalDelayBase() {
        return initialRenewalDelayBase;
    }
    
    public void setInitialRenewalDelayBase(int initialRenewalDelayBase) {
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
        if(_hasCanPlay)
            out.writeBool(1, canPlay);
        
        if(_hasCanPersist)
            out.writeBool(2, canPersist);
        
        if(_hasCanRenew)
            out.writeBool(3, canRenew);
        
        if(_hasRentalDurationSeconds)
            out.writeInt64(4, rentalDurationSeconds);
        
        if(_hasPlaybackDurationSeconds)
            out.writeInt64(5, playbackDurationSeconds);
        
        if(_hasLicenseDurationSeconds)
            out.writeInt64(6, licenseDurationSeconds);
        
        if(_hasRenewalRecoveryDurationSeconds)
            out.writeInt64(7, renewalRecoveryDurationSeconds);
        
        if(_hasRenewalServerUrl)
            out.writeString(8, renewalServerUrl);
        
        if(_hasRenewalDelaySeconds)
            out.writeInt64(9, renewalDelaySeconds);
        
        if(_hasRenewalRetryIntervalSeconds)
            out.writeInt64(10, renewalRetryIntervalSeconds);
        
        if(_hasRenewWithUsage)
            out.writeBool(11, renewWithUsage);
        
        if(_hasAlwaysIncludeClientId)
            out.writeBool(12, alwaysIncludeClientId);
        
        if(_hasPlayStartGracePeriodSeconds)
            out.writeInt64(13, playStartGracePeriodSeconds);
        
        if(_hasSoftEnforcePlaybackDuration)
            out.writeBool(14, softEnforcePlaybackDuration);
        
        if(_hasSoftEnforceRentalDuration)
            out.writeBool(15, softEnforceRentalDuration);
        
        if(_hasWatermarkingControl)
            out.writeInt32(16, watermarkingControl);
        
        out.writeMessage(17, dtcp2);
        
        if(_hasInitialRenewalDelayBase)
            out.writeInt32(18, initialRenewalDelayBase);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    canPlay = in.readBool();
                    _hasCanPlay = true;
                    break; }
                case 16: {
                    canPersist = in.readBool();
                    _hasCanPersist = true;
                    break; }
                case 24: {
                    canRenew = in.readBool();
                    _hasCanRenew = true;
                    break; }
                case 32: {
                    rentalDurationSeconds = in.readInt64();
                    _hasRentalDurationSeconds = true;
                    break; }
                case 40: {
                    playbackDurationSeconds = in.readInt64();
                    _hasPlaybackDurationSeconds = true;
                    break; }
                case 48: {
                    licenseDurationSeconds = in.readInt64();
                    _hasLicenseDurationSeconds = true;
                    break; }
                case 56: {
                    renewalRecoveryDurationSeconds = in.readInt64();
                    _hasRenewalRecoveryDurationSeconds = true;
                    break; }
                case 66: {
                    renewalServerUrl = in.readString();
                    _hasRenewalServerUrl = true;
                    break; }
                case 72: {
                    renewalDelaySeconds = in.readInt64();
                    _hasRenewalDelaySeconds = true;
                    break; }
                case 80: {
                    renewalRetryIntervalSeconds = in.readInt64();
                    _hasRenewalRetryIntervalSeconds = true;
                    break; }
                case 88: {
                    renewWithUsage = in.readBool();
                    _hasRenewWithUsage = true;
                    break; }
                case 96: {
                    alwaysIncludeClientId = in.readBool();
                    _hasAlwaysIncludeClientId = true;
                    break; }
                case 104: {
                    playStartGracePeriodSeconds = in.readInt64();
                    _hasPlayStartGracePeriodSeconds = true;
                    break; }
                case 112: {
                    softEnforcePlaybackDuration = in.readBool();
                    _hasSoftEnforcePlaybackDuration = true;
                    break; }
                case 120: {
                    softEnforceRentalDuration = in.readBool();
                    _hasSoftEnforceRentalDuration = true;
                    break; }
                case 128: {
                    watermarkingControl = in.readInt32();
                    _hasWatermarkingControl = true;
                    break; }
                case 138: {
                    dtcp2 = new DTCPUsageRules();
                    in.readMessage(dtcp2);
                    break; }
                case 144: {
                    initialRenewalDelayBase = in.readInt32();
                    _hasInitialRenewalDelayBase = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static Policy fromBytes(byte[] in) throws EncodingException {
        Policy message = new Policy();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class KeyContainer implements Message {
      
    public static class KeyControl implements Message {
      
  
  
    protected byte[] keyControlBlock; // 1
    protected boolean _hasKeyControlBlock;
    protected byte[] iv; // 2
    protected boolean _hasIv;
    
    public byte[] getKeyControlBlock() {
        return keyControlBlock;
    }
    
    public void setKeyControlBlock(byte[] keyControlBlock) {
        this.keyControlBlock = keyControlBlock;
        this._hasKeyControlBlock = true;
    }
    
    public void clearKeyControlBlock() {
        _hasKeyControlBlock = false;
    }
    
    public boolean hasKeyControlBlock() {
        return _hasKeyControlBlock;
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
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasKeyControlBlock)
            out.writeBytes(1, keyControlBlock);
        
        if(_hasIv)
            out.writeBytes(2, iv);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    keyControlBlock = in.readBytes();
                    _hasKeyControlBlock = true;
                    break; }
                case 18: {
                    iv = in.readBytes();
                    _hasIv = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static KeyControl fromBytes(byte[] in) throws EncodingException {
        KeyControl message = new KeyControl();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class OutputProtection implements Message {
      
  
    // HDCP
    public static final int HDCP_NONE = 0;
    public static final int HDCP_V1 = 1;
    public static final int HDCP_V2 = 2;
    public static final int HDCP_V2_1 = 3;
    public static final int HDCP_V2_2 = 4;
    public static final int HDCP_V2_3 = 5;
    public static final int HDCP_NO_DIGITAL_OUTPUT = 255;
    // CGMS
    public static final int CGMS_NONE = 42;
    public static final int COPY_FREE = 0;
    public static final int COPY_ONCE = 2;
    public static final int COPY_NEVER = 3;
    // HdcpSrmRule
    public static final int HDCP_SRM_RULE_NONE = 0;
    public static final int CURRENT_SRM = 1;
  
    protected int hdcp; // 1
    protected boolean _hasHdcp;
    protected int cgmsFlags; // 2
    protected boolean _hasCgmsFlags;
    protected int hdcpSrmRule; // 3
    protected boolean _hasHdcpSrmRule;
    protected boolean disableAnalogOutput; // 4
    protected boolean _hasDisableAnalogOutput;
    protected boolean disableDigitalOutput; // 5
    protected boolean _hasDisableDigitalOutput;
    protected boolean allowRecord; // 6
    protected boolean _hasAllowRecord;
    
    public int getHdcp() {
        return hdcp;
    }
    
    public void setHdcp(int hdcp) {
        this.hdcp = hdcp;
        this._hasHdcp = true;
    }
    
    public void clearHdcp() {
        _hasHdcp = false;
    }
    
    public boolean hasHdcp() {
        return _hasHdcp;
    }
    public int getCgmsFlags() {
        return cgmsFlags;
    }
    
    public void setCgmsFlags(int cgmsFlags) {
        this.cgmsFlags = cgmsFlags;
        this._hasCgmsFlags = true;
    }
    
    public void clearCgmsFlags() {
        _hasCgmsFlags = false;
    }
    
    public boolean hasCgmsFlags() {
        return _hasCgmsFlags;
    }
    public int getHdcpSrmRule() {
        return hdcpSrmRule;
    }
    
    public void setHdcpSrmRule(int hdcpSrmRule) {
        this.hdcpSrmRule = hdcpSrmRule;
        this._hasHdcpSrmRule = true;
    }
    
    public void clearHdcpSrmRule() {
        _hasHdcpSrmRule = false;
    }
    
    public boolean hasHdcpSrmRule() {
        return _hasHdcpSrmRule;
    }
    public boolean getDisableAnalogOutput() {
        return disableAnalogOutput;
    }
    
    public void setDisableAnalogOutput(boolean disableAnalogOutput) {
        this.disableAnalogOutput = disableAnalogOutput;
        this._hasDisableAnalogOutput = true;
    }
    
    public void clearDisableAnalogOutput() {
        _hasDisableAnalogOutput = false;
    }
    
    public boolean hasDisableAnalogOutput() {
        return _hasDisableAnalogOutput;
    }
    public boolean getDisableDigitalOutput() {
        return disableDigitalOutput;
    }
    
    public void setDisableDigitalOutput(boolean disableDigitalOutput) {
        this.disableDigitalOutput = disableDigitalOutput;
        this._hasDisableDigitalOutput = true;
    }
    
    public void clearDisableDigitalOutput() {
        _hasDisableDigitalOutput = false;
    }
    
    public boolean hasDisableDigitalOutput() {
        return _hasDisableDigitalOutput;
    }
    public boolean getAllowRecord() {
        return allowRecord;
    }
    
    public void setAllowRecord(boolean allowRecord) {
        this.allowRecord = allowRecord;
        this._hasAllowRecord = true;
    }
    
    public void clearAllowRecord() {
        _hasAllowRecord = false;
    }
    
    public boolean hasAllowRecord() {
        return _hasAllowRecord;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasHdcp)
            out.writeInt32(1, hdcp);
        
        if(_hasCgmsFlags)
            out.writeInt32(2, cgmsFlags);
        
        if(_hasHdcpSrmRule)
            out.writeInt32(3, hdcpSrmRule);
        
        if(_hasDisableAnalogOutput)
            out.writeBool(4, disableAnalogOutput);
        
        if(_hasDisableDigitalOutput)
            out.writeBool(5, disableDigitalOutput);
        
        if(_hasAllowRecord)
            out.writeBool(6, allowRecord);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    hdcp = in.readInt32();
                    _hasHdcp = true;
                    break; }
                case 16: {
                    cgmsFlags = in.readInt32();
                    _hasCgmsFlags = true;
                    break; }
                case 24: {
                    hdcpSrmRule = in.readInt32();
                    _hasHdcpSrmRule = true;
                    break; }
                case 32: {
                    disableAnalogOutput = in.readBool();
                    _hasDisableAnalogOutput = true;
                    break; }
                case 40: {
                    disableDigitalOutput = in.readBool();
                    _hasDisableDigitalOutput = true;
                    break; }
                case 48: {
                    allowRecord = in.readBool();
                    _hasAllowRecord = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static OutputProtection fromBytes(byte[] in) throws EncodingException {
        OutputProtection message = new OutputProtection();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class VideoResolutionConstraint implements Message {
      
  
  
    protected int minResolutionPixels; // 1
    protected boolean _hasMinResolutionPixels;
    protected int maxResolutionPixels; // 2
    protected boolean _hasMaxResolutionPixels;
    protected License.KeyContainer.OutputProtection requiredProtection; // 3
    
    public int getMinResolutionPixels() {
        return minResolutionPixels;
    }
    
    public void setMinResolutionPixels(int minResolutionPixels) {
        this.minResolutionPixels = minResolutionPixels;
        this._hasMinResolutionPixels = true;
    }
    
    public void clearMinResolutionPixels() {
        _hasMinResolutionPixels = false;
    }
    
    public boolean hasMinResolutionPixels() {
        return _hasMinResolutionPixels;
    }
    public int getMaxResolutionPixels() {
        return maxResolutionPixels;
    }
    
    public void setMaxResolutionPixels(int maxResolutionPixels) {
        this.maxResolutionPixels = maxResolutionPixels;
        this._hasMaxResolutionPixels = true;
    }
    
    public void clearMaxResolutionPixels() {
        _hasMaxResolutionPixels = false;
    }
    
    public boolean hasMaxResolutionPixels() {
        return _hasMaxResolutionPixels;
    }
    public License.KeyContainer.OutputProtection getRequiredProtection() {
        return requiredProtection;
    }
    
    public void setRequiredProtection(License.KeyContainer.OutputProtection requiredProtection) {
        this.requiredProtection = requiredProtection;
    }
    
    public void clearRequiredProtection() {
        requiredProtection = null;
    }
    
    public boolean hasRequiredProtection() {
        return requiredProtection != null;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasMinResolutionPixels)
            out.writeUInt32(1, minResolutionPixels);
        
        if(_hasMaxResolutionPixels)
            out.writeUInt32(2, maxResolutionPixels);
        
        out.writeMessage(3, requiredProtection);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    minResolutionPixels = in.readUInt32();
                    _hasMinResolutionPixels = true;
                    break; }
                case 16: {
                    maxResolutionPixels = in.readUInt32();
                    _hasMaxResolutionPixels = true;
                    break; }
                case 26: {
                    requiredProtection = new License.KeyContainer.OutputProtection();
                    in.readMessage(requiredProtection);
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static VideoResolutionConstraint fromBytes(byte[] in) throws EncodingException {
        VideoResolutionConstraint message = new VideoResolutionConstraint();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class OperatorSessionKeyPermissions implements Message {
      
  
  
    protected boolean allowEncrypt; // 1
    protected boolean _hasAllowEncrypt;
    protected boolean allowDecrypt; // 2
    protected boolean _hasAllowDecrypt;
    protected boolean allowSign; // 3
    protected boolean _hasAllowSign;
    protected boolean allowSignatureVerify; // 4
    protected boolean _hasAllowSignatureVerify;
    
    public boolean getAllowEncrypt() {
        return allowEncrypt;
    }
    
    public void setAllowEncrypt(boolean allowEncrypt) {
        this.allowEncrypt = allowEncrypt;
        this._hasAllowEncrypt = true;
    }
    
    public void clearAllowEncrypt() {
        _hasAllowEncrypt = false;
    }
    
    public boolean hasAllowEncrypt() {
        return _hasAllowEncrypt;
    }
    public boolean getAllowDecrypt() {
        return allowDecrypt;
    }
    
    public void setAllowDecrypt(boolean allowDecrypt) {
        this.allowDecrypt = allowDecrypt;
        this._hasAllowDecrypt = true;
    }
    
    public void clearAllowDecrypt() {
        _hasAllowDecrypt = false;
    }
    
    public boolean hasAllowDecrypt() {
        return _hasAllowDecrypt;
    }
    public boolean getAllowSign() {
        return allowSign;
    }
    
    public void setAllowSign(boolean allowSign) {
        this.allowSign = allowSign;
        this._hasAllowSign = true;
    }
    
    public void clearAllowSign() {
        _hasAllowSign = false;
    }
    
    public boolean hasAllowSign() {
        return _hasAllowSign;
    }
    public boolean getAllowSignatureVerify() {
        return allowSignatureVerify;
    }
    
    public void setAllowSignatureVerify(boolean allowSignatureVerify) {
        this.allowSignatureVerify = allowSignatureVerify;
        this._hasAllowSignatureVerify = true;
    }
    
    public void clearAllowSignatureVerify() {
        _hasAllowSignatureVerify = false;
    }
    
    public boolean hasAllowSignatureVerify() {
        return _hasAllowSignatureVerify;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasAllowEncrypt)
            out.writeBool(1, allowEncrypt);
        
        if(_hasAllowDecrypt)
            out.writeBool(2, allowDecrypt);
        
        if(_hasAllowSign)
            out.writeBool(3, allowSign);
        
        if(_hasAllowSignatureVerify)
            out.writeBool(4, allowSignatureVerify);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    allowEncrypt = in.readBool();
                    _hasAllowEncrypt = true;
                    break; }
                case 16: {
                    allowDecrypt = in.readBool();
                    _hasAllowDecrypt = true;
                    break; }
                case 24: {
                    allowSign = in.readBool();
                    _hasAllowSign = true;
                    break; }
                case 32: {
                    allowSignatureVerify = in.readBool();
                    _hasAllowSignatureVerify = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static OperatorSessionKeyPermissions fromBytes(byte[] in) throws EncodingException {
        OperatorSessionKeyPermissions message = new OperatorSessionKeyPermissions();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
    public static class KeyCategorySpec implements Message {
      
  
    // KeyCategory
    public static final int SINGLE_CONTENT_KEY_DEFAULT = 0;
    public static final int GROUP_KEY = 1;
  
    protected int keyCategory; // 1
    protected boolean _hasKeyCategory;
    protected byte[] contentId; // 2
    protected boolean _hasContentId;
    protected byte[] groupId; // 3
    protected boolean _hasGroupId;
    
    public int getKeyCategory() {
        return keyCategory;
    }
    
    public void setKeyCategory(int keyCategory) {
        this.keyCategory = keyCategory;
        this._hasKeyCategory = true;
    }
    
    public void clearKeyCategory() {
        _hasKeyCategory = false;
    }
    
    public boolean hasKeyCategory() {
        return _hasKeyCategory;
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
    public byte[] getGroupId() {
        return groupId;
    }
    
    public void setGroupId(byte[] groupId) {
        this.groupId = groupId;
        this._hasGroupId = true;
    }
    
    public void clearGroupId() {
        _hasGroupId = false;
    }
    
    public boolean hasGroupId() {
        return _hasGroupId;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasKeyCategory)
            out.writeInt32(1, keyCategory);
        
        if(_hasContentId)
            out.writeBytes(2, contentId);
        
        if(_hasGroupId)
            out.writeBytes(3, groupId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 8: {
                    keyCategory = in.readInt32();
                    _hasKeyCategory = true;
                    break; }
                case 18: {
                    contentId = in.readBytes();
                    _hasContentId = true;
                    break; }
                case 26: {
                    groupId = in.readBytes();
                    _hasGroupId = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static KeyCategorySpec fromBytes(byte[] in) throws EncodingException {
        KeyCategorySpec message = new KeyCategorySpec();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
    // KeyType
    public static final int SIGNING = 1;
    public static final int CONTENT = 2;
    public static final int KEY_CONTROL = 3;
    public static final int OPERATOR_SESSION = 4;
    public static final int ENTITLEMENT = 5;
    public static final int OEM_CONTENT = 6;
    public static final int PROVIDER_ECM_VERIFIER_PUBLIC_KEY = 7;
    public static final int OEM_ENTITLEMENT = 8;
    // SecurityLevel
    public static final int SW_SECURE_CRYPTO = 1;
    public static final int SW_SECURE_DECODE = 2;
    public static final int HW_SECURE_CRYPTO = 3;
    public static final int HW_SECURE_DECODE = 4;
    public static final int HW_SECURE_ALL = 5;
    // EncryptionScheme
    public static final int ENCRYPTION_SCHEME_UNSPECIFIED = 0;
    public static final int AES128_CTR = 1;
    public static final int AES128_CBC = 2;
  
    protected byte[] id; // 1
    protected boolean _hasId;
    protected byte[] iv; // 2
    protected boolean _hasIv;
    protected byte[] key; // 3
    protected boolean _hasKey;
    protected int type; // 4
    protected boolean _hasType;
    protected int level; // 5
    protected boolean _hasLevel;
    protected License.KeyContainer.OutputProtection requiredProtection; // 6
    protected License.KeyContainer.OutputProtection requestedProtection; // 7
    protected License.KeyContainer.KeyControl keyControl; // 8
    protected License.KeyContainer.OperatorSessionKeyPermissions operatorSessionKeyPermissions; // 9
    protected Vector videoResolutionConstraints = new Vector(); // 10
    protected boolean antiRollbackUsageTable; // 11
    protected boolean _hasAntiRollbackUsageTable;
    protected String trackLabel; // 12
    protected boolean _hasTrackLabel;
    protected License.KeyContainer.KeyCategorySpec keyCategorySpec; // 13
    protected int encryptionScheme; // 14
    protected boolean _hasEncryptionScheme;
    
    public byte[] getId() {
        return id;
    }
    
    public void setId(byte[] id) {
        this.id = id;
        this._hasId = true;
    }
    
    public void clearId() {
        _hasId = false;
    }
    
    public boolean hasId() {
        return _hasId;
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
    public int getLevel() {
        return level;
    }
    
    public void setLevel(int level) {
        this.level = level;
        this._hasLevel = true;
    }
    
    public void clearLevel() {
        _hasLevel = false;
    }
    
    public boolean hasLevel() {
        return _hasLevel;
    }
    public License.KeyContainer.OutputProtection getRequiredProtection() {
        return requiredProtection;
    }
    
    public void setRequiredProtection(License.KeyContainer.OutputProtection requiredProtection) {
        this.requiredProtection = requiredProtection;
    }
    
    public void clearRequiredProtection() {
        requiredProtection = null;
    }
    
    public boolean hasRequiredProtection() {
        return requiredProtection != null;
    }
    public License.KeyContainer.OutputProtection getRequestedProtection() {
        return requestedProtection;
    }
    
    public void setRequestedProtection(License.KeyContainer.OutputProtection requestedProtection) {
        this.requestedProtection = requestedProtection;
    }
    
    public void clearRequestedProtection() {
        requestedProtection = null;
    }
    
    public boolean hasRequestedProtection() {
        return requestedProtection != null;
    }
    public License.KeyContainer.KeyControl getKeyControl() {
        return keyControl;
    }
    
    public void setKeyControl(License.KeyContainer.KeyControl keyControl) {
        this.keyControl = keyControl;
    }
    
    public void clearKeyControl() {
        keyControl = null;
    }
    
    public boolean hasKeyControl() {
        return keyControl != null;
    }
    public License.KeyContainer.OperatorSessionKeyPermissions getOperatorSessionKeyPermissions() {
        return operatorSessionKeyPermissions;
    }
    
    public void setOperatorSessionKeyPermissions(License.KeyContainer.OperatorSessionKeyPermissions operatorSessionKeyPermissions) {
        this.operatorSessionKeyPermissions = operatorSessionKeyPermissions;
    }
    
    public void clearOperatorSessionKeyPermissions() {
        operatorSessionKeyPermissions = null;
    }
    
    public boolean hasOperatorSessionKeyPermissions() {
        return operatorSessionKeyPermissions != null;
    }
    public void addVideoResolutionConstraints(License.KeyContainer.VideoResolutionConstraint value) {
        this.videoResolutionConstraints.addElement(value);
    }

    public int getVideoResolutionConstraintsCount() {
        return this.videoResolutionConstraints.size();
    }

    public License.KeyContainer.VideoResolutionConstraint getVideoResolutionConstraints(int index) {
        return (License.KeyContainer.VideoResolutionConstraint)this.videoResolutionConstraints.elementAt(index);
    }

    public Vector getVideoResolutionConstraintsVector() {
        return this.videoResolutionConstraints;
    }

    public void setVideoResolutionConstraintsVector(Vector value) {
        this.videoResolutionConstraints = value;
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
    public String getTrackLabel() {
        return trackLabel;
    }
    
    public void setTrackLabel(String trackLabel) {
        this.trackLabel = trackLabel;
        this._hasTrackLabel = true;
    }
    
    public void clearTrackLabel() {
        _hasTrackLabel = false;
    }
    
    public boolean hasTrackLabel() {
        return _hasTrackLabel;
    }
    public License.KeyContainer.KeyCategorySpec getKeyCategorySpec() {
        return keyCategorySpec;
    }
    
    public void setKeyCategorySpec(License.KeyContainer.KeyCategorySpec keyCategorySpec) {
        this.keyCategorySpec = keyCategorySpec;
    }
    
    public void clearKeyCategorySpec() {
        keyCategorySpec = null;
    }
    
    public boolean hasKeyCategorySpec() {
        return keyCategorySpec != null;
    }
    public int getEncryptionScheme() {
        return encryptionScheme;
    }
    
    public void setEncryptionScheme(int encryptionScheme) {
        this.encryptionScheme = encryptionScheme;
        this._hasEncryptionScheme = true;
    }
    
    public void clearEncryptionScheme() {
        _hasEncryptionScheme = false;
    }
    
    public boolean hasEncryptionScheme() {
        return _hasEncryptionScheme;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        if(_hasId)
            out.writeBytes(1, id);
        
        if(_hasIv)
            out.writeBytes(2, iv);
        
        if(_hasKey)
            out.writeBytes(3, key);
        
        if(_hasType)
            out.writeInt32(4, type);
        
        if(_hasLevel)
            out.writeInt32(5, level);
        
        out.writeMessage(6, requiredProtection);
        
        out.writeMessage(7, requestedProtection);
        
        out.writeMessage(8, keyControl);
        
        out.writeMessage(9, operatorSessionKeyPermissions);
        
        for(int i = 0; i < getVideoResolutionConstraintsCount(); i++) {
            out.writeMessage(10, getVideoResolutionConstraints(i));
        }
        
        if(_hasAntiRollbackUsageTable)
            out.writeBool(11, antiRollbackUsageTable);
        
        if(_hasTrackLabel)
            out.writeString(12, trackLabel);
        
        out.writeMessage(13, keyCategorySpec);
        
        if(_hasEncryptionScheme)
            out.writeInt32(14, encryptionScheme);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    id = in.readBytes();
                    _hasId = true;
                    break; }
                case 18: {
                    iv = in.readBytes();
                    _hasIv = true;
                    break; }
                case 26: {
                    key = in.readBytes();
                    _hasKey = true;
                    break; }
                case 32: {
                    type = in.readInt32();
                    _hasType = true;
                    break; }
                case 40: {
                    level = in.readInt32();
                    _hasLevel = true;
                    break; }
                case 50: {
                    requiredProtection = new License.KeyContainer.OutputProtection();
                    in.readMessage(requiredProtection);
                    break; }
                case 58: {
                    requestedProtection = new License.KeyContainer.OutputProtection();
                    in.readMessage(requestedProtection);
                    break; }
                case 66: {
                    keyControl = new License.KeyContainer.KeyControl();
                    in.readMessage(keyControl);
                    break; }
                case 74: {
                    operatorSessionKeyPermissions = new License.KeyContainer.OperatorSessionKeyPermissions();
                    in.readMessage(operatorSessionKeyPermissions);
                    break; }
                case 82: {
                    License.KeyContainer.VideoResolutionConstraint message = new License.KeyContainer.VideoResolutionConstraint();
                    in.readMessage(message);
                    addVideoResolutionConstraints(message);
                    break; }
                case 88: {
                    antiRollbackUsageTable = in.readBool();
                    _hasAntiRollbackUsageTable = true;
                    break; }
                case 98: {
                    trackLabel = in.readString();
                    _hasTrackLabel = true;
                    break; }
                case 106: {
                    keyCategorySpec = new License.KeyContainer.KeyCategorySpec();
                    in.readMessage(keyCategorySpec);
                    break; }
                case 112: {
                    encryptionScheme = in.readInt32();
                    _hasEncryptionScheme = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static KeyContainer fromBytes(byte[] in) throws EncodingException {
        KeyContainer message = new KeyContainer();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

    }
  
  
    protected LicenseIdentification id; // 1
    protected License.Policy policy; // 2
    protected Vector key = new Vector(); // 3
    protected long licenseStartTime; // 4
    protected boolean _hasLicenseStartTime;
    protected boolean remoteAttestationVerified; // 5
    protected boolean _hasRemoteAttestationVerified;
    protected byte[] providerClientToken; // 6
    protected boolean _hasProviderClientToken;
    protected int protectionScheme; // 7
    protected boolean _hasProtectionScheme;
    protected byte[] srmRequirement; // 8
    protected boolean _hasSrmRequirement;
    protected byte[] srmUpdate; // 9
    protected boolean _hasSrmUpdate;
    protected int platformVerificationStatus; // 10
    protected boolean _hasPlatformVerificationStatus;
    protected Vector groupIds = new Vector(); // 11
    protected LicenseCategorySpec licenseCategorySpec; // 12
    protected int providerKeyId; // 13
    protected boolean _hasProviderKeyId;
    
    public LicenseIdentification getId() {
        return id;
    }
    
    public void setId(LicenseIdentification id) {
        this.id = id;
    }
    
    public void clearId() {
        id = null;
    }
    
    public boolean hasId() {
        return id != null;
    }
    public License.Policy getPolicy() {
        return policy;
    }
    
    public void setPolicy(License.Policy policy) {
        this.policy = policy;
    }
    
    public void clearPolicy() {
        policy = null;
    }
    
    public boolean hasPolicy() {
        return policy != null;
    }
    public void addKey(License.KeyContainer value) {
        this.key.addElement(value);
    }

    public int getKeyCount() {
        return this.key.size();
    }

    public License.KeyContainer getKey(int index) {
        return (License.KeyContainer)this.key.elementAt(index);
    }

    public Vector getKeyVector() {
        return this.key;
    }

    public void setKeyVector(Vector value) {
        this.key = value;
    }
    public long getLicenseStartTime() {
        return licenseStartTime;
    }
    
    public void setLicenseStartTime(long licenseStartTime) {
        this.licenseStartTime = licenseStartTime;
        this._hasLicenseStartTime = true;
    }
    
    public void clearLicenseStartTime() {
        _hasLicenseStartTime = false;
    }
    
    public boolean hasLicenseStartTime() {
        return _hasLicenseStartTime;
    }
    public boolean getRemoteAttestationVerified() {
        return remoteAttestationVerified;
    }
    
    public void setRemoteAttestationVerified(boolean remoteAttestationVerified) {
        this.remoteAttestationVerified = remoteAttestationVerified;
        this._hasRemoteAttestationVerified = true;
    }
    
    public void clearRemoteAttestationVerified() {
        _hasRemoteAttestationVerified = false;
    }
    
    public boolean hasRemoteAttestationVerified() {
        return _hasRemoteAttestationVerified;
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
    public byte[] getSrmRequirement() {
        return srmRequirement;
    }
    
    public void setSrmRequirement(byte[] srmRequirement) {
        this.srmRequirement = srmRequirement;
        this._hasSrmRequirement = true;
    }
    
    public void clearSrmRequirement() {
        _hasSrmRequirement = false;
    }
    
    public boolean hasSrmRequirement() {
        return _hasSrmRequirement;
    }
    public byte[] getSrmUpdate() {
        return srmUpdate;
    }
    
    public void setSrmUpdate(byte[] srmUpdate) {
        this.srmUpdate = srmUpdate;
        this._hasSrmUpdate = true;
    }
    
    public void clearSrmUpdate() {
        _hasSrmUpdate = false;
    }
    
    public boolean hasSrmUpdate() {
        return _hasSrmUpdate;
    }
    public int getPlatformVerificationStatus() {
        return platformVerificationStatus;
    }
    
    public void setPlatformVerificationStatus(int platformVerificationStatus) {
        this.platformVerificationStatus = platformVerificationStatus;
        this._hasPlatformVerificationStatus = true;
    }
    
    public void clearPlatformVerificationStatus() {
        _hasPlatformVerificationStatus = false;
    }
    
    public boolean hasPlatformVerificationStatus() {
        return _hasPlatformVerificationStatus;
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
    public LicenseCategorySpec getLicenseCategorySpec() {
        return licenseCategorySpec;
    }
    
    public void setLicenseCategorySpec(LicenseCategorySpec licenseCategorySpec) {
        this.licenseCategorySpec = licenseCategorySpec;
    }
    
    public void clearLicenseCategorySpec() {
        licenseCategorySpec = null;
    }
    
    public boolean hasLicenseCategorySpec() {
        return licenseCategorySpec != null;
    }
    public int getProviderKeyId() {
        return providerKeyId;
    }
    
    public void setProviderKeyId(int providerKeyId) {
        this.providerKeyId = providerKeyId;
        this._hasProviderKeyId = true;
    }
    
    public void clearProviderKeyId() {
        _hasProviderKeyId = false;
    }
    
    public boolean hasProviderKeyId() {
        return _hasProviderKeyId;
    }
    
    public final void serialize(CodedOutputStream out) throws IOException {
        out.writeMessage(1, id);
        
        out.writeMessage(2, policy);
        
        for(int i = 0; i < getKeyCount(); i++) {
            out.writeMessage(3, getKey(i));
        }
        
        if(_hasLicenseStartTime)
            out.writeInt64(4, licenseStartTime);
        
        if(_hasRemoteAttestationVerified)
            out.writeBool(5, remoteAttestationVerified);
        
        if(_hasProviderClientToken)
            out.writeBytes(6, providerClientToken);
        
        if(_hasProtectionScheme)
            out.writeUInt32(7, protectionScheme);
        
        if(_hasSrmRequirement)
            out.writeBytes(8, srmRequirement);
        
        if(_hasSrmUpdate)
            out.writeBytes(9, srmUpdate);
        
        if(_hasPlatformVerificationStatus)
            out.writeInt32(10, platformVerificationStatus);
        
        for(int i = 0; i < getGroupIdsCount(); i++) {
            out.writeBytes(11, getGroupIds(i));
        }
        
        out.writeMessage(12, licenseCategorySpec);
        
        if(_hasProviderKeyId)
            out.writeUInt32(13, providerKeyId);
        
    }

    public final void deserialize(CodedInputStream in) throws IOException {
        while(true) {
            int tag = in.readTag();
            switch(tag) {
                case 0:
                    return;
                case 10: {
                    id = new LicenseIdentification();
                    in.readMessage(id);
                    break; }
                case 18: {
                    policy = new License.Policy();
                    in.readMessage(policy);
                    break; }
                case 26: {
                    License.KeyContainer message = new License.KeyContainer();
                    in.readMessage(message);
                    addKey(message);
                    break; }
                case 32: {
                    licenseStartTime = in.readInt64();
                    _hasLicenseStartTime = true;
                    break; }
                case 40: {
                    remoteAttestationVerified = in.readBool();
                    _hasRemoteAttestationVerified = true;
                    break; }
                case 50: {
                    providerClientToken = in.readBytes();
                    _hasProviderClientToken = true;
                    break; }
                case 56: {
                    protectionScheme = in.readUInt32();
                    _hasProtectionScheme = true;
                    break; }
                case 66: {
                    srmRequirement = in.readBytes();
                    _hasSrmRequirement = true;
                    break; }
                case 74: {
                    srmUpdate = in.readBytes();
                    _hasSrmUpdate = true;
                    break; }
                case 80: {
                    platformVerificationStatus = in.readInt32();
                    _hasPlatformVerificationStatus = true;
                    break; }
                case 90: {
                    addGroupIds(in.readBytes());
                    break; }
                case 98: {
                    licenseCategorySpec = new LicenseCategorySpec();
                    in.readMessage(licenseCategorySpec);
                    break; }
                case 104: {
                    providerKeyId = in.readUInt32();
                    _hasProviderKeyId = true;
                    break; }
                default:
                    in.skipTag(tag);
            }
        }
    }
    
    public static License fromBytes(byte[] in) throws EncodingException {
        License message = new License();
        ProtoUtil.messageFromBytes(in, message);
        return message;
    }
    
    public byte[] toBytes() throws EncodingException {
        return ProtoUtil.messageToBytes(this);
    }
    

}



