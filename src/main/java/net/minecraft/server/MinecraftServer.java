package net.minecraft.server;

import java.awt.GraphicsEnvironment;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

// CraftBukkit start
import java.io.PrintStream;
import java.net.UnknownHostException;
import jline.ConsoleReader;
import joptsimple.OptionSet;
import org.bukkit.World.Environment;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.RemoteConsoleCommandSender;
import org.bukkit.craftbukkit.command.CraftRemoteConsoleCommandSender;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.LoggerOutputStream;
import org.bukkit.craftbukkit.scheduler.CraftScheduler;
import org.bukkit.craftbukkit.util.ServerShutdownThread;
import org.bukkit.event.Event;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoadOrder;
// CraftBukkit end

public class MinecraftServer implements Runnable, ICommandListener, IMinecraftServer {

    public static Logger log = Logger.getLogger("Minecraft");
    public static HashMap trackerList = new HashMap();
    private String s;
    private int t;
    public NetworkListenThread networkListenThread;
    public PropertyManager propertyManager;
    // public WorldServer[] worldServer; // CraftBukkit - removed!
    public long[] f = new long[100];
    public long[][] g;
    public ServerConfigurationManager serverConfigurationManager;
    public ConsoleCommandHandler consoleCommandHandler; // CraftBukkit - made public
    private boolean isRunning = true;
    public boolean isStopped = false;
    int ticks = 0;
    public String k;
    public int l;
    private List w = new ArrayList();
    private List x = Collections.synchronizedList(new ArrayList());
    // public EntityTracker[] tracker = new EntityTracker[3]; // CraftBukkit - removed!
    public boolean onlineMode;
    public boolean spawnAnimals;
    public boolean pvpMode;
    public boolean allowFlight;
    public String r;
    private RemoteStatusListener y;
    private RemoteControlListener z;

    // CraftBukkit start
    public List<WorldServer> worlds = new ArrayList<WorldServer>();
    public CraftServer server;
    public OptionSet options;
    public ConsoleCommandSender console;
    public RemoteConsoleCommandSender remoteConsole;
    public ConsoleReader reader;
    public static int currentTick;
    // CraftBukkit end

    public MinecraftServer(OptionSet options) { // CraftBukkit - adds argument OptionSet
        new ThreadSleepForever(this);

        // CraftBukkit start
        this.options = options;
        try {
            this.reader = new ConsoleReader();
        } catch (IOException ex) {
            Logger.getLogger(MinecraftServer.class.getName()).log(Level.SEVERE, null, ex);
        }
        Runtime.getRuntime().addShutdownHook(new ServerShutdownThread(this));
        // CraftBukkit end
    }

