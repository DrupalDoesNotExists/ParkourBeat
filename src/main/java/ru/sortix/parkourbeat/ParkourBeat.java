package ru.sortix.parkourbeat;

import lombok.Getter;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import ru.sortix.parkourbeat.commands.*;
import ru.sortix.parkourbeat.data.Settings;
import ru.sortix.parkourbeat.data.Songs;
import ru.sortix.parkourbeat.editor.LevelEditorListener;
import ru.sortix.parkourbeat.editor.LevelEditorsManager;
import ru.sortix.parkourbeat.editor.menu.SongMenuListener;
import ru.sortix.parkourbeat.game.GameManager;
import ru.sortix.parkourbeat.levels.LevelsManager;
import ru.sortix.parkourbeat.levels.WorldsManager;
import ru.sortix.parkourbeat.levels.dao.files.FileLevelSettingDAO;
import ru.sortix.parkourbeat.listeners.EventListener;
import ru.sortix.parkourbeat.listeners.MoveListener;
import ru.sortix.parkourbeat.listeners.ResourcePackListener;
import ru.sortix.parkourbeat.listeners.SprintListener;
import ru.sortix.parkourbeat.location.Waypoint;

public class ParkourBeat extends JavaPlugin {

    @Getter private static Songs songs;

    public static JavaPlugin getPlugin() {
        return JavaPlugin.getPlugin(ParkourBeat.class);
    }

    @Override
    public void onEnable() {
        WorldsManager worldsManager = new WorldsManager(this);
        Settings.load(this, worldsManager);
        songs = new Songs();

        ConfigurationSerialization.registerClass(Waypoint.class);
        FileLevelSettingDAO fileLevelSettingDAO = new FileLevelSettingDAO(this);
        LevelsManager levelsManager = new LevelsManager(this, worldsManager, fileLevelSettingDAO);
        GameManager gameManager = new GameManager(levelsManager);
        LevelEditorsManager levelEditorsManager = new LevelEditorsManager(gameManager, levelsManager);

        registerCommand(
                "tptoworld", new TpToWorldCommand(levelEditorsManager, gameManager, levelsManager));
        registerCommand("play", new PlayCommand(gameManager, levelsManager));
        registerCommand("edit", new EditCommand(levelEditorsManager, levelsManager, gameManager));
        registerCommand("create", new CreateCommand(levelEditorsManager, levelsManager, gameManager));
        registerCommand("delete", new DeleteCommand(levelEditorsManager, levelsManager, gameManager));
        registerCommand("song", new SongCommand(levelEditorsManager));
        registerCommand("color", new ColorCommand(levelEditorsManager));
        registerCommand("test", new TestCommand(levelsManager));

        PluginManager pluginManager = this.getServer().getPluginManager();
        pluginManager.registerEvents(
                new EventListener(gameManager, levelsManager, levelEditorsManager), this);
        pluginManager.registerEvents(new MoveListener(gameManager), this);
        pluginManager.registerEvents(new ResourcePackListener(gameManager), this);
        pluginManager.registerEvents(new SprintListener(gameManager), this);
        pluginManager.registerEvents(new LevelEditorListener(levelEditorsManager), this);
        pluginManager.registerEvents(new SongMenuListener(), this);
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            this.getLogger()
                    .severe("Unable to register command " + commandName + ". Is it specified in plugin.yml?");
            return;
        }
        command.setExecutor(executor);
        if (executor instanceof TabCompleter) {
            command.setTabCompleter((TabCompleter) executor);
        }
    }

    @Override
    public void onDisable() {}
}
