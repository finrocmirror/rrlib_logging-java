//
// You received this file as part of RRLib
// Robotics Research Library
//
// Copyright (C) Finroc GbR (finroc.org)
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
//----------------------------------------------------------------------
package org.rrlib.logging;

/**
 * Java variant of C++ LogStream
 */
public class LogStream {

    private LogLevel level;
    private Object description;
    private LogDomain domain;
    private StringBuilder buffer = new StringBuilder();

    LogStream(LogLevel level, LogDomain domain, Object description) {
        this.level = level;
        this.domain = domain;
        this.description = description;
    }

    public LogStream append(String s) {
        buffer.append(s);
        return this;
    }

    public LogStream append(short s) {
        buffer.append(s);
        return this;
    }

    public LogStream append(int s) {
        buffer.append(s);
        return this;
    }

    public LogStream appendln(String s) {
        buffer.append(s);
        buffer.append('\n');
        return this;
    }

    public void close() {
        domain.log(level, null, description, buffer, 2);
    }
}