    private boolean init() throws UnknownHostException { // CraftBukkit - added throws UnknownHostException
        this.consoleCommandHandler = new ConsoleCommandHandler(this);
        ThreadCommandReader threadcommandreader = new ThreadCommandReader(this);

        threadcommandreader.setDaemon(true);
        threadcommandreader.start();
        ConsoleLogManager.init(this); // CraftBukkit

        // CraftBukkit start
        System.setOut(new PrintStream(new LoggerOutputStream(log, Level.INFO), true));
        System.setErr(new PrintStream(new LoggerOutputStream(log, Level.SEVERE), true));
        // CraftBukkit end

        log.info("Starting minecraft server version 1.0.1");
        if (Runtime.getRuntime().maxMemory() / 1024L / 1024L < 512L) {
            log.warning("**** NOT ENOUGH RAM!");
            log.warning("To start the server with more ram, launch it as \"java -Xmx1024M -Xms1024M -jar minecraft_server.jar\"");
        }

        log.info("Loading properties");
        this.propertyManager = new PropertyManager(this.options); // CraftBukkit - CLI argument support
        this.s = this.propertyManager.getString("server-ip", "");
        this.onlineMode = this.propertyManager.getBoolean("online-mode", true);
        this.spawnAnimals = this.propertyManager.getBoolean("spawn-animals", true);
        this.pvpMode = this.propertyManager.getBoolean("pvp", true);
        this.allowFlight = this.propertyManager.getBoolean("allow-flight", false);
        this.r = this.propertyManager.getString("motd", "A Minecraft Server");
        this.r.replace('\u00a7', '$');
        InetAddress inetaddress = null;

        if (this.s.length() > 0) {
            inetaddress = InetAddress.getByName(this.s);
        }

        this.t = this.propertyManager.getInt("server-port", 25565);
        log.info("Starting Minecraft server on " + (this.s.length() == 0 ? "*" : this.s) + ":" + this.t);

        try {
            this.networkListenThread = new NetworkListenThread(this, inetaddress, this.t);
        } catch (Throwable ioexception) { // CraftBukkit - IOException -> Throwable
            log.warning("**** FAILED TO BIND TO PORT!");
            log.log(Level.WARNING, "The exception was: " + ioexception.toString());
            log.warning("Perhaps a server is already running on that port?");
            return false;
        }

        if (!this.onlineMode) {
            log.warning("**** SERVER IS RUNNING IN OFFLINE/INSECURE MODE!");
            log.warning("The server will make no attempt to authenticate usernames. Beware.");
            log.warning("While this makes the game possible to play without internet access, it also opens up the ability for hackers to connect with any username they choose.");
            log.warning("To change this, set \"online-mode\" to \"true\" in the server.properties file."); // CraftBukkit - type. Seriously. :D
        }

        this.serverConfigurationManager = new ServerConfigurationManager(this);
        // CraftBukkit - removed trackers
        long i = System.nanoTime();
        String s = this.propertyManager.getString("level-name", "world");
        String s1 = this.propertyManager.getString("level-seed", "");
        long j = (new Random()).nextLong();

        if (s1.length() > 0) {
            try {
                long k = Long.parseLong(s1);

                if (k != 0L) {
                    j = k;
                }
            } catch (NumberFormatException numberformatexception) {
                j = (long) s1.hashCode();
            }
        }

        log.info("Preparing level \"" + s + "\"");
        this.a(new WorldLoaderServer(new File(".")), s, j);

        // CraftBukkit start
        long elapsed = System.nanoTime() - i;
        String time = String.format("%.3fs", elapsed / 10000000000.0D);
        log.info("Done (" + time + ")! For help, type \"help\" or \"?\"");
        // CratBukkit end

        if (this.propertyManager.getBoolean("enable-query", false)) {
            log.info("Starting GS4 status listener");
            this.y = new RemoteStatusListener(this);
            this.y.a();
        }

        if (this.propertyManager.getBoolean("enable-rcon", false)) {
            log.info("Starting remote control listener");
            this.z = new RemoteControlListener(this);
            this.z.a();
            this.remoteConsole = new CraftRemoteConsoleCommandSender();
        }

        // CraftBukkit start
        if (this.propertyManager.properties.containsKey("spawn-protection")) {
            log.info("'spawn-protection' in server.properties has been moved to 'settings.spawn-radius' in bukkit.yml. I will move your config for you.");
            this.server.setSpawnRadius(this.propertyManager.getInt("spawn-protection", 16));
            this.propertyManager.properties.remove("spawn-protection");
            this.propertyManager.savePropertiesFile();
        }
        // CratBukkit end

        return true;
    }

