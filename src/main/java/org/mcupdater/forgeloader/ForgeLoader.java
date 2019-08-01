package org.mcupdater.forgeloader;

import net.minecraftforge.installer.ProgressFrame;
import net.minecraftforge.installer.SimpleInstaller;
import net.minecraftforge.installer.actions.*;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.OptionalLibrary;
import net.minecraftforge.installer.json.Util;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public class ForgeLoader {

	public static void main(String[] args) {
		File installPath = new File(args[0]);
		String side = args[1];

		File profiles = new File(installPath, "launcher_profiles.json");
		if (!profiles.exists()) {
			InputStream inStream = ForgeLoader.class.getResourceAsStream("/launcher_profiles.json");
			try {
				Files.copy(inStream, profiles.toPath());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		List<OptionalListEntry> optionals = new ArrayList<>();
		Action action;

		Install profile = Util.loadInstallProfile();
		ProgressCallback monitor;
		try {
			monitor = ProgressCallback.withOutputs(new OutputStream[]{System.out, getLog()});
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			monitor = ProgressCallback.withOutputs(new OutputStream[]{System.out});
		}
		switch(side.toLowerCase()) {
			case "client":
				action = new ClientInstall(profile, monitor);
				break;
			case "server":
				action = new ServerInstall(profile, monitor);
				break;
			default:
				action = new ExtractAction(profile, monitor);
		}
		Predicate<String> optPred = input -> {
			Optional<OptionalListEntry> ent = optionals.stream().filter(e -> e.lib.getArtifact().equals(input)).findFirst();
			return !ent.isPresent() || ent.get().isEnabled();
		};
		try {
			action.run(installPath, optPred);
		} catch (ActionCanceledException e) {
			e.printStackTrace();
		}
	}

	private static OutputStream getLog() throws FileNotFoundException {
		File f = new File(SimpleInstaller.class.getProtectionDomain().getCodeSource().getLocation().getFile());
		File output;
		if (f.isFile()) {
			output = new File(f.getName() + ".log");
		} else {
			output = new File("installer.log");
		}

		return new BufferedOutputStream(new FileOutputStream(output));
	}

	private static class OptionalListEntry {
		OptionalLibrary lib;
		private boolean enabled;

		public boolean isEnabled() {
			return this.enabled;
		}
	}
}
