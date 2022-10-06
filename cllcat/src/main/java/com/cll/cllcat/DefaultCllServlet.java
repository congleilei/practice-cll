package com.cll.cllcat;

import com.cll.servlet.CllRequest;
import com.cll.servlet.CllResponse;
import com.cll.servlet.CllServlet;

public class DefaultCllServlet extends CllServlet {

  @Override
  public void doGet(CllRequest request, CllResponse response) throws Exception {
    String uri = request.getUri();
    response.write("404 NOT FOUND \n" + uri);
  }

  @Override
  public void doPost(CllRequest request, CllResponse response) throws Exception {
    this.doGet(request,response);
  }

  @Override
  public void doStatic(CllRequest request, CllResponse response,String staticInfo) throws Exception {
    String uri = request.getUri();
    response.write(staticInfo);
  }

}
