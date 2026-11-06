package com.example;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainClient implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("template");

    @Override
    public void onInitialize() {
        LOGGER.info("Hello Fabric world!");
    }
}