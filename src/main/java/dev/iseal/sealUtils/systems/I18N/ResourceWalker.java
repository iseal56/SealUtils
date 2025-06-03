package dev.iseal.sealUtils.systems.I18N;

import dev.iseal.sealUtils.SealUtils;
import dev.iseal.sealUtils.utils.ExceptionHandler;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

public class ResourceWalker {

    private static ResourceWalker instance;
    public static ResourceWalker getInstance() {
        if (instance == null) {
            instance = new ResourceWalker();
        }
        return instance;
    }

    /**
     * Globbing pattern to match message lang files.
     */
    public static final Pattern GLOB_MESSAGES = Pattern.compile("Messages_[a-z]{2}_[A-Z]{2}\\.properties");

    private static final Logger log = SealUtils.getLogger();

    /**
     *
     * @param callingClass The class that is calling this method, used to find the resource directory.
     * @param directory The directory to walk through, relative to the classpath of the calling class.
     * @param c A consumer that takes an InputStream and the filename as parameters. It will be called for each file that matches the glob pattern.
     */
    public void walk(Class callingClass, String directory, BiConsumer<InputStream, String> c) {
        try {
            URL resource = callingClass.getClassLoader().getResource(directory);
            if (resource == null) {
                throw new IllegalArgumentException("Resource not found: " + directory);
            }

            URI uri = resource.toURI();
            Path path;
            if (uri.getScheme().equals("jar")) {
                try (FileSystem fs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                    path = fs.getPath(directory);
                    Files.walk(path)
                            .filter(Files::isRegularFile)
                            .filter(p -> GLOB_MESSAGES.matcher(p.getFileName().toString()).find())
                            .forEach(p -> {
                                try (InputStream is = Files.newInputStream(p)) {
                                    c.accept(is, p.getFileName().toString());
                                } catch (IOException e) {
                                    ExceptionHandler.getInstance().dealWithException(e, Level.WARNING, "RESOURCE_WALKER_FAILED", log);
                                }
                            });
                }
            } else {
                path = Paths.get(uri);
                Files.walk(path)
                        .filter(Files::isRegularFile)
                        .filter(p -> GLOB_MESSAGES.matcher(p.getFileName().toString()).find())
                        .forEach(p -> {
                            try (InputStream is = Files.newInputStream(p)) {
                                c.accept(is, p.getFileName().toString());
                            } catch (IOException e) {
                                ExceptionHandler.getInstance().dealWithException(e, Level.WARNING, "RESOURCE_WALKER_FAILED", log);
                            }
                        });
            }
        } catch (IOException | URISyntaxException e) {
            ExceptionHandler.getInstance().dealWithException(e, Level.WARNING, "RESOURCE_WALKER_FAILED", log);
        }
    }
}