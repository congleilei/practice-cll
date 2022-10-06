package com.cll.webapp.test;

import com.cll.servlet.CllRequest;
import com.cll.servlet.CllResponse;
import com.cll.servlet.CllServlet;

public class TestServlet extends CllServlet {

  @Override
  public void doGet(CllRequest request, CllResponse response) throws Exception {
    String uri = request.getUri();
    String path = request.getPath();
    String method = request.getMethod();
    String name = request.getParameter("name");

    String content = "uri = " + uri + "\n" +
        "path = " + path + "\n" +
        "method = " + method + "\n" +
        "param = " + name;
    response.write(content);
  }

  @Override
  public void doPost(CllRequest request, CllResponse response) throws Exception {
    doGet(request, response);
  }

  @Override
  public void doStatic(CllRequest request, CllResponse response, String staticInfo)
      throws Exception {
    doGet(request, response);
  }
}