    private void a(Convertable convertable, String s, long i) {
        if (convertable.isConvertable(s)) {
            log.info("Converting map!");
            convertable.convert(s, new ConvertProgressUpdater(this));
        }

        // CraftBukkit - removed world and ticktime arrays
        int j = this.propertyManager.getInt("gamemode", 0);

        j = WorldSettings.a(j);
        log.info("Default game type: " + j);

        // CraftBukkit start (+ removed worldsettings and servernbtmanager)
        int worldCount = 3;

        for (int k = 0; k < worldCount; ++k) {
            WorldServer world;
            int dimension = 0;

            if (k == 1) {
                if (this.propertyManager.getBoolean("allow-nether", true)) {
                    dimension = -1;
                } else {
                    continue;
                }
            }

            if (k == 2) {
                // CraftBukkit start (+ don't do this in server.properties, do it in bukkit.yml)
                if (this.server.getAllowEnd()) {
                    dimension = 1;
                } else {
                    continue;
                }
                // CraftBukkit end
            }

            String worldType = Environment.getEnvironment(dimension).toString().toLowerCase();
            String name = (dimension == 0) ? s : s + "_" + worldType;

            ChunkGenerator gen = this.server.getGenerator(name);
            WorldSettings settings = new WorldSettings(i, j, true, false);

            if (k == 0) {
                world = new WorldServer(this, new ServerNBTManager(server.getWorldContainer(), s, true), s, dimension, settings, org.bukkit.World.Environment.getEnvironment(dimension), gen); // CraftBukkit
            } else {
                String dim = "DIM-1";

                File newWorld = new File(new File(name), dim);
                File oldWorld = new File(new File(s), dim);

                if ((!newWorld.isDirectory()) && (oldWorld.isDirectory())) {
                    log.info("---- Migration of old " + worldType + " folder required ----");
                    log.info("Unfortunately due to the way that Minecraft implemented multiworld support in 1.6, Bukkit requires that you move your " + worldType + " folder to a new location in order to operate correctly.");
                    log.info("We will move this folder for you, but it will mean that you need to move it back should you wish to stop using Bukkit in the future.");
                    log.info("Attempting to move " + oldWorld + " to " + newWorld + "...");

                    if (newWorld.exists()) {
                        log.severe("A file or folder already exists at " + newWorld + "!");
                        log.info("---- Migration of old " + worldType + " folder failed ----");
                    } else if (newWorld.getParentFile().mkdirs()) {
                        if (oldWorld.renameTo(newWorld)) {
                            log.info("Success! To restore the nether in the future, simply move " + newWorld + " to " + oldWorld);
                            log.info("---- Migration of old " + worldType + " folder complete ----");
                        } else {
                            log.severe("Could not move folder " + oldWorld + " to " + newWorld + "!");
                            log.info("---- Migration of old " + worldType + " folder failed ----");
                        }
                    } else {
                        log.severe("Could not create path for " + newWorld + "!");
                        log.info("---- Migration of old " + worldType + " folder failed ----");
                    }
                }

                world = new SecondaryWorldServer(this, new ServerNBTManager(server.getWorldContainer(), name, true), name, dimension, settings, this.worlds.get(0), org.bukkit.World.Environment.getEnvironment(dimension), gen); // CraftBukkit
            }

            if (gen != null) {
                world.getWorld().getPopulators().addAll(gen.getDefaultPopulators(world.getWorld()));
            }

            this.server.getPluginManager().callEvent(new WorldInitEvent(world.getWorld()));

            world.tracker = new EntityTracker(this, world); // CraftBukkit
            world.addIWorldAccess(new WorldManager(this, world));
            world.difficulty = this.propertyManager.getInt("difficulty", 1);
            world.setSpawnFlags(this.propertyManager.getBoolean("spawn-monsters", true), this.spawnAnimals);
            world.getWorldData().setGameType(j);
            this.worlds.add(world);
            this.serverConfigurationManager.setPlayerFileData(this.worlds.toArray(new WorldServer[0]));
        }
        // CraftBukkit end

        short short1 = 196;
        long l = System.currentTimeMillis();

        // CraftBukkit start
        for (int i1 = 0; i1 < this.worlds.size(); ++i1) {
            // if (i1 == 0 || this.propertyManager.getBoolean("allow-nether", true)) {
            WorldServer worldserver = this.worlds.get(i1);
            log.info("Preparing start region for level " + i1 + " (Seed: " + worldserver.getSeed() + ")");
            if (worldserver.getWorld().getKeepSpawnInMemory()) {
                // CraftBukkit end
                ChunkCoordinates chunkcoordinates = worldserver.getSpawn();

                for (int j1 = -short1; j1 <= short1 && this.isRunning; j1 += 16) {
                    for (int k1 = -short1; k1 <= short1 && this.isRunning; k1 += 16) {
                        long l1 = System.currentTimeMillis();

                        if (l1 < l) {
                            l = l1;
                        }

                        if (l1 > l + 1000L) {
                            int i2 = (short1 * 2 + 1) * (short1 * 2 + 1);
                            int j2 = (j1 + short1) * (short1 * 2 + 1) + k1 + 1;

                            this.b("Preparing spawn area", j2 * 100 / i2);
                            l = l1;
                        }

                        worldserver.chunkProviderServer.getChunkAt(chunkcoordinates.x + j1 >> 4, chunkcoordinates.z + k1 >> 4);

                        while (worldserver.updateLights() && this.isRunning) {
                            ;
                        }
                    }
                }
            } // CraftBukkit
        }

        // CraftBukkit start
        for (World world : this.worlds) {
            this.server.getPluginManager().callEvent(new WorldLoadEvent(world.getWorld()));
        }
        // CraftBukkit end

        this.t();
    }

