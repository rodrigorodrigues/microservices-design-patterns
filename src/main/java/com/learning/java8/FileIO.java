package com.learning.java8;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;

@Slf4j
public class FileIO {
    public static void main(String[] args) throws IOException {
        //Old way
        File file = new File("src/main/resources/names.txt");
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("old way - Line: {}", line);
            }
        }
        //Old way

        //New way
        log.debug("-------------");
        Files.lines(file.toPath())
                .forEach(l -> log.debug("new way - Line: {}", l));
        //New way
    }
}
