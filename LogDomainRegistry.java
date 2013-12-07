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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * tLoggingDomainRegistry is a central management facility for logging
 * domains and their configuration.
 * In RRLib logging messages can be send via several logging domains.
 * These have to be created and maintained using a single instance of
 * tLoggingDomainRegistry. Thus, this class implements the singleton
 * pattern and contains a list of logging domains and configurations
 * that either were created along with active domains or were configured
 * by the user from a file or calling the appropriate methods.
 *
 * The central management facility for logging domains and their configuration
 * In RRLib logging messages can be send via several logging domains.
 * These have to be created and maintained using a single instance of
 * this class. Thus, this class implements the singleton pattern and
 * contains a list of active logging domains and configurations that
 * either were created along with active domains or were configured by
 * the user from a file or calling the appropriate methods.
 *
 * @author Max Reichardt
 * @author Tobias Föhst
 */
public class LogDomainRegistry {

    /** Ctor of tLoggingDomainRegistry
     *
     * Private default ctor for singleton pattern
     */
    LogDomainRegistry() {
        LogDomainConfiguration conf = new LogDomainConfiguration(".");
        conf.enabled = true;
        domainConfigurations.add(conf);
        domains.add(new LogDomain(conf));
    }

    /** Get the index of the domain with the given name
     *
     * Helper method that implements the lookup for existing domains
     *
     * @param name   The name of the wanted domain
     *
     * @return The index of the domain if found. If not, the methods returns the size of the domain vector.
     */
    private int getDomainIndexByName(String name) {
        for (int i = 0; i < domains.size(); i++) {
            if (domains.get(i).getName().equals(name)) {
                return i;
            }
        }
        return domains.size();
    }

    /** Get a configuration object for a domain with the given name
     *
     * This methods implements the lookup for existing domain names and
     * creates a new configuration object for new names.
     *
     * @param name   The name of the domain to be configured
     *
     * @return The wanted domain configuration as a shared pointer
     */
    private LogDomainConfiguration getConfigurationByName(String name) {
        for (LogDomainConfiguration conf : domainConfigurations) {
            if (conf.name.equals(name)) {
                return conf;
            }
        }
        LogDomainConfiguration conf = new LogDomainConfiguration(name);
        domainConfigurations.add(conf);
        return conf;
    }

    /** Update configuration the subtree of a domain for recursion
     *
     * If the configuration of one domain is changed start update of its
     * subtree. This method should always be called because the decision
     * about recursive configuration is done within its call.
     * That keeps update methods simpler.
     *
     * @param name   The name of the updated domain
     */
    private void propagateDomainConfigurationToChildren(String name) {
        int i = getDomainIndexByName(name);
        if (i != domains.size()) {
            for (LogDomain dom : domains.get(i).children) {
                dom.configureSubTree();
            }
        }
    }

    /** Add a domain configuration from a given XML node
    *
    * This method configures a logging domain using the values specified in
    * the given XML node. It also implements recursive configuration in case
    * of nested nodes.
    *
    * @param node    The XML node that contains the configuration
    *
    * @return Whether the domain was successfully configured or not
    */
    boolean addConfigurationFromXMLNode(Node node) {
        return addConfigurationFromXMLNode(node, "");
    }

    static final List<String> levelNames = Arrays.asList("user", "error", "warning", "debug_warning", "debug", "debug_verbose_1", "debug_verbose_2", "debug_verbose_3");
    static final List<String> streamNames = Arrays.asList("stdout", "stderr", "file", "combined_file");

