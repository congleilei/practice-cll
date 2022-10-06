package com.cll.cllcat;

public class CllCat {
  public static void run(String[] args) throws Exception {
    CllCatServer server = new CllCatServer("com.cll.webapp");
    server.start();
  }
}