    private void b(String s, int i) {
        this.k = s;
        this.l = i;
        log.info(s + ": " + i + "%");
    }

    private void t() {
        this.k = null;
        this.l = 0;

        this.server.enablePlugins(PluginLoadOrder.POSTWORLD); // CraftBukkit
    }

    void saveChunks() { // CraftBukkit - private -> default
        log.info("Saving chunks");

        // CraftBukkit start
        for (int i = 0; i < this.worlds.size(); ++i) {
            WorldServer worldserver = this.worlds.get(i);

            worldserver.save(true, (IProgressUpdate) null);
            worldserver.saveLevel();

            WorldSaveEvent event = new WorldSaveEvent(worldserver.getWorld());
            this.server.getPluginManager().callEvent(event);
        }

        WorldServer world = this.worlds.get(0);
        if (!world.savingDisabled) {
            this.serverConfigurationManager.savePlayers();
        }
        // CraftBukkit end
    }

    public void stop() { // CraftBukkit - private -> public
        log.info("Stopping server");
        // CraftBukkit start
        if (this.server != null) {
            this.server.disablePlugins();
        }
        // CraftBukkit end

        if (this.serverConfigurationManager != null) {
            this.serverConfigurationManager.savePlayers();
        }

        // CraftBukkit start - multiworld is handled in saveChunks() already.
        WorldServer worldserver = this.worlds.get(0);

        if (worldserver != null) {
            this.saveChunks();
        }
        // CraftBukkit end
    }

    public void safeShutdown() {
        this.isRunning = false;
    }

    public void run() {
        try {
            if (this.init()) {
                long i = System.currentTimeMillis();

                for (long j = 0L; this.isRunning; Thread.sleep(1L)) {
                    long k = System.currentTimeMillis();
                    long l = k - i;

                    if (l > 2000L) {
                        log.warning("Can\'t keep up! Did the system time change, or is the server overloaded?");
                        l = 2000L;
                    }

                    if (l < 0L) {
                        log.warning("Time ran backwards! Did the system time change?");
                        l = 0L;
                    }

                    j += l;
                    i = k;
                    if (this.worlds.get(0).everyoneDeeplySleeping()) { // CraftBukkit
                        this.w();
                        j = 0L;
                    } else {
                        while (j > 50L) {
                            MinecraftServer.currentTick = (int) (System.currentTimeMillis() / 50); // CraftBukkit
                            j -= 50L;
                            this.w();
                        }
                    }
                }
            } else {
                while (this.isRunning) {
                    this.b();

                    try {
                        Thread.sleep(10L);
                    } catch (InterruptedException interruptedexception) {
                        interruptedexception.printStackTrace();
                    }
                }
            }
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            log.log(Level.SEVERE, "Unexpected exception", throwable);

            while (this.isRunning) {
                this.b();

                try {
                    Thread.sleep(10L);
                } catch (InterruptedException interruptedexception1) {
                    interruptedexception1.printStackTrace();
                }
            }
        } finally {
            try {
                this.stop();
                this.isStopped = true;
            } catch (Throwable throwable1) {
                throwable1.printStackTrace();
            } finally {
                System.exit(0);
            }
        }
    }

