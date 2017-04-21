package ru.obolensk.afff.beetle.util;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by Afff on 21.04.2017.
 */
public class FileUtils {

    public static boolean exists(@Nullable final Path path) {
        return path != null && Files.exists(path);
    }
}
