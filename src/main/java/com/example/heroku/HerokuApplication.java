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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet; // For results from SQL select query.
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList; // Object for storing rectangles -> great to pass into model.
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
  String index(Map<String, Object> model) { // you can add the thymeleaf thingy, in fact, its a must if you want dynamic webpage
    try (Connection connection = dataSource.getConnection()) {
      Statement stmt = connection.createStatement();
      String query = "SELECT * FROM rectangles";
      ResultSet rs = stmt.executeQuery(query);

      // loop through result set, making a list of Rectangle objects
      ArrayList<Rectangle> rectangles = new ArrayList<>();
      while (rs.next()) {
        Rectangle rect = new Rectangle();
        rect.setId(rs.getInt("id"));
        rect.setName(rs.getString("name"));
        rect.setColor(rs.getString("color"));
        rect.setWidth(rs.getInt("width"));
        rect.setHeight(rs.getInt("height"));
        rectangles.add(rect);
      }

      // display contents of db in HTML table
      model.put("rectangles", rectangles);

      // debug af
      for (Rectangle rectangle : rectangles) {
        System.out.println(rectangle);
      }

      // have a blank rectangle ready for post requests
      Rectangle rectangle = new Rectangle();
      model.put("rectangle", rectangle);
      return "index";
    }

    catch (SQLException e) {
      System.out.println("Connection failed");
      model.put("message", e.getMessage());
      return "error";
    }
  }

  @PostMapping("/submit") // triggered by submit button on form
  String handleSubmit(Map<String, Object> model, Rectangle rect) {
    System.out.println("Post request detected");
    try (Connection connection = dataSource.getConnection()) {
      System.out.println("Connection succeeded");
      String name = rect.getName();
      String color = rect.getColor();
      Integer width = rect.getWidth();
      Integer height = rect.getHeight();
      String values = String.format("('%s','%s', '%d', '%d')", name, color, width, height);
      System.out.println("Submitting rectangle " + values);
      Statement stmt = connection.createStatement();
      stmt.executeUpdate(
          "CREATE TABLE IF NOT EXISTS rectangles (id SERIAL, name TEXT, color TEXT, width INTEGER, height INTEGER)");
      String addRect = "INSERT INTO rectangles (name, color, width, height) VALUES " + values;
      stmt.executeUpdate(addRect);
      return "redirect:/"; // so that we run all of the code in @GetMapping("/")

    } catch (SQLException e) {
      System.out.println("Connection failed");
      model.put("message", e.getMessage());
      return "error";
    }
  }

  // Delete specific rectangle using button-> rectangle id is path variable
  @DeleteMapping("/delrect/{id}")
  String deleteRectId(Map<String, Object> model, @PathVariable String id) {

    System.out.println("Delete request detected");
    try (Connection connection = dataSource.getConnection()) {
      System.out.println("Connection succeeded");
      Statement stmt = connection.createStatement();
      stmt.executeUpdate(String.format("DELETE FROM rectangles WHERE id=%s", id));
      return "redirect:/"; // we go to a path well traveled ;)

    } catch (Exception e) {
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
