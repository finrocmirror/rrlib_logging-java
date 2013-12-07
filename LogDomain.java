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

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

/**
 * The RRLib logging system is structured into hierarchical domains that
 * can be created and configured via LoggingDomainRegistry. That given,
 * in the program implementation instances of the class LoggingDomain
 * wrap the stream that can be access either in C++ iostream style via
 * operator << or in good old-fashioned C style using printf formatting.
 *
 * This class implements messaging via a specific logging domain
 * The RRLib logging system is structured into hierarchical domains that
 * can be created and configured via LoggingDomainRegistry. That given,
 * in the program implementation instances of this class wrap the stream
 * that can be access either in C++ iostream style via operator << or
 * in the good old-fashioned C style using printf formatting.
 *
 * @author Max Reichardt
 * @author Tobias Föhst
 */
public class LogDomain {

    LogDomain parent;
    ArrayList<LogDomain> children = new ArrayList<LogDomain>();

    private LogDomainConfiguration configuration;

    //private PrintStream streamBuffer;
    //private OutputStream stream;
    class FileStream extends PrintStream {
        public FileStream(String fileName) throws FileNotFoundException {
            super(new BufferedOutputStream(new FileOutputStream(fileName)));
        }
    }

    private FileStream fileStream;

    private int outputStreamsRevision = -1;
    private ArrayList<PrintStream> outputStreams = new ArrayList<PrintStream>();

    public static final DateFormat format = DateFormat.getTimeInstance();

    private StringBuilder buffer = new StringBuilder(); // temporary buffer for output string (only use with lock on domain)

    /** Use colored console output ? (we don't want during debugging in IDEs such as Eclipse or on Android devices) */
    private final static boolean COLORED_CONSOLE_OUTPUT;

    static {
        boolean coloredOutput = false;
        try {
            coloredOutput = LogDomain.class.getResource("LogDomain.class").toString().contains(".jar!");
        } catch (Exception e) {}
        COLORED_CONSOLE_OUTPUT = coloredOutput;
    }

    /** The ctor of a top level domain
     *
     * This ctor is to be called by the registry that creates the top level
     * domain.
     *
     * @param configuration   The configuration for the new domain
     */
    LogDomain(LogDomainConfiguration configuration) {
        this.configuration = configuration;
        //streamBuffer = new PrintStream
    }

    /** The ctor for a new sub domain
     *
     * This ctor is to be called by the registry to create a new subdomain
     * with a given configuration
     *
     * @param configuration   The configuration for the new domain
     * @param parent          The parent domain
     */
    LogDomain(LogDomainConfiguration configuration, LogDomain parent) {
        this(configuration);
        this.parent = parent;
        parent.children.add(this);
        configureSubTree();
    }

    /** Recursively configure the subtree that begins in this domain
     *
     * If the domain is configured by its parent, the configuration is
     * copied and propagated to this domain's children
     */
    void configureSubTree() {
        if (parent != null && parent.configuration.configureSubTree) {
            configuration = new LogDomainConfiguration(configuration.name, parent.configuration);
            for (LogDomain ld : children) {
                ld.configureSubTree();
            }
        }
    }

    /** Open the file stream for file output
     *
     * This method creates a new file which name is build using a prefix
     * and the full qualified domain name.
     * If the file already exists, it will be truncated.
     *
     * @return Whether the file stream could be opened or not
     */
    private boolean openFileOutputStream() {
        if (fileStream != null) {
            return true;
        }
        String fileNamePrefix = LogDomainRegistry.getInstance().getOutputFileNamePrefix();
        if (fileNamePrefix.length() == 0) {
            System.err.println("RRLib Logging >> Prefix for file names not set. Can not use eMS_FILE.");
            System.err.println("Consider calling tMessageDomainRegistry::GetInstance().SetOutputFileNamePrefix(basename(argv[0])) for example.");
            return false;
        }
        String fileName = fileNamePrefix + getName() + ".log";
        try {
            fileStream = new FileStream(fileName);
        } catch (Exception e) {
            System.err.println("RRLib Logging >> Could not open file `" + fileName + "'!");
            return false;
        }
        return true;
    }

