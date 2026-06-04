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

import com.atlauncher.managers.LogManager;

public class ShortcutManager {
    /**
     * Creates a desktop shortcut to ATLauncher. Returns true on success, false on failure.
     */
    public static boolean createDesktopShortcut() {
        try {
            // Get the path to the currently running EXE
            String exePath = getExePath();
            if (exePath == null || exePath.isEmpty()) {
                LogManager.warn("Could not determine EXE path for shortcut");
                return false;
            }

            // Desktop folder
            String desktop = System.getProperty("user.home") + "\\Desktop";
            String shortcutPath = desktop + "\\ATLauncher Offline.lnk";

            // Create VBScript to generate .lnk file (portable, no external deps)
            String vbScript = createVBScript(exePath, shortcutPath);

            // Write VBScript to temp file
            Path tempScript = Files.createTempFile("atlauncher_shortcut_", ".vbs");
            Files.write(tempScript, vbScript.getBytes(StandardCharsets.UTF_8));

            // Execute VBScript
            ProcessBuilder pb = new ProcessBuilder("cscript.exe", tempScript.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            int exitCode = process.waitFor();

            // Clean up temp file
            Files.deleteIfExists(tempScript);

            if (exitCode == 0) {
                LogManager.info("Desktop shortcut created successfully at " + shortcutPath);
                return true;
            } else {
                LogManager.warn("VBScript exited with code " + exitCode);
                return false;
            }

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
            // Try to get from sun.java.command property (set by Launch4j)
            String cmd = System.getProperty("sun.java.command", "");
            if (!cmd.isEmpty()) {
                // Format is typically: "path\to\ATLauncher.exe" [other args]
                String[] parts = cmd.split(" ");
                if (parts.length > 0) {
                    String exePath = parts[0].replaceAll("\"", "");
                    if (exePath.endsWith(".exe")) {
                        return exePath;
                    }
                }
            }

            // Fallback: construct path based on classloader
            String classPath = ShortcutManager.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            if (classPath.contains(".exe")) {
                return classPath.substring(0, classPath.lastIndexOf(".exe") + 4);
            }

            // Final fallback: check if running from a JAR and find adjacent EXE
            File classPathFile = new File(classPath);
            if (classPathFile.getParent() != null) {
                File[] files = new File(classPathFile.getParent()).listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().endsWith(".exe")) {
                            return f.getAbsolutePath();
                        }
                    }
                }
            }

            return null;
        } catch (Exception e) {
            LogManager.logStackTrace("Error getting EXE path", e);
            return null;
        }
    }

    /**
     * Generate VBScript code to create a Windows .lnk shortcut file.
     */
    private static String createVBScript(String exePath, String shortcutPath) {
        // VBScript to create a .lnk shortcut
        return "Set oWS = WScript.CreateObject(\"WScript.Shell\")\n" +
               "sLinkFile = \"" + shortcutPath + "\"\n" +
               "Set oLink = oWS.CreateShortcut(sLinkFile)\n" +
               "oLink.TargetPath = \"" + exePath + "\"\n" +
               "oLink.WorkingDirectory = \"" + new File(exePath).getParent() + "\"\n" +
               "oLink.Description = \"ATLauncher Offline\"\n" +
               "oLink.Save\n" +
               "WScript.Echo \"Shortcut created\"\n";
    }
}
