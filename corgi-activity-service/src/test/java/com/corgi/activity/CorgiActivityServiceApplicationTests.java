package com.corgi.activity;

import com.corgi.activity.entity.CorgiActivity;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.query.Criteria;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//@SpringBootTest
class CorgiActivityServiceApplicationTests {

	@Test
	static void contextLoads() {
		Field[] fields = CorgiActivity.class.getDeclaredFields();
		for (int i = 0; i < fields.length; i++) {
			Field field = fields[i];
			field.setAccessible(true);

			System.out.println(field.getName()+" "+field.getType() + int.class.equals(field.getType()));
		}
	}

	public static void main(String[] args) {
		System.out.println("[\"1\", \"2\",'3']".replaceAll("[\\]\\[\"'\\s]",""));
	}

}
