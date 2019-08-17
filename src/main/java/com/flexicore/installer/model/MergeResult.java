package com.flexicore.installer.model;

public class MergeResult {
    enum MergeStatus {
        ok,
        duplicateParameter
    }
    MergeStatus status;
    String mergeMessage;

    public MergeStatus getStatus() {
        return status;
    }

    public MergeResult setStatus(MergeStatus status) {
        this.status = status;
        return this;
    }

    public String getMergeMessage() {
        return mergeMessage;
    }

    public MergeResult setMergeMessage(String mergeMessage) {
        this.mergeMessage = mergeMessage;
        return this;
    }
}
