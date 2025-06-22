package dev.iseal.sealUtils.utils;

import dev.iseal.sealUtils.Interfaces.Dumpable;
import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.SealUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.logging.Level;

public class ExceptionHandler {

    private static ExceptionHandler instance;
    private final SealLogger log = SealUtils.getLogger();
    private ArrayList<String> currentLog = new ArrayList<>();

    // class, instance
    private final HashMap<Class<? extends Dumpable>, Dumpable> registeredClasses = new HashMap<>();

    public static ExceptionHandler getInstance() {
        if (instance == null)
            instance = new ExceptionHandler();
        return instance;
    }

    public void dealWithException(Exception ex, Level logLevel, String errorMessage, Object... moreInfo) {
        dealWithExceptionExtended(
                ex, logLevel, errorMessage,
                false,
                // execute here instead of passing it since it would return ExceptionHandler.class
                Optional.of(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()),
                Arrays.stream(moreInfo).map(Object::toString).toArray(String[]::new)
        );
    }

    public Optional<ArrayList<String>> dealWithExceptionExtended(Exception ex, Level logLevel, String errorMessage, boolean returnLog, Optional<Class<?>> callerClassOpt, String... moreInfo){
        currentLog = new ArrayList<>();
        Class<?> callerClass = callerClassOpt.orElse(
                StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).getCallerClass()
        );

        currentLog.add("Exception triggered by "+callerClass.getName());
        currentLog.add("The exception message is "+ex.getMessage());
        currentLog.add("The error message is "+errorMessage);
        currentLog.add("The stacktrace and all of its details known are as follows: ");
        for (StackTraceElement stackTraceElement : ex.getStackTrace())
            currentLog.add(stackTraceElement.toString());

        currentLog.add("More details (make sure to tell these to the developer): ");
        int i = 1;
        for (Object obj : moreInfo) {
            currentLog.add("More info "+i+": "+obj.toString());
            i++;
        }

        attemptToDealWithCustomException(ex);

        if (SealUtils.isDebug())
            currentLog.addAll(dumpAllClasses(false).orElse(new ArrayList<>()));
        currentLog.forEach((str) -> log.log(logLevel, str));
        return returnLog ? Optional.of(currentLog) : Optional.empty();
    }
    
    public Optional<ArrayList<String>> dumpAllClasses(boolean printToConsole) {
        ArrayList<String> dumpLog = new ArrayList<>();
        HashMap<String, HashMap<String, Object>> dumpMap = new HashMap<>();
        registeredClasses.forEach((clazz, dumpable) -> {
            dumpMap.put(clazz.getSimpleName(), dumpable.dump());
        });

        dumpMap.forEach((className, dumpMapTemp) -> {
            dumpMapTemp.forEach((toDump, dumpValue) -> {
                if (dumpValue == null)
                    dumpLog.add("Dump from: "+className+" -> "+toDump+": null - something is wrong.");
                else
                    dumpLog.add("Dump from: "+className+" -> "+toDump+": "+dumpValue.toString());
            });
        });
        if (printToConsole) {
            dumpLog.forEach(log::info);
        }
        
        return printToConsole ? Optional.empty() : Optional.of(dumpLog);
    }

    private void attemptToDealWithCustomException(Exception ex) {
        if (ex instanceof SecurityException se) {
            currentLog.add("[SealUtils] SecurityException caught, what?");
        }
    }

    public void registerClass(Class<? extends Dumpable> clazz, Dumpable instance) {
        registeredClasses.put(clazz, instance);
    }
}
