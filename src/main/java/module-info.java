module flexicore.installer {
    requires commons.cli;
    requires java.logging;
    requires org.pf4j;
    requires org.jgrapht.core;
    requires java.xml;
    requires java.base;
    requires java.desktop;
    requires java.naming;
    requires zt.zip;
    requires org.apache.commons.lang3;
    requires jdk.unsupported;
    requires commons.validator;
    requires jdk.jdwp.agent;
    requires java.sql;
    requires shortcuts;

    exports com.flexicore.installer.exceptions;
    exports com.flexicore.installer.model;
    exports com.flexicore.installer.utilities;

    exports com.flexicore.installer.runner;
    exports com.flexicore.installer.interfaces;

}
