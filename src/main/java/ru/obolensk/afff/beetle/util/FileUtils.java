package ru.obolensk.afff.beetle.util;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.nio.file.Files.readAllLines;

/**
 * Created by Afff on 21.04.2017.
 */
public class FileUtils {

    public static boolean exists(@Nullable final Path path) {
        return path != null && Files.exists(path);
    }

}
