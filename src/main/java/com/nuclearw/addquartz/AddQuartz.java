package com.nuclearw.addquartz;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_5_R2.CraftWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import net.minecraft.server.v1_5_R2.WorldGenMinable;

public class AddQuartz extends JavaPlugin implements Listener, Runnable {
	private static WorldGenMinable orePopulator = new WorldGenMinable(Material.QUARTZ_ORE.getId(), 13, Material.NETHERRACK.getId());
	private static Random random = new Random();

	private static LinkedBlockingQueue<ChunkLocation> chunksToProcess = new LinkedBlockingQueue<ChunkLocation>();
	private static HashSet<String> processList = new HashSet<String>();
	private static HashSet<String> processedList = new HashSet<String>();

	private static File processedFile;
	private static int processedCount = 0;

	@Override
	public void onEnable() {
		getDataFolder().mkdir();

		processedFile = new File(getDataFolder(), "addQuartzProcessed");

		saveDefaultConfig();

		processList.addAll(getConfig().getStringList("worlds"));

		try {
			Files.touch(processedFile);
			BufferedReader reader = Files.newReader(processedFile, Charsets.UTF_8);

			String line;
			while((line = reader.readLine()) != null) {
				processedList.add(line);
			}

			reader.close();
		} catch(Exception e) {
			e.printStackTrace();
		}

		getServer().getPluginManager().registerEvents(this, this);

		getServer().getScheduler().runTaskTimer(this, this, 20, 20);
	}

	@Override
	public void onDisable() {
		saveProcessed();
	}

	@Override
	public void run() {
		if(chunksToProcess.isEmpty()) return;

		int processed = 0;
		while(!chunksToProcess.isEmpty() && processed < 10) {
			ChunkLocation location = chunksToProcess.poll();

			int cx = location.x * 16;
			int cz = location.z * 16;

			for(int i = 0; i < 16; i++) {
	            int x = cx + random.nextInt(16);
	            int y = random.nextInt(108) + 10;
	            int z = cz + random.nextInt(16);
	            orePopulator.a(((CraftWorld) getServer().getWorld(location.world)).getHandle(), random, x, y, z);
	        }

			processedList.add(location.toString());
			processed++;
		}

		if(processed > 0) {
			processedCount++;
		}

		if(processedCount > 10) {
			saveProcessed();
			processedCount = 0;
		}
	}

	@EventHandler
	public void onChunkLoad(ChunkLoadEvent event) {
		if(!event.getWorld().getEnvironment().equals(World.Environment.NETHER)) return;
		if(!processList.contains(event.getWorld().getName())) return;
		if(processedList.contains(event.getWorld().getName() + ":" + event.getChunk().getX() + ":" + event.getChunk().getZ())) return;

		chunksToProcess.add(new ChunkLocation(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ()));
	}

	private static void saveProcessed() {
		try {
			BufferedWriter writer = Files.newWriter(processedFile, Charsets.UTF_8);
			for(String line : processedList) {
				writer.write(line + "\n");
			}

			writer.flush();
			writer.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private class ChunkLocation {
		protected final int x, z;
		protected final String world;

		public ChunkLocation(String world, int x, int z) {
			this.x = x;
			this.z = z;
			this.world = world;
		}

		@Override
		public String toString() {
			return world + ":" + x + ":" + z;
		}
	}
}