    private void w() {
        long i = System.nanoTime();
        ArrayList arraylist = new ArrayList();
        Iterator iterator = trackerList.keySet().iterator();

        while (iterator.hasNext()) {
            String s = (String) iterator.next();
            int j = ((Integer) trackerList.get(s)).intValue();

            if (j > 0) {
                trackerList.put(s, Integer.valueOf(j - 1));
            } else {
                arraylist.add(s);
            }
        }

        int k;

        for (k = 0; k < arraylist.size(); ++k) {
            trackerList.remove(arraylist.get(k));
        }

        AxisAlignedBB.a();
        Vec3D.a();
        ++this.ticks;

        // CraftBukkit start - only send timeupdates to the people in that world

        ((CraftScheduler) this.server.getScheduler()).mainThreadHeartbeat(this.ticks);

        // Send timeupdates to everyone, it will get the right time from the world the player is in.
        if (this.ticks % 20 == 0) {
            for ( k = 0; k < this.serverConfigurationManager.players.size(); ++k) {
                EntityPlayer entityplayer = (EntityPlayer) this.serverConfigurationManager.players.get(k);
                entityplayer.netServerHandler.sendPacket(new Packet4UpdateTime(entityplayer.getPlayerTime())); // Add support for per player time
            }
        }

        for (k = 0; k < this.worlds.size(); ++k) {
            long l = System.nanoTime();
            // if (k == 0 || this.propertyManager.getBoolean("allow-nether", true)) {
                WorldServer worldserver = this.worlds.get(k);

                /* Drop global timeupdates
                if (this.ticks % 20 == 0) {
                    this.serverConfigurationManager.a(new Packet4UpdateTime(worldserver.getTime()), worldserver.worldProvider.dimension);
                }
                // CraftBukkit end */

                worldserver.doTick();

                while (true) {
                    if (!worldserver.updateLights()) {
                        worldserver.tickEntities();
                        break;
                    }
                }
            }

            // this.g[k][this.ticks % 100] = System.nanoTime() - l; // CraftBukkit
        // } // CraftBukkit

        this.networkListenThread.a();
        this.serverConfigurationManager.tick();

        // CraftBukkit start
        for (k = 0; k < this.worlds.size(); ++k) {
            this.worlds.get(k).tracker.updatePlayers();
        }
        // CraftBukkit end

        for (k = 0; k < this.w.size(); ++k) {
            ((IUpdatePlayerListBox) this.w.get(k)).a();
        }

        try {
            this.b();
        } catch (Exception exception) {
            log.log(Level.WARNING, "Unexpected exception while parsing console command", exception);
        }

        this.f[this.ticks % 100] = System.nanoTime() - i;
    }

    public void issueCommand(String s, ICommandListener icommandlistener) {
        this.x.add(new ServerCommand(s, icommandlistener));
    }

    public void b() {
        while (this.x.size() > 0) {
            ServerCommand servercommand = (ServerCommand) this.x.remove(0);

            // CraftBukkit start - ServerCommand for preprocessing
            ServerCommandEvent event = new ServerCommandEvent(Event.Type.SERVER_COMMAND, this.console, servercommand.command);
            this.server.getPluginManager().callEvent(event);
            servercommand = new ServerCommand(event.getCommand(), servercommand.b);
            // CraftBukkit end

            // this.consoleCommandHandler.handle(servercommand); // CraftBukkit - Removed its now called in server.dispatchCommand
            this.server.dispatchCommand(this.console, servercommand); // CraftBukkit
        }
    }

    public void a(IUpdatePlayerListBox iupdateplayerlistbox) {
        this.w.add(iupdateplayerlistbox);
    }

    public static void main(final OptionSet options) { // CraftBukkit - replaces main(String args[])
        StatisticList.a();

        try {
            MinecraftServer minecraftserver = new MinecraftServer(options); // CraftBukkit - pass in the options

            // CraftBukkit - remove gui

            (new ThreadServerApplication("Server thread", minecraftserver)).start();
        } catch (Exception exception) {
            log.log(Level.SEVERE, "Failed to start the minecraft server", exception);
        }
    }

