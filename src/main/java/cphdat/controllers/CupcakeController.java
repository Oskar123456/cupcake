package cphdat.controllers;

import java.math.BigDecimal;

import org.jetbrains.annotations.NotNull;

import cphdat.entities.User;
import cphdat.exceptions.DatabaseException;
import cphdat.persistence.ConnectionPool;
import cphdat.persistence.CupcakeMapper;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class CupcakeController {
    public static void addRoutes(Javalin app, ConnectionPool cp) {
        /*
         * get
         *  */
        app.get("/", ctx -> renderIndex(ctx, cp));
        app.get("/index", ctx -> renderIndex(ctx, cp));
        app.get("/index.html", ctx -> renderIndex(ctx, cp));

        app.get("/login", ctx -> renderLogin(ctx, cp));
        app.get("/login.html", ctx -> renderLogin(ctx, cp));

        app.get("/create", ctx -> renderCreate(ctx, cp));
        app.get("/create.html", ctx -> renderCreate(ctx, cp));

        app.get("/profile", ctx -> renderProfile(ctx, cp));
        app.get("/profile.html", ctx -> renderProfile(ctx, cp));

        app.get("/myorders", ctx -> renderMyOrders(ctx, cp));
        app.get("/myorders.html", ctx -> renderMyOrders(ctx, cp));

        app.get("/allorders", ctx -> renderAllOrders(ctx, cp));
        app.get("/allorders.html", ctx -> renderAllOrders(ctx, cp));

        app.get("/customers", ctx -> renderCustomers(ctx, cp));
        app.get("/customers.html", ctx -> renderCustomers(ctx, cp));
        /*
         * post
         *  */
        app.post("order", ctx -> order(ctx, cp));
        app.post("login", ctx -> login(ctx, cp));
        app.post("createuser", ctx -> createUser(ctx, cp));
    }

    private static void renderProfile(@NotNull Context ctx, ConnectionPool cp) {
        String userEmail = ctx.queryParam("email");
        User currentUser = ctx.sessionAttribute("user");
        if (userEmail == null || userEmail.isEmpty() || currentUser == null) {
            ctx.attribute("message", "adgang nægtet");
            ctx.render("index.html");
            return;
        }
        if (currentUser.Email().equals(userEmail) ||
            isAdmin(currentUser)){
            try {
                User reqUser = CupcakeMapper.GetUserByEmail(cp, userEmail);
                if (reqUser != null){
                    ctx.attribute("requser", reqUser);
                    ctx.render("profile.html");
                }
                else {
                    ctx.attribute("message", "tilladelse nægtet");
                    ctx.redirect("/");
                }
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static void createUser(@NotNull Context ctx, ConnectionPool cp) {
        // TODO: validate form params
        User user = new User(0, ctx.formParam("email"), ctx.formParam("pwd"), "customer", new BigDecimal(0));
        boolean success = false;

        try {
            success = CupcakeMapper.CreateUser(cp, user);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        if (!success){
            ctx.attribute("message", "kunne ikke oprette bruger");
            ctx.render("create.html");
        }
        else {
            ctx.attribute("message", "bruger oprettet (" + user.Email() + "), nu kan du logge ind");
            ctx.render("login.html");
        }
    }

    private static void login(@NotNull Context ctx, ConnectionPool cp) {
        User user = null;
        try {
            user = CupcakeMapper.GetUserByEmailPwd(cp, ctx.formParam("email"), ctx.formParam("pwd"));
            if (user != null)
                System.err.println("logged in as " + user.Email());
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (user != null){
            ctx.sessionAttribute("user", user);
            ctx.sessionAttribute("admin", isAdmin(user));
            ctx.redirect("/");
        }
        else{
            ctx.attribute("message", "fejl ved login");
            ctx.render("login.html");
        }

    }

    private static void renderLogin(@NotNull Context ctx, ConnectionPool cp) {
        ctx.render("login.html");
    }

    private static void renderCreate(@NotNull Context ctx, ConnectionPool cp) {
        ctx.render("create.html");
    }

    private static Object renderAllOrders(@NotNull Context ctx, ConnectionPool cp) {
        return null;
    }

    private static Object renderMyOrders(@NotNull Context ctx, ConnectionPool cp) {
        return null;
    }

    private static Object renderCustomers(@NotNull Context ctx, ConnectionPool cp) {
        return null;
    }

    private static void renderIndex(Context ctx, ConnectionPool cp) {
        System.err.println("session attribute user: " + ctx.sessionAttribute("user"));
        ctx.render("index.html");
    }

    private static void order(Context ctx, ConnectionPool cp){
        /*
         * handle order
         *  */

        ctx.redirect("/");
    }

    private static boolean isAdmin(User user){
        return user.Role().equals("admin");
    }

}
