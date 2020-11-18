package com.appliedrec.credentials.app;

interface ISharedData {

    <T> void setSharedObject(String key, T object) throws Exception;

    <T> T getSharedObject(String key, Class<T> type) throws Exception;

    void setSharedData(String key, byte[] data) throws Exception;

    byte[] getSharedData(String key) throws Exception;
}
