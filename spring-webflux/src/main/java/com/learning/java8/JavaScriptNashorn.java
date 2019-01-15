package com.learning.java8;

import lombok.extern.slf4j.Slf4j;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.FileNotFoundException;
import java.io.FileReader;

@Slf4j
public class JavaScriptNashorn {
    public static void main(String[] args) throws FileNotFoundException, ScriptException, NoSuchMethodException {
        ScriptEngineManager engineManager = new ScriptEngineManager();
        ScriptEngine engine = engineManager.getEngineByName("nashorn");
        engine.eval(new FileReader("src/main/resources/script.js"));
        Invocable inv = (Invocable) engine;

        log.debug("call js function: {}", inv.invokeFunction("helloJS", "Rodrigo"));
    }
}
