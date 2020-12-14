package com.leexiaobu.collect;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import javassist.*;
import com.leexiaobu.model.JdbcStatistics;

public class JdbcCollect {
  public byte[] transform(ClassLoader loader, String className) throws Exception {
    if (className == null || !target.equals(className.replace("/", "."))) {
      return null;
    }
    String replace = className.replace("/", ".");
    CtClass ctclass = buildClass(replace, loader);
    return ctclass.toBytecode();
  }

  String target = "com.mysql.cj.jdbc.NonRegisteringDriver";

  //connection  -> statement --> ResultSet 结果
  public CtClass buildClass(String className, ClassLoader loader)
      throws NotFoundException, CannotCompileException, IOException {
    if (!className.equals(target)) {
      throw new RuntimeException("fail param");
    }
    /*获取ClassPool*/
    ClassPool pool = new ClassPool();
    pool.insertClassPath(new LoaderClassPath(loader));
    CtClass ctclass = pool.get(className);
    /*拦截connect方法
     * 复制旧方法，生成新方法
     * 旧方法源码修改*/
    CtMethod oldMethod = ctclass
        .getMethod("connect",
            "(Ljava/lang/String;Ljava/util/Properties;)Ljava/sql/Connection;");
    CtMethod newMethod = CtNewMethod.copy(oldMethod, ctclass, null);
    newMethod.setName(newMethod.getName() + "$agent");
    ctclass.addMethod(newMethod);
    oldMethod.setBody(source);
    return ctclass;
  }

  //第一步生成Connection 代理
  final static String source = "{\n"
      + "        java.sql.Connection result=null;\n"
      + "       try {\n"
      + "            result=($w)connect$agent($$);\n"
      + "			result=  com.leexiaobu.collect.JdbcCollect.proxyConnection(result);"
      // 封装代理connection
      + "        } catch (Throwable e) {\n"
      + "            throw e;\n"
      + "        }finally{\n"
      + "        }\n"
      + "        return ($r) result;\n" +
      "}\n";

  // 利用jdk动态代理生成 将原有connect传过来，生成 Connection 代理类，通过自定的InvocationHandler
  public static Connection proxyConnection(Connection connection) {
    return (Connection) Proxy.newProxyInstance(JdbcCollect.class.getClassLoader(),
        new Class[]{Connection.class},
        new ConnectionHandler(connection));
  }

  public static class ConnectionHandler implements InvocationHandler {

    Connection target;

    public ConnectionHandler(Connection target) {
      this.target = target;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object result = null;
      // 正常返回对象
      result = method.invoke(target, args);
      // 拦截prepareStatement方法，修改PreparedStatement
      if ("prepareStatement".equals(method.getName())) {
        PreparedStatement statement = (PreparedStatement) result;
        JdbcStatistics stat = begin(target.getMetaData().getURL(), (String) args[0]);
        // 进一步代理 PreparedStatement，生成 PreparedStatement 代理类
        result = proxyStatement(statement, stat);
        // begin
      }
      return result;
    }


  }

  public static Statement proxyStatement(PreparedStatement statement, JdbcStatistics stat) {
    return (Statement) Proxy.newProxyInstance(JdbcCollect.class.getClassLoader(),
        new Class[]{PreparedStatement.class}, new StatementHandler(statement, stat));
  }


  public static class StatementHandler implements InvocationHandler {

    Statement statement;
    JdbcStatistics stat;

    public StatementHandler(Statement statement, JdbcStatistics stat) {
      this.statement = statement;
      this.stat = stat;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
      Object result = null;
      try {
        try {
          result = method.invoke(statement, args);
        } catch (InvocationTargetException e) {
          if (method.getName().equals("executeQuery")) {
            error(e.getTargetException(), stat);
          }
          throw e;
        }
      } catch (Throwable e) {
        throw e;
      }
      if (method.getName().equals("close")) {
        end(stat);
      }
      return result;
    }
  }


  public static JdbcStatistics begin(String jdbcUrl, String sql) {
    // jdbc统计对象
    System.out.println(sql);
    JdbcStatistics jdbcStatistics = new JdbcStatistics(jdbcUrl, sql);
    jdbcStatistics.setBeginTime(System.currentTimeMillis());
    return jdbcStatistics;
  }

  public static void error(Throwable error, JdbcStatistics stat) {
    stat.setError(error.getMessage());
    System.out.println(stat);
  }

  public static void end(JdbcStatistics stat) {
    stat.setUseTime(System.currentTimeMillis() - stat.getBeginTime());
    System.out.println(stat);
    System.out.println("end");
  }


}
