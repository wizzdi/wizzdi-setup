package com.flexicore.installer.model;

public enum UserResponse {
    OK,CONTINUE,FORCESTOP,IGNORE,YES,NO,STOP;

    public static String getAll() {
        String result = null;
        for (UserResponse response: values()) {
          if (result==null)   {
             result=response.toString();
          }else {
              result+=","+response.toString();
          }
        }
       return result;
    }
}