    public File a(String s) {
        return new File(s);
    }

    public void sendMessage(String s) {
        log.info(s);
    }

    public void warning(String s) {
        log.warning(s);
    }

    public String getName() {
        return "CONSOLE";
    }

    public WorldServer getWorldServer(int i) {
        // CraftBukkit start
        for (WorldServer world : this.worlds) {
            if (world.dimension == i) {
                return world;
            }
        }

        return this.worlds.get(0);
        // CraftBukkit end
    }

    public EntityTracker getTracker(int i) {
        return this.getWorldServer(i).tracker; // CraftBukkit
    }

    public int getProperty(String s, int i) {
        return this.propertyManager.getInt(s, i);
    }

    public String a(String s, String s1) {
        return this.propertyManager.getString(s, s1);
    }

    public void a(String s, Object object) {
        this.propertyManager.a(s, object);
    }

    public void c() {
        this.propertyManager.savePropertiesFile();
    }

    public String getPropertiesFile() {
        File file1 = this.propertyManager.c();

        return file1 != null ? file1.getAbsolutePath() : "No settings file";
    }

    public String getMotd() {
        return this.s;
    }

    public int getPort() {
        return this.t;
    }

    public String getServerAddress() {
        return this.r;
    }

    public String getVersion() {
        return "1.0.1";
    }

    public int getPlayerCount() {
        return this.serverConfigurationManager.getPlayerCount();
    }

    public int getMaxPlayers() {
        return this.serverConfigurationManager.getMaxPlayers();
    }

    public String[] getPlayers() {
        return this.serverConfigurationManager.d();
    }

    public String getWorldName() {
        return this.propertyManager.getString("level-name", "world");
    }

    public String getPlugins() {
        // CraftBukkit start - whole method
        StringBuilder result = new StringBuilder();
        Plugin[] plugins = server.getPluginManager().getPlugins();

        result.append(server.getName());
        result.append(" on Bukkit ");
        result.append(server.getBukkitVersion());

        if (plugins.length > 0) {
            result.append(": ");

            for (int i = 0; i < plugins.length; i++) {
                if (i > 0) {
                    result.append("; ");
                }

                result.append(plugins[i].getDescription().getName());
                result.append(" ");
                result.append(plugins[i].getDescription().getVersion().replaceAll(";", ","));
            }
        }

        return result.toString();
        // CraftBukkit end
    }

    public void o() {}

    public String d(String s) {
        RemoteControlCommandListener.a.a();
        // CraftBukkt start
        ServerCommandEvent event = new ServerCommandEvent(Event.Type.REMOTE_COMMAND, this.remoteConsole, s);
        this.server.getPluginManager().callEvent(event);
        ServerCommand servercommand = new ServerCommand(event.getCommand(), RemoteControlCommandListener.a);
        // this.consoleCommandHandler.handle(new ServerCommand(s, RemoteControlCommandListener.a)); // CraftBukkit - removed
        this.server.dispatchCommand(this.remoteConsole, servercommand); // CraftBukkit
        // CraftBukkit end
        return RemoteControlCommandListener.a.b();
    }

    public boolean isDebugging() {
        return this.propertyManager.getBoolean("debug", false); // CraftBukkit - don't hardcode
    }

    public void severe(String s) {
        log.log(Level.SEVERE, s);
    }

    public void debug(String s) {
        if (this.isDebugging()) {
            log.log(Level.INFO, s);
        }
    }

    public String[] q() {
        return (String[]) this.serverConfigurationManager.getBannedAddresses().toArray(new String[0]);
    }

    public String[] r() {
        return (String[]) this.serverConfigurationManager.getBannedPlayers().toArray(new String[0]);
    }

    public static boolean isRunning(MinecraftServer minecraftserver) {
        return minecraftserver.isRunning;
    }
}
