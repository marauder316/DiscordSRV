/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.objects.threads;

import alexh.weak.Dynamic;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DiscordUtil;
import github.scarsz.discordsrv.util.PlaceholderUtil;
import net.dv8tion.jda.api.entities.Activity;
import org.apache.commons.lang3.StringUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class PresenceUpdater extends Thread {

    private int lastStatusIndex = 0;
    private String lastStatus = null;

    public PresenceUpdater() {
        setName("DiscordSRV - Presence Updater");
    }

    @Override
    public void run() {
        while (true) {
            int rate = DiscordSRV.config().getInt("StatusUpdateRateInMinutes");
            if (rate < 1) rate = 1;

            if (DiscordUtil.getJda() != null) {
                Dynamic dynamic = DiscordSRV.config().dget("DiscordGameStatus");
                List<String> statuses = new LinkedList<>();
                if (dynamic.isList()) {
                    statuses.addAll(dynamic.asList());
                } else {
                    statuses.add(dynamic.convert().intoString());
                }

                String status;
                if (statuses.size() == 0) {
                    status = null;
                } else {
                    int nextStatusIndex = lastStatusIndex + 1;
                    if (nextStatusIndex >= statuses.size()) nextStatusIndex = 0;
                    status = statuses.size() >= nextStatusIndex + 1 ? statuses.get(nextStatusIndex) : null;

                    lastStatusIndex = nextStatusIndex;
                }

                status = PlaceholderUtil.replacePlaceholders(status);
                status = DiscordUtil.strip(status); // remove color codes
                boolean same = Objects.equals(lastStatus, status);
                lastStatus = status;

                if (!same) {
                    if (StringUtils.isNotBlank(status)) {
                        DiscordSRV.debug("Setting presence to \"" + status + "\"");

                        if (StringUtils.startsWithIgnoreCase(status, "watching")) {
                            String removed = status.substring("watching".length()).trim();
                            DiscordUtil.getJda().getPresence().setPresence(Activity.watching(removed), false);
                        } else if (StringUtils.startsWithIgnoreCase(status, "listening to")) {
                            String removed = status.substring("listening to".length()).trim();
                            DiscordUtil.getJda().getPresence().setPresence(Activity.listening(removed), false);
                        } else if (StringUtils.startsWithIgnoreCase(status, "playing")) {
                            String removed = status.substring("playing".length()).trim();
                            DiscordUtil.getJda().getPresence().setPresence(Activity.playing(removed), false);
                        } else {
                            DiscordUtil.getJda().getPresence().setPresence(Activity.playing(status), false);
                        }
                    } else {
                        DiscordUtil.getJda().getPresence().setPresence((Activity) null, false);
                        DiscordSRV.debug("Cleared presence status");
                    }
                } else {
                    DiscordSRV.debug("Not setting presence, status was the same");
                }
            } else {
                DiscordSRV.debug("Skipping status update cycle, JDA was null");
            }

            try {
                Thread.sleep(TimeUnit.MINUTES.toMillis(rate));
            } catch (InterruptedException ignored) {
                DiscordSRV.debug("Broke from Status Updater thread: sleep interrupted");
                return;
            }
        }
    }

}
