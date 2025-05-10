package dev.iseal.sealUtils.Interfaces;

import dev.iseal.sealUtils.utils.ExceptionHandler;

import java.util.HashMap;

public interface Dumpable {

    default void dumpableInit() {
        ExceptionHandler.getInstance().registerClass(this.getClass(), this);
    }

    HashMap<String, Object> dump();
}
