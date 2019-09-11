package com.flexicore.installer.utilities.classfinder;

import com.flexicore.installer.interfaces.IInstallationTask;

import java.util.ArrayList;
import java.util.List;

public class TestInterface {
 public static   List test () {
       JavaClassFinder classFinder = new JavaClassFinder();
       List<Class<? extends IInstallationTask>> classes = classFinder.findAllMatchingTypes(IInstallationTask.class);
       return classes;
   }
}
