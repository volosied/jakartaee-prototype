package com.ibm.ws.jakarta.transformer.action;

public interface JavaChanges extends Changes {
    int getReplacements();
    void addReplacement();
    void addReplacements(int additions);
}
