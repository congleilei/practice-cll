package com.cll.servlet;

/**
 * Servlet规范之响应规范
 */
public interface CllResponse {
    // 将响应写入到Channel
    void write(String content) throws Exception;
//    void outResult();outResult
}
