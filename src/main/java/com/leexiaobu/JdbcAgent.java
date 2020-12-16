package com.leexiaobu;

import com.leexiaobu.collect.JdbcCollect;
import java.lang.instrument.Instrumentation;


/**
 * @author Leexiaobu
 * @date 2020-10-17 23:41:43
 */
public class JdbcAgent {

  public static void premain(String args, Instrumentation instrumentation) {
    System.out.println("pre main master");
    String target = "com.mysql.cj.jdbc.NonRegisteringDriver";
    instrumentation.addTransformer(
        (loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
          try {
//            System.out.println(className);
            return new JdbcCollect().transform(loader, className);
          } catch (Exception e) {
            e.printStackTrace();
          }
          return null;
        }, true);
  }
}
