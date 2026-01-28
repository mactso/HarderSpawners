package com.mactso.harderspawners.modloader.config;

import java.util.ArrayList;
import java.util.List;

import com.mojang.datafixers.util.Pair;

public class ModConfigProvider implements SimpleConfig.DefaultConfig {

    private String configContents = "";
    private final List<Pair<String,?>> configsList = new ArrayList<>();
    
    public List<Pair<String,?>> getConfigsList() {
        return configsList;
    }



    public void addKeyValuePair(Pair<String, ?> keyValuePair, String type, String possibleValues) {
        configsList.add(keyValuePair);
        configContents += keyValuePair.getFirst() + "=" + keyValuePair.getSecond() 
                + " # " + type + " | default: " + keyValuePair.getSecond()
                + " | possible values: " + possibleValues + "\n";
    }

    @Override
    public String get(String namespace) {
        return configContents;
    }
    
    /** MacTso ( 2026) Adds comment line to config file. Ignore during parsing. */
    public void addComment(String comment) {
        configContents += "# " + comment + "\n";
    }
}