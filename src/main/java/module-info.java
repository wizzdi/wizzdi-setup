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
    exports com.flexicore.installer.exceptions;
    exports com.flexicore.installer.model;
    exports com.flexicore.installer.runner;


}