    /** Add a domain configuration from a given XML node
     *
     * This method configures a logging domain using the values specified in
     * the given XML node. It also implements recursive configuration in case
     * of nested nodes.
     *
     * @param node    The XML node that contains the configuration
     * @param parentName   For recursive calls the current domain name is build from parent_name and domain_name
     *
     * @return Whether the domain was successfully configured or not
     */
    boolean addConfigurationFromXMLNode(Node node, String parentName) {

        assert(node.getNodeName().equals("domain"));

        String prefix = (parentName == "." ? "" : parentName) + ".";
        String nodeName = node.getAttributes().getNamedItem("name").getNodeValue().trim();
        String name = prefix + (parentName.length() == 0 && nodeName == "global" ? "" : nodeName);

        Node item = node.getAttributes().getNamedItem("configures_sub_tree");
        if (item != null) {
            setDomainConfiguresSubTree(name, item.getNodeValue().trim().toLowerCase().equals("true"));
        }

        item = node.getAttributes().getNamedItem("enabled");
        if (item != null) {
            setDomainIsEnabled(name, item.getNodeValue().trim().toLowerCase().equals("true"));
        }

        item = node.getAttributes().getNamedItem("print_time");
        if (item != null) {
            setDomainPrintsTime(name, item.getNodeValue().trim().toLowerCase().equals("true"));
        }

        item = node.getAttributes().getNamedItem("print_name");
        if (item != null) {
            setDomainPrintsName(name, item.getNodeValue().trim().toLowerCase().equals("true"));
        }

        item = node.getAttributes().getNamedItem("print_level");
        if (item != null) {
            setDomainPrintsLevel(name, item.getNodeValue().trim().toLowerCase().equals("true"));
        }

        item = node.getAttributes().getNamedItem("print_location");
        if (item != null) {
            setDomainPrintsLocation(name, item.getNodeValue().trim().toLowerCase().equals("true"));
        }

        item = node.getAttributes().getNamedItem("max_level");
        if (item != null) {
            setDomainMinMessageLevel(name, LogLevel.values()[levelNames.indexOf(item.getNodeValue().trim().toLowerCase())]);
        }

        boolean streamConfigured = false;
        item = node.getAttributes().getNamedItem("stream");
        if (item != null) {
            streamConfigured = true;
            setDomainStreamMask(name, LogStreamOutput.values()[streamNames.indexOf(item.getNodeValue().trim().toLowerCase())]);
        }


        ArrayList<LogStreamOutput> streamMask = new ArrayList<LogStreamOutput>();
        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            if (child.getNodeName().equals("stream")) {
                if (streamConfigured) {
                    System.err.println("RRLib Logging: tLoggingDomainRegistry::AddConfigurationFromXMLNode >> Stream already configured in domain element!");
                    return false;
                }
                item = node.getAttributes().getNamedItem("output");
                streamMask.add(LogStreamOutput.values()[streamNames.indexOf(item.getNodeValue().trim().toLowerCase())]);
            }
        }
        if (streamMask.size() > 0) {
            setDomainStreamMask(name, streamMask.toArray(new LogStreamOutput[0]));
        }

