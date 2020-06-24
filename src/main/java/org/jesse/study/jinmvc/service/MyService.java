package org.jesse.study.jinmvc.service;

import org.jesse.study.jinmvc.annotate.Service;

@Service("myService")
public class MyService {

    public String test(String name, String age) {
        return new StringBuffer().append("My name is ").append(name).append(",age is ").append(age).toString();
    }

    public String test1() {
        return "i love u so much, yy!";
    }
}
