package com.corgi;

import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableDubboConfiguration
public class CorgiActivityServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CorgiActivityServiceApplication.class, args);
	}

}