        for (int i = 0; i < node.getChildNodes().getLength(); i++) {
            Node child = node.getChildNodes().item(i);
            if (child.getNodeName().equals("domain")) {
                if (!addConfigurationFromXMLNode(child, name)) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Get an instance of this class (singleton)
     *
     * Due to the singleton pattern this class has no public constructor.
     * The only way to get an object is to call this method.
     *
     * @return The only instance of this class that should exist
     */
    public static LogDomainRegistry getInstance() {
        return instance;
    }

    /** Get the default domain
     *
     * There is always on default domain registered that can be accessed
     * using this method as a shortcut.
     *
     * @return The default domain object
     */
    public static LogDomain getDefaultDomain() {
        return getInstance().domains.get(0);
    }

    /** Get a subdomain with given name and parent.
     *
     * This method can be used to access a subdomain of the given parent
     * with given name. It then implements the lookup and if the domain
     * was not found creates a new one and applies an eventually existing
     * configuration.
     *
     * @param name     The name of the subdomain (local part)
     * @param parent   The parent of the subdomain
     *
     * @return The found or newly created domain object as a shared pointer
     */
    public LogDomain getSubDomain(String name, LogDomain parent) {
        assert(name != null && name.length() > 0 && parent != null);
        String fullQualifiedDomainName = (parent != getDefaultDomain() ? (parent.getName() + ".") : "") + name;
        int i = getDomainIndexByName(fullQualifiedDomainName);
        if (i == domains.size()) {
            LogDomainConfiguration configuration = getConfigurationByName(fullQualifiedDomainName);
            LogDomain ld = new LogDomain(configuration, parent);
            domains.add(ld);
            return ld;
        }
        return domains.get(i);

    }

    /** Set a prefix for filenames that are created as log
     *
     * If their output stream is set to eMS_FILE domains create a log
     * file with their name. Additionally, this name must have a prefix
     * to distinguish between programs, processes or runs.
     *
     * The method could be called with basename(argv[0]) in main, for example.
     *
     * @param fileNamePrefix   The string the should be used as prefix
     */
    public void setOutputFileNamePrefix(String fileNamePrefix) {
        assert(fileNamePrefix.length() > 0);
        this.fileNamePrefix = fileNamePrefix;
    }

    /** Get the configured file name prefix
     *
     * Get the file name prefix that was configured
     *
     * @return the stored prefix
     */
    public String getOutputFileNamePrefix() {
        return fileNamePrefix;
    }

// TODO: maybe..
// /** Get the length of the longest full qualified domain name
//  *
//  * This method can be used for formatting user dialogs
//  *
//  * @return The length of the longest domain name
//  */
// public int GetMaxDomainNameLength() {
//
// }

    /** Enable a given domain and switch recursion on or off
    *
    * This is a shortcut for setting the configuration of the domain's
    * sub tree and enabling it.
    *
    * @param name            The full qualified name of the domain
    */
    public void enableDomain(String name) {
        enableDomain(name, false);
    }

    /** Enable a given domain and switch recursion on or off
     *
     * This is a shortcut for setting the configuration of the domain's
     * sub tree and enabling it.
     *
     * @param name            The full qualified name of the domain
     * @param withSubTree   Switch recursion on or off
     */
    public void enableDomain(String name, boolean withSubTree) {
        setDomainConfiguresSubTree(name, withSubTree);
        setDomainIsEnabled(name, true);
    }

    /** Disable a given domain and switch recursion on or off
    *
    * This is a shortcut for setting the configuration of the domain's
    * sub tree and disabling it.
    *
    * @param name            The full qualified name of the domain
    */
    public void disableDomain(String name) {
        disableDomain(name, false);
    }

    /** Disable a given domain and switch recursion on or off
     *
     * This is a shortcut for setting the configuration of the domain's
     * sub tree and disabling it.
     *
     * @param name            The full qualified name of the domain
     * @param withSubTree   Switch recursion on or off
     */
    public void disableDomain(String name, boolean withSubTree) {
        setDomainConfiguresSubTree(name, withSubTree);
        setDomainIsEnabled(name, false);
    }

    /** Set if the domain configures its subtree or not
     *
     * If set to true every configuration update to the given domain
     * will be propagated to its subtree.
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainConfiguresSubTree(String name, boolean value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.configureSubTree = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set if the domain is enabled or not
     *
     * If a domain is not enabled, none of its messages will be visible
     * regardless of its min message level.
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainIsEnabled(String name, boolean value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.enabled = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set if the domain should prepend the current time to each message
     *
     * If set to true every message will start with the current time
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainPrintsTime(String name, boolean value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.printTime = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set if the domain should prepend messages with its name
     *
     * If set to true every message will include the full qualified domain
     * name
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainPrintsName(String name, boolean value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.printName = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set if the domain should prepend messages with their level
     *
     * If set to true every message will include its level
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainPrintsLevel(String name, boolean value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.printLevel = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set if the domain should prepend the message with its location
     *
     * If set to true every message will include its location in code.
     * This is extremely helpful during debugging phases.
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainPrintsLocation(String name, boolean value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.printLocation = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set the minimal message level of the given domain
     *
     * The output of each message that has a level below the given value
     * will be suppressed. Default is eML_MEDIUM or eML_HIGH depending on
     * compile flags.
     *
     * @param name    The full qualified name of the domain
     * @param value   The new value of the setting
     */
    public void setDomainMinMessageLevel(String name, LogLevel value) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        configuration.maxMessageLevel = value;
        propagateDomainConfigurationToChildren(name);
    }

    /** Set the output stream that should be used by the given domain
     *
     * If set to true every configuration update to the given domain
     * will be propagated to its subtree.
     *
     * @param name   The full qualified name of the domain
     * @param outputs   The new value of the setting
     */
    public void setDomainStreamMask(String name, LogStreamOutput... outputs) {
        LogDomainConfiguration configuration = getConfigurationByName(name);
        synchronized (configuration) {
            configuration.streamMask = outputs;
            configuration.streamMaskRevision = LogDomainConfiguration.streamMaskRevisionGen.incrementAndGet();
            propagateDomainConfigurationToChildren(name);
        }
    }

    /** Read domain configuration from a given XML file
     *
     * The overall configuration of the logging domains tends to be
     * too complicated for a classical command line option interface.
     * Therefore, it is possible to specify the configuration in form
     * of an XML file following the DTD -//RRLIB//logging
     *
     * @param fileName   The XML file to be read
     *
     * @return Whether the configuration could be read and applied or not
     */
    public boolean configureFromFile(String fileName) {
        try {
            // parse XML
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dbuilder = factory.newDocumentBuilder();
            Document doc = dbuilder.parse(fileName);

            return configureFromXMLNode(doc.getFirstChild());
        } catch (Exception e) {
            System.err.println("RRLib Logging: tLoggingDomainRegistry::ConfigureFromFile >> " + e.getMessage());
            return false;
        }
    }

    /** Read domain configuration from a given XML node
     *
     * Instead of reading and parsing an XML file dedicated to configure
     * logging domains this method can be used after externally parsing
     * a document that contains an rrlib_logging node following the DTD
     * -//RRLIB//logging
     *
     * @param node   The XML node containing the configuration
     *
     * @return Whether the configuration could be applied or not
     */
    public boolean configureFromXMLNode(Node node) {
        if (node.getNodeName() != "rrlib_logging") {
            System.err.println("RRLib Logging: tLoggingDomainRegistry::ConfigureFromXMLNode >> Unexpected content (Not an rrlib_logging tree)");
            return false;
        }

        try {
            for (int i = 0; i < node.getChildNodes().getLength(); i++) {
                Node child = node.getChildNodes().item(i);
                if (child.getNodeName().equals("domain")) {
                    if (!addConfigurationFromXMLNode(child)) {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("RRLib Logging: tLoggingDomainRegistry::ConfigureFromXMLNode >> " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Returns domain for specified package (creates it if necessary)
     *
     * @param package1 Package to obtain domain for
     * @return Log domain
     */
    public static LogDomain getDomainForPackage(Package package1) {
        LogDomain result = domainForPackageLookup.get(package1);
        if (result == null) {
            synchronized (LogDomain.class) {
                result = getDomainByQualifiedName(package1.getName());
                domainForPackageLookup.put(package1, result);
            }
        }
        return result;
    }

    /**
     * Returns domain for specified qualified name (. is used as separator)
     *
     * @param name Fully-qualified domain name
     * @return Log domain
     */
    public static LogDomain getDomainByQualifiedName(String name) {
        if (name.length() == 0 || (name.length() == 1 && name.charAt(0) == '.')) {
            return getDefaultDomain();
        }
        LogDomain result = domainForQualifiedNameLookup.get(name);
        if (result == null) {
            synchronized (LogDomain.class) {
                String[] names = name.split(".");
                int index = (names[0].length() == 0) ? 1 : 0;
                LogDomain current = getDefaultDomain();
                for (int i = index; i < names.length; i++) {
                    current = current.getSubDomain(names[i]);
                }
                result = current;

                domainForQualifiedNameLookup.put(name, result);
            }
        }
        return result;
    }


    private String fileNamePrefix;
    private ArrayList<LogDomain> domains = new ArrayList<LogDomain>();
    private ArrayList<LogDomainConfiguration> domainConfigurations = new ArrayList<LogDomainConfiguration>();

    private static final LogDomainRegistry instance = new LogDomainRegistry();

    /** Cache for package=>log domain lookup */
    private static final ConcurrentHashMap<Package, LogDomain> domainForPackageLookup = new ConcurrentHashMap<Package, LogDomain>();

    /** Cache for qualified name=>log domain lookup */
    private static final ConcurrentHashMap<String, LogDomain> domainForQualifiedNameLookup = new ConcurrentHashMap<String, LogDomain>();
}