    /** Setup the output stream to be used in this domain
     *
     * A domain can stream its input to stdout, stderr, an own file and/or its parent's file.
     *
     *@param outputs   Streams to output to
     */
    private void setupOutputStream(/*LogStream... outputs*/)  {
        if (outputStreamsRevision == configuration.streamMaskRevision) {
            return;
        }

        synchronized (configuration) {
            outputStreams.clear();
            Set<PrintStream> tmp = new HashSet<PrintStream>();
            for (LogStreamOutput ls : configuration.streamMask) {
                if (ls == LogStreamOutput.STDOUT) {
                    tmp.add(System.out);
                } else if (ls == LogStreamOutput.STDERR) {
                    tmp.add(System.err);
                } else if (ls == LogStreamOutput.FILE) {
                    tmp.add(openFileOutputStream() ? fileStream : System.err);
                } else if (ls == LogStreamOutput.COMBINED_FILE) {
                    LogDomain domain = this;
                    for (; domain.parent != null && domain.parent.configuration.configureSubTree; domain = domain.parent) {}
                    tmp.add(domain.openFileOutputStream() ? fileStream : System.err);
                }
            }
            outputStreams.addAll(tmp);
            outputStreamsRevision = configuration.streamMaskRevision;
        }
    }

    /** Get the current time as string for internal use in messages
     *
     * This method formats the current time as string that can be used in
     * messages.
     *
     * @return The current time as string
     */
    private String getTimeString() {
        return format.format(System.currentTimeMillis());
    }

    /** Get the domain's name as string for internal use in messages
     *
     * This method formats the name as string that can be used in
     * messages. This string is padded with spaces to the length of the
     * longest domain name
     *
     * @return The padded name as string
     */
    private String getNameString() {
        // TODO: maybe do this properly with padding
        return configuration.name;
    }

    /** Get the given message level as string for internal use in messages
     *
     * This method formats the given level as string that can be used in
     * messages.
     *
     * @param level   The level that should be represented as string
     *
     * @return The given level as padded string
     */
    private String getLevelString(LogLevel level) {
        switch (level) {
        case ERROR:
            return "[error]   ";
        case WARNING:
            return "[warning] ";
        case DEBUG_WARNING:
            return "[debug]   ";
        case DEBUG:
            return "[debug]   ";
        default:
            return "          ";
        }
    }

    /** Get the given location as string for internal use in messages
     *
     * This method formats given location consisting of a file name and a
     * line number as string that can be used in messages.
     *
     * @param file   The file name (e.g. from __FILE__)
     * @param line   The line number (e.g. from __LINE__)
     *
     * @return The given location as string
     */
    private String getLocationString(String file, int line) {
        return file + ":" + line;
    }

    /** Get a string to setup colored output in a terminal
     *
     * This method creates a string that contains the control sequence to
     * setup colored output according to the given level.
     *
     * @param level   The according log level
     *
     * @return The string containing the control sequence
     */
    private String getControlStringForColoredOutput(LogLevel level) {
        switch (level) {
        case ERROR:
            return "\033[;1;31m";
        case WARNING:
            return "\033[;1;34m";
        case DEBUG_WARNING:
            return "\033[;2;33m";
        case DEBUG:
            return "\033[;2;32m";
        default:
            return "\033[;0m";
        }
    }

//----------------------------------------------------------------------
// Public methods
//----------------------------------------------------------------------

//  /** The dtor of LoggingDomain
//   */
//  protected void finalize() {
//
//  }

    /** Get the full qualified name of this domain
     *
     * Each domain has a full qualified name consisting of its parent's name
     * and the local part that was given at creation time.
     *
     * @return The full qualified domain name
     */
    public String getName() {
        return configuration.name;
    }

    /** Get configuration status of this domain's enabled flag
     *
     * If a domain is enabled it processes log messages that are not below a
     * specified min level. Otherwise it is totally quite.
     *
     * @return Whether the domain is enabled or not
     */
    boolean isEnabled() {
        return configuration.enabled;
    }

    /** Get configuration status of this domain's print_time flag
     *
     * The current time is prepended to messages of this domain if the
     * print_time flag is set.
     *
     * @return Whether the print_time flag is set or not.
     */
    boolean getPrintTime() {
        return configuration.printTime;
    }

    /** Get configuration status of this domain's print_name flag
     *
     * The name of this domain prepended to messages of this domain if its
     * print_name flag is set.
     *
     * @return Whether the print_name flag is set or not.
     */
    boolean getPrintName() {
        return configuration.printName;
    }

