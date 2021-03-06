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
			stmt.executeUpdate(
					"CREATE TABLE IF NOT EXISTS rectangles (id SERIAL, name TEXT, color TEXT, width INTEGER, height INTEGER)");
			String query = "SELECT * FROM rectangles";
			ResultSet rs = stmt.executeQuery(query);

			// loop through result set, making a list of Rectangle objects
			ArrayList<Rectangle> rects = new ArrayList<>();
			while (rs.next()) {
				Rectangle rect = new Rectangle();
				rect.setId(rs.getInt("id"));
				rect.setName(rs.getString("name"));
				rect.setColor(rs.getString("color"));
				rect.setWidth(rs.getInt("width"));
				rect.setHeight(rs.getInt("height"));
				rects.add(rect);
			}

			// display contents of db in HTML table
			model.put("rects", rects);

			// debug af
			for (Rectangle rect : rects) {
				System.out.println(rect);
			}

			// have a blank rectangle ready for post requests
			Rectangle rectangle = new Rectangle();
			model.put("rectangle", rectangle);
			return "index";
		}

		catch (Exception e) {
			e.printStackTrace();
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
			String addRect = "INSERT INTO rectangles (name, color, width, height) VALUES " + values;
			stmt.executeUpdate(addRect);
			return "redirect:/"; // so that we run all of the code in @GetMapping("/")

		}

		catch (Exception e) {
			e.printStackTrace();
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
			System.out.printf("ID=%s\n", id);
			Statement stmt = connection.createStatement();
			stmt.executeUpdate(String.format("DELETE FROM rectangles WHERE id=%s", id));
			return "redirect:/"; // we go to a path well traveled ;)

		} catch (Exception e) {
			e.printStackTrace();
			model.put("message", e.getMessage());
			return "error";
		}
	}

	@GetMapping("/showrect/{id}")
	String rectangle(Map<String, Object> model, @PathVariable String id) {
		System.out.printf("Get request detected for id=%s", id);

		try (Connection connection = dataSource.getConnection()) {
			// create a rectangle object from sql table row
			Statement stmt = connection.createStatement();
			String query = String.format("SELECT * FROM rectangles WHERE id=%s", id);
			ResultSet rs = stmt.executeQuery(query);

			Rectangle showrect = new Rectangle();

			while (rs.next()) {
				showrect.setId(rs.getInt("id"));
				showrect.setName(rs.getString("name"));
				showrect.setColor(rs.getString("color"));
				showrect.setWidth(rs.getInt("width"));
				showrect.setHeight(rs.getInt("height"));
			}

			model.put("showrect", showrect);
			return "showrect";
		}

		catch (Exception e) {
			e.printStackTrace();
			model.put("message", e.getMessage());
			return "error";
		}
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

	// This is just for demonstration purposes,
	// not actually usefull for the rectangle app.
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
}
