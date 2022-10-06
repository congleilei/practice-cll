package com.cll.cllcat;

import com.cll.servlet.CllRequest;
import com.cll.servlet.CllResponse;
import com.cll.servlet.CllServlet;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.internal.StringUtil;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class CllCatHandler extends ChannelInboundHandlerAdapter {

  private volatile String staticFilePath;

  private Map<String, CllServlet> nameToServletMap;//线程安全  servlet--> 对象
  private Map<String, String> nameToClassNameMap;//线程不安全  servlet--> 全限定名称

  public CllCatHandler(Map<String, CllServlet> nameToServletMap,
      Map<String, String> nameToClassNameMap) {
    this.nameToServletMap = nameToServletMap;
    this.nameToClassNameMap = nameToClassNameMap;
  }

  private void getStaticFile(String basePackage, String fileName) {
    // 获取指定包中的资源
    URL resource = this.getClass().getClassLoader()
        // com.abc.webapp  =>  com/hero/webapp
        .getResource(basePackage.replaceAll("\\.", "/"));
    // 若目录中没有任何资源，则直接结束
    if (resource == null) {
      return;
    }

    // 将URL资源转换为File资源
    File dir = new File(resource.getFile());
    // 遍历指定包及其子孙包中的所有文件，查找所有.class文件
    for (File file : dir.listFiles()) {
      if (file.isDirectory()) {
        // 若当前遍历的file为目录，则递归调用当前方法
        if (!"com".equals(file.getName())) {
          getStaticFile(
              "".equals(basePackage) ? file.getName() : basePackage + "." + file.getName(),
              fileName);
        }
      } else if (file.getName().equals(fileName)) {
        staticFilePath = file.getPath();
      }
    }
  }

  /**
   * 读文件
   */
  private byte[] readFile(File file) {
    //字节数组内存流，防止文件过大读取异常
    /*
     * 1、用于操作字节数组的流对象，其实它们就是对应设备为内存的流对象。
     * 2、该流的关闭是无效的，因为没有调用过系统资源。
     * 3、按照流的读写思想操作数组中元素。
     */
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    byte[] bt = new byte[10 * 1024];
    int length = 0;
    try (
        InputStream in = new FileInputStream(file)
    ) {
      while ((length = in.read(bt, 0, bt.length)) != -1) {
        //先读到内存流中做缓冲
        baos.write(bt, 0, length);
        baos.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return baos.toByteArray();
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    if (msg instanceof HttpRequest) {
      HttpRequest request = (HttpRequest) msg;
      String uri = request.uri();
      //静态文件
      if (uri.contains(".")) {
        String fileName = uri.substring(uri.lastIndexOf("/") + 1);
        String fileType = fileName.substring(fileName.lastIndexOf(".") + 1);
        String contentType = "text/json";
        if ("html".equalsIgnoreCase(fileType) || "htm".equalsIgnoreCase(fileType)) {
          contentType = "text/html";
        } else if ("css".equalsIgnoreCase(fileType)) {
          contentType = "text/css";
        } else if ("js".equalsIgnoreCase(fileType)) {
          contentType = "text/javascript";
        } else if ("jpeg".equalsIgnoreCase(fileType) || "jpg".equalsIgnoreCase(fileType)) {
          contentType = "image/jpeg";
        } else if ("png".equalsIgnoreCase(fileType)) {
          contentType = "image/png";
        } else if ("gif".equalsIgnoreCase(fileType)) {
          contentType = "image/gif";
        }
        getStaticFile("", fileName);
        System.out.println("filePath:" + staticFilePath);
        if(!StringUtil.isNullOrEmpty(staticFilePath)) {

          Path path = Paths.get(staticFilePath);
          byte[] data = Files.readAllBytes(path);
          String result = new String(data, "utf-8");
          CllServlet servlet = new DefaultCllServlet();
          CllRequest req = new HttpCllRequest(request);
          CllResponse res = new HttpCllResponse(request, ctx, contentType);
          servlet.doStatic(req, res, result);
        }
      } else {

        // 从请求中解析出要访问的Servlet名称
        //aaa/bbb/twoservlet?name=aa
        String servletName = "";
        if (uri.contains("?") && uri.contains("/")) {
          servletName = uri.substring(uri.lastIndexOf("/") + 1, uri.indexOf("?"));
        }

        CllServlet servlet = new DefaultCllServlet();
        //第一次访问，Servlet是不会被加载的
        //初始化加载的只是类全限定名称，懒加载
        //如果访问Servlet才会去初始化它对象
        if (nameToServletMap.containsKey(servletName)) {
          servlet = nameToServletMap.get(servletName);
        } else if (nameToClassNameMap.containsKey(servletName)) {
          // double-check，双重检测锁：为什么要在锁前判断一次，还要在锁后继续判断一次？
          if (nameToServletMap.get(servletName) == null) {
            synchronized (this) {
              if (nameToServletMap.get(servletName) == null) {
                // 获取当前Servlet的全限定性类名
                String className = nameToClassNameMap.get(servletName);
                // 使用反射机制创建Servlet实例
                servlet = (CllServlet) Class.forName(className).newInstance();
                // 将Servlet实例写入到nameToServletMap
                nameToServletMap.put(servletName, servlet);
              }
            }
          }
        } //  end-else if

        // 代码走到这里，servlet肯定不空
        CllRequest req = new HttpCllRequest(request);
        CllResponse res = new HttpCllResponse(request, ctx, "text/json");
        // 根据不同的请求类型，调用servlet实例的不同方法
        if (request.method().name().equalsIgnoreCase("GET")) {
          servlet.doGet(req, res);
        } else if (request.method().name().equalsIgnoreCase("POST")) {
          servlet.doPost(req, res);
        }
      }
      ctx.close();
    }

  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    cause.printStackTrace();
    ctx.close();
  }
}
