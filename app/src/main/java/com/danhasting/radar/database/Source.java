/*
 * Copyright (c) 2018, Dan Hasting
 *
 * This file is part of WeatherRadar
 *
 * WeatherRadar is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * WeatherRadar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with WeatherRadar.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.danhasting.radar.database;

public enum Source {
    NWS(0),
    MOSAIC(1),
    WUNDERGROUND(2);

    private final int sourceInt;

    Source(int sourceInt) {
        this.sourceInt = sourceInt;
    }

    public int getInt() {
        return this.sourceInt;
    }

    private static Source[] values = null;

    public static Source fromInt(int sourceInt) {
        if (Source.values == null)
            Source.values = Source.values();
        return Source.values[sourceInt];
    }
}