    /** Get configuration status of this domain's print_level flag
     *
     * The level of each message is contained in the output of this domain
     * if the print_level flag is set.
     *
     * @return Whether the print_level flag is set or not.
     */
    boolean getPrintLevel() {
        return configuration.printLevel;
    }

    /** Get configuration status of this domain's print_location flag
     *
     * The location given to each message is contained in the output of this
     * domain if the print_location flag is set.
     *
     * @return Whether the print_location flag is set or not.
     */
    boolean getPrintLocation() {
        return configuration.printLocation;
    }

    /** Get the minimal log level a message must have to be processed
     *
     * Each message has a log level that must not below the configured limit to be processed.
     *
     * @return The configured minimal log level
     */
    LogLevel getMaxMessageLevel() {
        return configuration.maxMessageLevel;
    }

    /** Get the mask representing which streams are used for message output
     *
     * For message output several streams can be used. This bitmask configures
     * which of them are enabled.
     *
     * @return The bitmask that contains the enabled message streams
     */
    /*inline const eLogStreamMask GetStreamMask() const
    {
      return this->configuration->stream_mask;
    }*/

//  /** Get a message stream from this domain
//   *
//   * This method is the streaming interface to this logging domain.
//   * It must be used for every output using operator <<.
//   * The method then depending on the domain's configuration chooses
//   * a stream, prints the prefix that should be prepended to every
//   * message and returns the stream to process further input given as
//   * operator << cascade in the user's program.
//   * To properly specify the arguments of this method consider using
//   * the macros defined in rrlib/logging/definitions.h
//   *
//   * @param description   A string that describes the global context of the message
//   * @param function      The name of the function that contains the message (__FUNCTION__)
//   * @param file          The file that contains the message
//   * @param line          The line that contains the message
//   * @param level         The log level of the message
//   *
//   * @return A reference to the stream that can be used for the remaining message parts
//   */
//  inline tLoggingStreamProxy GetMessageStream(const char *description, const char *function, const char *file, unsigned int line, eLogLevel level) const
//  {
//    tLoggingStreamProxy stream_proxy(this->stream);
//    this->streamBuffer.Clear();
//    if (level < this->GetMinMessageLevel() || !this->IsEnabled())
//    {
//      return stream_proxy;
//    }
//    this->SetupOutputStream(this->configuration->stream_mask);
//
//    if (this->GetPrintTime())
//    {
//      this->stream << this->GetTimeString();
//    }
//    this->SetupOutputStream(this->configuration->stream_mask & ~(eLSM_FILE | eLSM_COMBINED_FILE));
//    this->stream << this->GetControlStringForColoredOutput(level);
//    this->SetupOutputStream(this->configuration->stream_mask);
//
//#ifndef _RRLIB_LOGGING_LESS_OUTPUT_
//    if (this->GetPrintName())
//    {
//      this->stream << this->GetNameString();
//    }
//    if (this->GetPrintLevel())
//    {
//      this->stream << this->GetLevelString(level);
//    }
//#endif
//    this->stream << description << "::" << function << " ";
//#ifndef _RRLIB_LOGGING_LESS_OUTPUT_
//    if (this->GetPrintLocation())
//    {
//      this->stream << this->GetLocationString(file, line);
//    }
//#endif
//    this->stream << ">> ";
//    this->SetupOutputStream(this->configuration->stream_mask & ~(eLSM_FILE | eLSM_COMBINED_FILE));
//    this->stream << "\033[;0m";
//    this->SetupOutputStream(this->configuration->stream_mask);
//
//    return stream_proxy;
//  }
//
//    /** A printf like variant of using logging domains for message output
//    *
//    * Instead of using operator << to output messages this method can be
//    * used. It then itself uses printf to format the given message and
//    * streams the result through the result obtained from GetMessageStream.
//    * That way the message prefix is only generated in one place and - more
//    * important - the underlying technique is the more sane one from
//    * iostreams instead of file descriptors.
//    * Apart from that: iostreams and file descriptors can not be mixed. So
//    * a decision had to be made.
//    *
//    * @param level         The log level of the message
//    * @param callerDescription Description of calling object or context
//    * @param msg           The message to output
//    */
//    public void log(LogLevel level, String callerDescription, String msg) {
//        log(level, callerDescription, msg, null, 2);
//    }
//
//    /** A printf like variant of using logging domains for message output
//    *
//    * Instead of using operator << to output messages this method can be
//    * used. It then itself uses printf to format the given message and
//    * streams the result through the result obtained from GetMessageStream.
//    * That way the message prefix is only generated in one place and - more
//    * important - the underlying technique is the more sane one from
//    * iostreams instead of file descriptors.
//    * Apart from that: iostreams and file descriptors can not be mixed. So
//    * a decision had to be made.
//    *
//    * @param level         The log level of the message
//    * @param callerDescription Description of calling object or context
//    * @param msg           The message to output
//    * @param e             Exception to output
//    */
//    public void log(LogLevel level, String callerDescription, String msg, Exception e) {
//        log(level, callerDescription, msg, e, 2);
//    }
//
//    /** A printf like variant of using logging domains for message output
//    *
//    * Instead of using operator << to output messages this method can be
//    * used. It then itself uses printf to format the given message and
//    * streams the result through the result obtained from GetMessageStream.
//    * That way the message prefix is only generated in one place and - more
//    * important - the underlying technique is the more sane one from
//    * iostreams instead of file descriptors.
//    * Apart from that: iostreams and file descriptors can not be mixed. So
//    * a decision had to be made.
//    *
//    * @param level         The log level of the message
//    * @param callerDescription Description of calling object or context
//    * @param e             Exception to output
//    */
//    public void log(LogLevel level, String callerDescription, Exception e) {
//        log(level, callerDescription, "", e, 2);
//    }

