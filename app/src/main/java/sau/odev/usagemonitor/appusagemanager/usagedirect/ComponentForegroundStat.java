/*
 * usageDirect
 * Copyright (C) 2020 Fynn Godau
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
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package sau.odev.usagemonitor.appusagemanager.usagedirect;


import android.annotation.SuppressLint;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Object representing a timespan that an application was in the foreground
 */
@SuppressLint("NewApi")
public class ComponentForegroundStat {
    public final long beginTime, endTime;
    public final String packageName;
    public final int launchCount;

    public ComponentForegroundStat(long beginTime, long endTime, String packageName, int launchCount) {
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.packageName = packageName;
        this.launchCount = launchCount;
    }

    @Override
    public String toString() {
        return "ComponentForegroundStat{" +
                "beginTime=" + Instant.ofEpochMilli(beginTime) +
                ", endTime=" + Instant.ofEpochMilli(endTime) +
                ", packageName='" + packageName + '\'' +
                '}';
    }
}
