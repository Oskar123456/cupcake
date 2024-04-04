package cphdat.controllers;

import cphdat.persistence.ConnectionPool;
import io.javalin.Javalin;
import io.javalin.http.Context;

public class CupcakeController {
    public static void addRoutes(Javalin app, ConnectionPool cp) {
        app.get("/", ctx -> renderIndex(ctx, cp));
        app.get("/index.html", ctx -> renderIndex(ctx, cp));
        app.post("order", ctx -> order(ctx, cp));
    }

    private static void renderIndex(Context ctx, ConnectionPool cp) {
        ctx.render("index.html");
    }

    private static void order(Context ctx, ConnectionPool cp){
        /*
         * handle order
         *  */

        ctx.redirect("/");
    }

}
