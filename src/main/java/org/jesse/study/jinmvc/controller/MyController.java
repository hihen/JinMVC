package org.jesse.study.jinmvc.controller;

import org.jesse.study.jinmvc.annotate.Autowired;
import org.jesse.study.jinmvc.annotate.Controller;
import org.jesse.study.jinmvc.annotate.RequestMapping;
import org.jesse.study.jinmvc.annotate.RequestParam;
import org.jesse.study.jinmvc.service.MyService;

@Controller
public class MyController {
    @Autowired("myService")
    private MyService myService;

    @RequestMapping("/test")
    public String test(@RequestParam("name") String name, @RequestParam("age") String age) {
        return myService.test(name, age);
    }

    @RequestMapping("/test1")
    public String test1() {
        return myService.test1();
    }

}
