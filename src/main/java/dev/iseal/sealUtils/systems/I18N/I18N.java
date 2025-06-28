package dev.iseal.sealUtils.systems.I18N;

import dev.iseal.sealUtils.Interfaces.Dumpable;
import dev.iseal.sealUtils.Interfaces.SealLogger;
import dev.iseal.sealUtils.SealUtils;

import java.io.*;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

import static dev.iseal.sealUtils.SealUtils.isDebug;

public class I18N implements Dumpable {

    private static I18N instance;
    public static I18N getInstance() {
        if (instance == null) {
            instance = new I18N();
        }
        return instance;
    }

    public I18N() {
        dumpableInit();
    }

    private static final HashMap<String, ResourceBundle> selectedBundles = new HashMap<>();

    public void unpackAllLanguages(Class<?> fromClass, File extractToFolder) throws URISyntaxException {
        SealLogger logger = SealUtils.getLogger();
        ResourceWalker.getInstance().walk(fromClass, "languages", (inputStream, fileName) -> {
            File targetFile = new File(extractToFolder, "languages/" + fileName);
            if (isDebug())
                logger.info("[SealUtils] Processing language file: " + fileName);

            if (targetFile.exists()) {
                if (isDebug())
                    logger.info("[SealUtils] Target file exists, checking for updates");
                PropertyResourceBundle oldResourceBundle;
                try (InputStreamReader reader = new InputStreamReader(new FileInputStream(targetFile), StandardCharsets.UTF_8)) {
                    oldResourceBundle = new PropertyResourceBundle(reader);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                PropertyResourceBundle newResourceBundle;
                try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
                    newResourceBundle = new PropertyResourceBundle(reader);
                } catch (Exception e) {
                    logger.error("[SealUtils] Exception while loading resource file: " + e.getMessage());
                    throw new RuntimeException(e);
                }

                checkAndApplyUpdates(newResourceBundle, oldResourceBundle, targetFile);
            } else {
                if (isDebug())
                    logger.info("[SealUtils] Target file for "+fileName+" does not exist, creating directories and new file");
                try {
                    targetFile.getParentFile().mkdirs();
                    targetFile.createNewFile();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                if (isDebug())
                    logger.info("[SealUtils] Copying file from resources to data folder");
                try {
                    Files.copy(inputStream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                if (isDebug())
                    logger.info("[SealUtils] File copied successfully");
            }
        });
    }

    public void setBundle(Class<?> fromClass, File extractToFolder, String localeLang, String localeCountry) throws IOException {
        SealLogger logger = SealUtils.getLogger();
        PropertyResourceBundle resourceBundle = null;

        // unpack all languages and check updates
        try {
            unpackAllLanguages(fromClass, extractToFolder);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // get caller class
        String packageKey = fromClass.getPackageName().split("\\.")[2];
        String fileName = "Messages_" + localeLang + "_" + localeCountry + ".properties";
        if (isDebug())
            logger.info("[SealUtils] File name constructed: " + fileName);

        // make file object
        File targetFile = new File(extractToFolder, "languages/" + fileName);

        if (!targetFile.exists()) {
            logger.error("[SealUtils] Target file does not exist, loading default en_US file");
            fileName = "Messages_en_US.properties";
            targetFile = new File(extractToFolder, "languages/" + fileName);
        }

        logger.info("[SealUtils] Target file path: " + targetFile.getAbsolutePath());

        resourceBundle = new PropertyResourceBundle(new FileInputStream(targetFile));
        logger.info("[SealUtils] Loaded language file: " + fileName + " v" + resourceBundle.getString("BUNDLE_VERSION"));
        selectedBundles.put(packageKey, resourceBundle);
    }

    private void checkAndApplyUpdates(PropertyResourceBundle newResourceBundle, PropertyResourceBundle oldResourceBundle, File targetFile) {
        try {
            int newVer = Integer.parseInt(newResourceBundle.getString("BUNDLE_VERSION"));
            int oldVer = Integer.parseInt(oldResourceBundle.getString("BUNDLE_VERSION"));
            if (newVer > oldVer) {
                // update bundle
                try (OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(targetFile), StandardCharsets.UTF_8)) {
                    newResourceBundle.keySet().forEach(key -> {
                        try {
                            writer.write(key + "=" + newResourceBundle.getString(key) + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTranslation(String key, String... args) {
        try {
            String translation = selectedBundles.get(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .getCallerClass().getPackageName().split("\\.")[2])
                    .getString(key);

            for (int i = 0; i < args.length; i++) {
                translation = translation.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String getTranslation(String key) {
        try {
            String translation = selectedBundles.get(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .getCallerClass().getPackageName().split("\\.")[2])
                    .getString(key);

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String translate(String key) {
        try {
            String translation = selectedBundles.get(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .getCallerClass().getPackageName().split("\\.")[2])
                    .getString(key);

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String translate(String key, String... args) {
        try {
            String translation = selectedBundles.get(StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                            .getCallerClass().getPackageName().split("\\.")[2])
                    .getString(key);

            for (int i = 0; i < args.length; i++) {
                translation = translation.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    // same but with class input
    public static String getTranslation(Class<?> clazz, String key, String... args) {
        try {
            String translation = selectedBundles.get(clazz.getPackageName().split("\\.")[2])
                    .getString(key);

            for (int i = 0; i < args.length; i++) {
                translation = translation.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String getTranslation(Class<?> clazz, String key) {
        try {
            String translation = selectedBundles.get(clazz.getPackageName().split("\\.")[2])
                    .getString(key);

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String translate(Class<?> clazz, String key) {
        try {
            String translation = selectedBundles.get(clazz.getPackageName().split("\\.")[2])
                    .getString(key);

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    public static String translate(Class<?> clazz, String key, String... args) {
        try {
            String translation = selectedBundles.get(clazz.getPackageName().split("\\.")[2])
                    .getString(key);

            for (int i = 0; i < args.length; i++) {
                translation = translation.replace("{" + i + "}", args[i] != null ? args[i] : "null");
            }

            return translation;
        } catch (MissingResourceException | NullPointerException e) {
            return key;
        }
    }

    @Override
    public HashMap<String, Object> dump() {
        HashMap<String, Object> dump = new HashMap<>();
        selectedBundles.forEach((key, value) -> {
            HashMap<String, String> bundle = new HashMap<>();
            Enumeration<String> keys = value.getKeys();
            while (keys.hasMoreElements()) {
                String k = keys.nextElement();
                bundle.put(k, value.getString(k));
            }
            dump.put(key, bundle);
        });
        return dump;
    }
}
