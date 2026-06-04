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

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NodeList;

import com.atlauncher.managers.LogManager;
import com.atlauncher.network.Download;

/**
 * Lightweight reader for Maven {@code maven-metadata.xml} files, used to list available loader
 * versions directly from upstream Maven repositories (bypassing the ATLauncher API).
 */
public class MavenMetadata {
    /**
     * Downloads a {@code maven-metadata.xml} and returns every {@code <version>} value in the order
     * they appear (Maven lists oldest first). Returns an empty list on any failure.
     */
    public static List<String> getVersions(String url) {
        List<String> versions = new ArrayList<>();

        try {
            String xml = Download.build().setUrl(url).asString();

            if (xml == null || xml.isEmpty()) {
                return versions;
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // Harden against XXE - we only ever read simple version listings.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setExpandEntityReferences(false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            NodeList nodes = builder
                    .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)))
                    .getElementsByTagName("version");

            for (int i = 0; i < nodes.getLength(); i++) {
                versions.add(nodes.item(i).getTextContent().trim());
            }
        } catch (Exception e) {
            LogManager.logStackTrace(String.format("Error reading maven metadata from %s", url), e);
        }

        return versions;
    }
}
