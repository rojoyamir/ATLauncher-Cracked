/*
 * ATLauncher - https://github.com/ATLauncher/ATLauncher
 * Copyright (C) 2013-2022 ATLauncher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package com.atlauncher.utils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import javax.swing.filechooser.FileSystemView;

import com.atlauncher.managers.LogManager;

public class ShortcutManager {
    /**
     * Creates a desktop shortcut to ATLauncher. Returns true on success, false on failure.
     */
    public static boolean createDesktopShortcut() {
        try {
            String exePath = getExePath();
            if (exePath == null || exePath.isEmpty()) {
                LogManager.warn("Could not determine EXE path for shortcut");
                return false;
            }

            Path desktopPath = getDesktopPath();
            if (desktopPath == null) {
                LogManager.warn("Could not determine desktop path for shortcut");
                return false;
            }

            Path shortcutPath = desktopPath.resolve("ATLauncher Offline.lnk");
            String vbScript = createVBScript(exePath, shortcutPath.toString());
            Path tempScript = Files.createTempFile("atlauncher_shortcut_", ".vbs");
            Files.write(tempScript, vbScript.getBytes(StandardCharsets.UTF_8));

            LogManager.info("Creating desktop shortcut target=" + exePath + " shortcut=" + shortcutPath);

            ProcessBuilder pb = new ProcessBuilder("cscript.exe", "//nologo", tempScript.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String scriptOutput = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8)
                .trim();
            int exitCode = process.waitFor();

            Files.deleteIfExists(tempScript);

            if (exitCode == 0 && Files.exists(shortcutPath)) {
                LogManager.info("Desktop shortcut created successfully at " + shortcutPath);
                if (!scriptOutput.isEmpty()) {
                    LogManager.info("cscript.exe output: " + scriptOutput);
                }
                return true;
            }

            LogManager.warn("Desktop shortcut creation failed target=" + exePath + " shortcut=" + shortcutPath
                + " exitCode=" + exitCode + " output=" + scriptOutput);
            return false;
        } catch (Exception e) {
            LogManager.logStackTrace("Error creating desktop shortcut", e);
            return false;
        }
    }

    /**
     * Get the path to the currently running EXE. Works with Launch4j-wrapped jars.
     */
    private static String getExePath() {
        try {
            Path codeSourcePath = Paths.get(ShortcutManager.class.getProtectionDomain().getCodeSource()
                .getLocation().toURI()).toAbsolutePath().normalize();

            if (codeSourcePath.toString().toLowerCase(Locale.ROOT).endsWith(".exe")) {
                return codeSourcePath.toString();
            }

            Path candidate = findAdjacentExe(codeSourcePath.getParent());
            if (candidate != null) {
                return candidate.toString();
            }

            String cmd = System.getProperty("sun.java.command", "").trim();
            if (cmd.toLowerCase(Locale.ROOT).endsWith(".exe")) {
                Path commandPath = Paths.get(cmd.replace("\"", "")).toAbsolutePath().normalize();
                if (Files.exists(commandPath)) {
                    return commandPath.toString();
                }
            }

            return null;
        } catch (Exception e) {
            LogManager.logStackTrace("Error getting EXE path", e);
            return null;
        }
    }

    private static Path getDesktopPath() {
        File desktopDirectory = FileSystemView.getFileSystemView().getHomeDirectory();
        if (desktopDirectory != null && desktopDirectory.isDirectory()) {
            return desktopDirectory.toPath();
        }

        String userHome = System.getProperty("user.home");
        if (userHome == null || userHome.isEmpty()) {
            return null;
        }

        return Paths.get(userHome, "Desktop");
    }

    private static Path findAdjacentExe(Path directory) throws Exception {
        if (directory == null || !Files.isDirectory(directory)) {
            return null;
        }

        try (Stream<Path> files = Files.list(directory)) {
            List<Path> executables = files
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".exe"))
                .sorted()
                .toList();

            if (executables.isEmpty()) {
                return null;
            }

            for (Path executable : executables) {
                String name = executable.getFileName().toString().toLowerCase(Locale.ROOT);
                if (name.startsWith("atlauncher-offline")) {
                    return executable.toAbsolutePath().normalize();
                }
            }

            return executables.get(0).toAbsolutePath().normalize();
        }
    }

    /**
     * Generate VBScript code to create a Windows .lnk shortcut file.
     */
    private static String createVBScript(String exePath, String shortcutPath) {
        String workingDirectory = new File(exePath).getParent();

        return "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
            "sLinkFile = \"" + escapeForVbscript(shortcutPath) + "\"\n" +
            "Set oLink = oWS.CreateShortcut(sLinkFile)\n" +
            "oLink.TargetPath = \"" + escapeForVbscript(exePath) + "\"\n" +
            "oLink.WorkingDirectory = \"" + escapeForVbscript(workingDirectory) + "\"\n" +
            "oLink.Description = \"ATLauncher Offline\"\n" +
            "oLink.Save\n" +
            "WScript.Echo \"Shortcut created\"\n";
    }

    private static String escapeForVbscript(String value) {
        return value.replace("\"", "\"\"");
    }
}
