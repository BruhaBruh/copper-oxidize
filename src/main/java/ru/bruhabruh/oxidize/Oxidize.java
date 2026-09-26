package ru.bruhabruh.oxidize;

import org.bukkit.plugin.java.JavaPlugin;
import ru.bruhabruh.oxidize.listener.OxidizeListener;

public final class Oxidize extends JavaPlugin {
    private final OxidizeListener oxidizeListener = new OxidizeListener();

    @Override
    public void onEnable() {
        getServer()
                .getPluginManager()
                .registerEvents(oxidizeListener, this);
    }
}