    /** A printf like variant of using logging domains for message output
     *
     * Instead of using operator << to output messages this method can be
     * used. It then itself uses printf to format the given message and
     * streams the result through the result obtained from GetMessageStream.
     * That way the message prefix is only generated in one place and - more
     * important - the underlying technique is the more sane one from
     * iostreams instead of file descriptors.
     * Apart from that: iostreams and file descriptors can not be mixed. So
     * a decision had to be made.
     *
     * @param level         The log level of the message
     * @param callFrame     Stack trace element of caller
     * @param caller        Description of calling object or context
     * @param msg           The message to output
     * @param callerStackIndex Stack index of caller (advanced feature)
     */
    public void log(LogLevel level, StackTraceElement callFrame, Object callerDescription, Object msg, int callerStackIndex) {

        if (level.ordinal() > getMaxMessageLevel().ordinal() || !isEnabled()) {
            return;
        }

        // extract data from caller
        if (callFrame == null) {
            callFrame = new Throwable().getStackTrace()[callerStackIndex];
        }
        String file = callFrame.getFileName();
        int line = callFrame.getLineNumber();
        String function = callFrame.getMethodName();

        // produce string to output
        synchronized (this) {
            setupOutputStream();
            buffer.delete(0, buffer.length());
            if (getPrintTime()) {
                buffer.append(getTimeString()).append(" ");
            }

            if (getPrintName()) {
                buffer.append("[").append(getNameString()).append("] ");
            }
            if (getPrintLevel()) {
                buffer.append(getLevelString(level)).append(" ");
            }
            buffer.append(callerDescription.toString()).append("::").append(function).append("()");
            if (getPrintLocation()) {
                buffer.append(" (").append(getLocationString(file, line)).append(")");
            }
            buffer.append(" >> ").append(msg.toString());

            String nonColoredOutput = buffer.toString();
            String coloredOutput = getControlStringForColoredOutput(level) + nonColoredOutput + "\033[;0m";

            for (PrintStream ps : outputStreams) {
                if (ps instanceof FileStream || (!COLORED_CONSOLE_OUTPUT)) {
                    ps.println(nonColoredOutput);
                } else {
                    ps.println(coloredOutput);
                }
                if (msg instanceof Exception) {
                    ((Exception)msg).printStackTrace(ps);
                }
            }
        }
    }

    /**
     * Convenience method.
     * Calls tLoggingDomainRegistry::GetSubDomain(...)
     *
     * @param name Name of sub domain
     */
    public LogDomain getSubDomain(String name) {
        return LogDomainRegistry.getInstance().getSubDomain(name, this);
    }

    public LogStream getLogStream(LogLevel level, String description) {
        return new LogStream(level, this, description);
    }

    public String toString() {
        return getName();
    }
}
