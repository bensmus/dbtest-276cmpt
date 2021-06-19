/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.heroku;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

@Controller
@SpringBootApplication
public class HerokuApplication {

  @Value("${spring.datasource.url}")
  private String dbUrl;

  @Autowired
  private DataSource dataSource;

  public static void main(String[] args) throws Exception {
    SpringApplication.run(HerokuApplication.class, args);
  }

  @GetMapping("/")
  String index(Map<String, Object> model) { // you can add the thymeleaf thingy
    Rectangle rectangle = new Rectangle();
    model.put("rectangle", rectangle);
    return "index";
  }

  @PostMapping("/") // triggered by submit button on form
  String handleSubmit(Map<String, Object> model, Rectangle rect) {
    System.out.println("Post request detected");
    try (Connection connection = dataSource.getConnection()) {
      System.out.println("Connection succeeded");
      String name = rect.getName();
      String color = rect.getColor();
      Integer width = rect.getWidth();
      Integer height = rect.getHeight();
      String values = String.format("(\'%s\',\'%s\', \'%d\', \'%d\')", name, color, width, height);
      System.out.println("Submitting rectangle " + values);
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS rectangles (name TEXT, color TEXT, width INTEGER, height INTEGER)");
      String addRect = "INSERT INTO rectangles (name, color, width, height) VALUES " + values + ";";
      stmt.executeUpdate(addRect);
      return "index";

    } catch (SQLException e) {
      System.out.println("Connection failed");
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @GetMapping("/db_time")
  String db_time(Map<String, Object> model) { // function can be called anything
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      stmt.executeUpdate("CREATE TABLE IF NOT EXISTS ticks (tick timestamp)");
      stmt.executeUpdate("INSERT INTO ticks VALUES (now())");
      ResultSet rs = stmt.executeQuery("SELECT tick FROM ticks");

      ArrayList<String> output = new ArrayList<String>();
      while (rs.next()) {
        output.add("Read from DB: " + rs.getTimestamp("tick"));
      }

      model.put("records", output); // this is for thymeleaf
      return "db_time";
    } catch (Exception e) {
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @GetMapping("/rectangle")
  String rectangle(Map<String, Object> model) {
    return "rectangle";
  }

  @Bean
  public DataSource dataSource() throws SQLException {
    if (dbUrl == null || dbUrl.isEmpty()) {
      return new HikariDataSource();
    } else {
      HikariConfig config = new HikariConfig();
      System.out.println("CUSTOM OUTPUT-- Database URL: " + dbUrl);
      config.setJdbcUrl(dbUrl);
      return new HikariDataSource(config);
    }
  }

}
