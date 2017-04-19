package ru.obolensk.afff.beetle;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by Afff on 10.04.2017.
 */
public class Storage {

    //TODO add settings to specify the www root
    //TODO add virtual hosts support
    //FIXME !!!prevent top level attacks for server

    public static final Path ROOT = Paths.get("/");
    public static final String WELCOME_FILE_NAME = "index.html";

    private static final Path ROOT_DIR = Paths.get("D:/Beetle/www"); //FIXME remove this shit

    @Nonnull
    public static Path getFilePath(@Nonnull final Path path) {
        return ROOT.equals(path) ? ROOT_DIR.resolve(WELCOME_FILE_NAME) : ROOT_DIR.resolve(path.subpath(0, path.getNameCount())); //TODO add welcome files list
    }
}
