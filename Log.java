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

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This class should be used to output log messages
 */
public class Log {

    /**
     * Log message
     *
     * TODO: This method looks at the stack trace in order to decide if log message
     * should be printed. This is kind of expensive.
     * We might need another more efficient method with more parameters.
     *
     * @param level Log level
     * @param origin Object that log message originates from (can also be a string)
     * @param msg Log message
     */
    public static void log(LogLevel level, Object origin, Object msg) {
        StackTraceElement callFrame = new Throwable().getStackTrace()[1];
        LogDomain domain = LogDomainRegistry.getDomainByQualifiedName(callFrame.getClassName().substring(0, callFrame.getClassName().lastIndexOf('/')));
        domain.log(level, callFrame, origin, msg, 2);
    }

    /**
     * Log message
     *
     * TODO: This method looks at the stack trace in order to decide if log message
     * should be printed. This is kind of expensive.
     * We might need another more efficient method with more parameters.
     *
     * @param level Log level
     * @param msg Log message
     */
    public static void log(LogLevel level, Object msg) {
        StackTraceElement callFrame = new Throwable().getStackTrace()[1];
        LogDomain domain = LogDomainRegistry.getDomainByQualifiedName(callFrame.getClassName().substring(0, callFrame.getClassName().lastIndexOf('/')));
        domain.log(level, callFrame, callFrame.getClassName().substring(callFrame.getClassName().lastIndexOf('/') + 1), msg, 2);
    }

    /**
     * Log message
     *
     * TODO: This method looks at the stack trace in order to decide if log message
     * should be printed. This is kind of expensive.
     * We might need another more efficient method with more parameters.
     *
     * @param level Log level
     * @param origin Object that log message originates from (can also be a string)
     * @param msg Log message
     * @param e Additional exception (if only exception is to be printed, use first method in this class)
     */
    public static void log(LogLevel level, Object origin, Object msg, Exception e) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.append(msg.toString()).append(e.getMessage()).append(" ");
        e.printStackTrace(printWriter);
        printWriter.close();
        log(level, origin, stringWriter);
    }

    /**
     * Get log stream
     *
     * TODO: This method looks at the stack trace in order to decide if log message
     * should be printed. This is kind of expensive.
     * We might need another more efficient method with more parameters.
     *
     * @param level Log level
     * @param origin Object that log message originates from (can also be a string)
     */
    public static LogStream getLogStream(LogLevel level, Object origin) {
        StackTraceElement callFrame = new Throwable().getStackTrace()[1];
        LogDomain domain = LogDomainRegistry.getDomainByQualifiedName(callFrame.getClassName().substring(0, callFrame.getClassName().lastIndexOf('/')));
        return domain.getLogStream(level, origin);
    }

}
