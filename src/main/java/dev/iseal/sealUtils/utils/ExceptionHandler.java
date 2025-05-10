package dev.iseal.sealUtils.utils;

import dev.iseal.sealUtils.Interfaces.Dumpable;
import dev.iseal.sealUtils.SealUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ExceptionHandler {

    private static ExceptionHandler instance;
    private Logger defaultLog = Logger.getLogger("SealUtils");
    private ArrayList<String> currentLog = new ArrayList<>();

    // class, instance
    private final HashMap<Class<? extends Dumpable>, Dumpable> registeredClasses = new HashMap<>();

    public static ExceptionHandler getInstance() {
        if (instance == null)
            instance = new ExceptionHandler();
        return instance;
    }

    public void dealWithException(Exception ex, Level logLevel, String errorMessage, Object... moreInfo) {
        dealWithExceptionExtended(ex, logLevel, errorMessage, defaultLog, false, Arrays.stream(moreInfo).map(Object::toString).toArray(String[]::new));
    }

    public Optional<ArrayList<String>> dealWithExceptionExtended(Exception ex, Level logLevel, String errorMessage, Logger log, boolean returnLog, String... moreInfo){
        currentLog = new ArrayList<>();
        Class<?> mainClass = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        currentLog.add( "[SealUtils] "+"Exception triggered by "+mainClass.getName());
        currentLog.add( "[SealUtils] "+"The exception message is "+ex.getMessage());
        currentLog.add( "[SealUtils] "+"The error message is "+errorMessage);
        currentLog.add("[SealUtils] "+"The stacktrace and all of its details known are as follows: ");
        for (StackTraceElement stackTraceElement : ex.getStackTrace())
            currentLog.add( "[SealUtils] "+stackTraceElement.toString());

        currentLog.add( "[SealUtils] "+"More details (make sure to tell these to the developer): ");
        int i = 1;
        for (Object obj : moreInfo) {
            currentLog.add( "[SealUtils] More info "+i+": "+obj.toString());
            i++;
        }

        attemptToDealWithCustomException(ex);

        if (SealUtils.isDebug())
            dumpAllClasses(mainClass);
        currentLog.forEach((str) -> log.log(logLevel, str));
        return returnLog ? Optional.of(currentLog) : Optional.empty();
    }

    public void dumpAllClasses(Class<?> caller) {
        if (caller == null) {
            caller = StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass();
        }

        HashMap<String, HashMap<String, Object>> dumpMap = new HashMap<>();
        registeredClasses.forEach((clazz, dumpable) -> {
            dumpMap.put(clazz.getSimpleName(), dumpable.dump());
        });

        dumpMap.forEach((className, dumpMapTemp) -> {
            dumpMapTemp.forEach((toDump, dumpValue) -> {
                if (dumpValue == null)
                    currentLog.add("[SealUtils] Dump from: "+className+" -> "+toDump+": null - something is wrong.");
                else
                    currentLog.add("[SealUtils] Dump from: "+className+" -> "+toDump+": "+dumpValue.toString());
            });
        });
    }

    private void attemptToDealWithCustomException(Exception ex) {
        if (ex instanceof SecurityException se) {
            currentLog.add("[SealUtils] SecurityException caught, what?");
        }
    }

    public void registerClass(Class<? extends Dumpable> clazz, Dumpable instance) {
        registeredClasses.put(clazz, instance);
    }

    public void setDefaultLog(Logger defaultLog) {
        this.defaultLog = defaultLog;
    }

}
