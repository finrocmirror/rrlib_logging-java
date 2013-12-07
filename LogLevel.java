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
 * The enumeration that encodes the message levels. These levels are
 * predefined and can be used to give the messages different priorities,
 * as messages with too low level will be suppressed by a configuration
 * setting or when _RRLIB_LOGGING_LESS_OUTPUT_ is defined (e.g. in
 * release mode).
 * They are also used for colored output to stdout or stderr.
 */
public enum LogLevel {
    USER,             //!< Information for user (including end-users). Is always shown if domain is active.
    ERROR,            //!< Error message. Used to inform about _certain_ malfunction of application. Is always shown if domain is active.
    WARNING,          //!< Critical warning. Warns about possible application malfunction and invalid (and therefore discarded) user input. (default max level with _RRLIB_LOG_LESS_OUTPUT_)
    DEBUG_WARNING,    //!< Debug info with warning character (e.g. "Parameter x not set - using default y")
    DEBUG,            //!< Debug info about coarse program flow (default max level without _RRLIB_LOG_LESS_OUTPUT_) - information possibly relevant to developers outside of respective domain
    DEBUG_VERBOSE_1,  //!< Higher detail debug info (not available in release mode) - only relevant to developers in respective domain
    DEBUG_VERBOSE_2,  //!< Higher detail debug info (not available in release mode) - only relevant to developers in respective domain
    DEBUG_VERBOSE_3,  //!< Higher detail debug info (not available in release mode) - only relevant to developers in respective domain
    DIMENSION         //!< Endmarker and dimension of eLogLevel
};
