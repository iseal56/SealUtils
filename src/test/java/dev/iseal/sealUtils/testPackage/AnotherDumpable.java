package dev.iseal.sealUtils.testPackage;

import dev.iseal.sealUtils.Interfaces.Dumpable;

import java.util.HashMap;

public class AnotherDumpable implements Dumpable {

    public AnotherDumpable() {
        dumpableInit();
    }

    @Override
    public HashMap<String, Object> dump() {
        HashMap<String, Object> dump = new HashMap<>();
        dump.put("name", "AnotherDumpable");
        dump.put("value", 84);
        dump.put("hash", this.hashCode());
        return dump;
    }
}
