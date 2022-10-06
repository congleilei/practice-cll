package com.cll.servlet;

/**
 * 定义Servlet规范
 */
public abstract class CllServlet {

    public abstract void doGet(CllRequest request, CllResponse response) throws Exception;
    public abstract void doPost(CllRequest request, CllResponse response) throws Exception;
    public abstract void doStatic(CllRequest request, CllResponse response,String staticInfo) throws Exception;
}
