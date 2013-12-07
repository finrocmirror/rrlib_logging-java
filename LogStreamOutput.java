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
 * The enumeration that encodes the streams used by a logging domain.
 * Messages can be streams to stdout, stderr, into on file per domain
 * or into on combined file for all domains that are recursively
 * configured in one subtree of the domain hierarchy.
 */
public enum LogStreamOutput {
    STDOUT,          //!< Messages are printed to stdout
    STDERR,          //!< Messages are printed to stderr
    FILE,            //!< Messages are printed to one file per domain
    COMBINED_FILE,   //!< Messages are collected in one file per recursively configured subtree
    DIMENSION        //!< Endmarker and dimension of eLogStream
}
