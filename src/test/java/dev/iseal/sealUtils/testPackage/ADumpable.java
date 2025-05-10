package dev.iseal.sealUtils.testPackage;

import dev.iseal.sealUtils.Interfaces.Dumpable;

import java.util.HashMap;

public class ADumpable implements Dumpable {

    public ADumpable() {
        dumpableInit();
    }

    @Override
    public HashMap<String, Object> dump() {
        HashMap<String, Object> dump = new HashMap<>();
        dump.put("name", "ADumpable");
        dump.put("value", 42);
        dump.put("hash", this.hashCode());
        return dump;
    }
}
