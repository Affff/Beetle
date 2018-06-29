package ru.obolensk.afff.beetle.util;

import java.nio.file.Files;
import java.nio.file.Path;

import javax.annotation.Nullable;

/**
 * Created by Afff on 21.04.2017.
 */
public class FileUtils {

    public static boolean exists(@Nullable final Path path) {
        return path != null && Files.exists(path);
    }

}
