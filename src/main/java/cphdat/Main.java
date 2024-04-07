package cphdat;

import java.math.BigDecimal;
import java.util.Random;

import cphdat.config.ThymeleafConfig;
import cphdat.controllers.CupcakeController;
import cphdat.entities.User;
import cphdat.exceptions.DatabaseException;
import cphdat.persistence.ConnectionPool;
import cphdat.persistence.CupcakeMapper;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;

public class Main
{
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static final String URL = "jdbc:postgresql://localhost:5432/%s?currentSchema=public";
    private static final String DB = "cupcake";

    private static final ConnectionPool connectionPool = ConnectionPool.getInstance(USER, PASSWORD, URL, DB);

    public static void main(String[] args)
    {
        // Initializing Javalin and Jetty webserver

        Javalin jav = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.templateEngine()));
        }).start(7070);

        // Routing
        CupcakeController.addRoutes(jav, connectionPool);

        // Random rnd = new Random();
        // User user = new User(0, "obh" + rnd.nextInt() + "@mail.dk", "1234", "customer", new BigDecimal(0));
        // boolean createusersuccess;
        // try {
        //     createusersuccess = CupcakeMapper.CreateUser(connectionPool, user);
        //     System.out.printf("create user (%s) : %b%n", user.Email(), createusersuccess);
        //     System.out.println("fetch user");
        //     System.out.println(CupcakeMapper.GetUserByEmail(connectionPool, user.Email()));
        //     System.out.println(CupcakeMapper.GetUserByEmailPwd(connectionPool, user.Email(), user.Pwd()));
        // } catch (DatabaseException e) {
        //     // TODO Auto-generated catch block
        //     e.printStackTrace();
        // }
    }
}
