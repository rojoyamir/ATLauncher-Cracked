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
package com.atlauncher.data;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import com.atlauncher.data.microsoft.LoginResponse;
import com.atlauncher.data.microsoft.OauthTokenResponse;
import com.atlauncher.data.microsoft.Profile;
import com.atlauncher.data.microsoft.XboxLiveAuthResponse;

public class OfflineAccount extends MicrosoftAccount {
    private static final long serialVersionUID = 1L;

    /** Marker field so GSON/AccountManager can distinguish offline accounts. */
    public final boolean isOffline = true;

    public OfflineAccount(String name) {
        super(null, null, loginResponse(name), profile(name));
        this.username = name;
        this.minecraftUsername = name;
        this.accessToken = this.uuid = offlineUuid(name);
    }

    // Called by super() constructor via polymorphism — intentional no-op so we
    // can set fields manually afterwards without touching the OAuth flow.
    @Override
    public void update(OauthTokenResponse oauthTokenResponse, XboxLiveAuthResponse xstsAuthResponse,
            LoginResponse loginResponse, Profile profile) {
    }

    @Override
    public boolean ensureAccountIsLoggedIn() {
        return true;
    }

    @Override
    public boolean ensureAccessTokenValid() {
        return true;
    }

    private static LoginResponse loginResponse(String name) {
        LoginResponse r = new LoginResponse();
        r.username = name;
        return r;
    }

    private static Profile profile(String name) {
        Profile p = new Profile();
        p.name = name;
        p.id = offlineUuid(name);
        return p;
    }

    private static String offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
