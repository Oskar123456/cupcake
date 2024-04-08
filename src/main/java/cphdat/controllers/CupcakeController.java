package cphdat.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.jetbrains.annotations.NotNull;

import cphdat.entities.Bottom;
import cphdat.entities.Order;
import cphdat.entities.OrderFrontEnd;
import cphdat.entities.Top;
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
         */
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
         */
        app.post("logout", ctx -> logout(ctx, cp));
        app.post("order", ctx -> order(ctx, cp));
        app.post("login", ctx -> login(ctx, cp));
        app.post("createuser", ctx -> createUser(ctx, cp));
        app.post("myordersconfirm", ctx -> confirmOrder(ctx, cp));
        app.post("myordersdelete", ctx -> deleteMyOrder(ctx, cp));
        app.post("profileaddcredit", ctx -> profileAddCredit(ctx, cp));
        app.post("profiledeleteactiveorder", ctx -> profileDeleteActiveOrder(ctx, cp));
        app.post("profileacceptactiveorder", ctx -> profileAcceptActiveOrder(ctx, cp));
    }

    private static void logout(Context ctx, ConnectionPool cp)
    {
        ctx.sessionAttribute("user", null);
        ctx.sessionAttribute("admin", false);
        ctx.redirect("/");
    }

    private static void profileAcceptActiveOrder(@NotNull Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        if (!isAdmin(user))
            return;
        String selectedOrder = ctx.formParam("acceptactiveorder");
        int orderId = -1;
        try {
            orderId = Integer.parseInt(selectedOrder);
        }
        catch (NumberFormatException e){
            e.printStackTrace();
            ctx.redirect("/");
            return;
        }

        try {
            Order order = CupcakeMapper.GetOrderById(cp, orderId);
            CupcakeMapper.FinishOrder(cp, order);
            User reqUser = CupcakeMapper.GetUserById(cp, order.UserId());
            /**
             * handle not enough credits
             */
            List<Order> orderAsList = new ArrayList<>();
            orderAsList.add(order);
            List<BigDecimal> orderPriceAsList = CupcakeMapper.ListPrices(cp, orderAsList);
            BigDecimal orderPrice = orderPriceAsList.get(0).multiply(new BigDecimal(-1));
            CupcakeMapper.AddCredit(cp, reqUser, orderPrice);
            reqUser = CupcakeMapper.GetUserById(cp, order.UserId());
            renderProfilePage(ctx, cp, reqUser);
            return;
        }
        catch (DatabaseException e){
            e.printStackTrace();
        }
    }

    private static void profileDeleteActiveOrder(@NotNull Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        if (!isAdmin(user))
            return;
        String selectedOrder = ctx.formParam("deleteactiveorder");
        int orderId = -1;
        try {
            orderId = Integer.parseInt(selectedOrder);
        }
        catch (NumberFormatException e){
            e.printStackTrace();
            ctx.redirect("/");
            return;
        }

        try {
            Order order = CupcakeMapper.GetOrderById(cp, orderId);
            CupcakeMapper.DeleteOrder(cp, orderId);
            User reqUser = CupcakeMapper.GetUserById(cp, order.UserId());
            renderProfilePage(ctx, cp, reqUser);
            return;
        }
        catch (DatabaseException e){
            e.printStackTrace();
        }

    }

    private static void profileAddCredit(@NotNull Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        if (!isAdmin(user)) {
            ctx.attribute("message", "adgang nægtet");
            renderIndex(ctx, cp);
            return;
        }
        try {
            int reqUserId = Integer.parseInt(ctx.formParam("profileaddcreditid"));
            User reqUser = CupcakeMapper.GetUserById(cp, reqUserId);
            BigDecimal amount = new BigDecimal(ctx.formParam("profileaddcredit"));
            CupcakeMapper.AddCredit(cp, reqUser, amount);
            reqUser = CupcakeMapper.GetUserById(cp, reqUserId);
            renderProfilePage(ctx, cp, reqUser);

        } catch (DatabaseException | NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private static void deleteMyOrder(@NotNull Context ctx, ConnectionPool cp) {
        List<OrderFrontEnd> orders = ctx.sessionAttribute("pendingorders");
        int orderToRemove = -1;
        try {
            orderToRemove = Integer.parseInt(ctx.formParam("myordersdeleteid"));
        } catch (NumberFormatException e) {
        }
        if (orderToRemove >= 0)
            orders.remove(orderToRemove);
        ctx.sessionAttribute("pendingorders", orders);
        renderMyOrders(ctx, cp);
    }

    private static void confirmOrder(@NotNull Context ctx, ConnectionPool cp) {
        List<OrderFrontEnd> orders = ctx.sessionAttribute("pendingorders");
        User user = ctx.sessionAttribute("user");
        if (orders == null || orders.isEmpty() || user == null) {
            System.err.println("bestilling mislykkedes.");
            ctx.redirect("/");
            return;
        }

        List<Order> ordersDBFormat = new ArrayList<>();
        for (OrderFrontEnd order : orders) {
            ordersDBFormat.add(new Order(0, 0, null, null, null, order.BotId(), order.TopId(), order.Quant()));
        }
        try {
            for (Order order : ordersDBFormat) {
                CupcakeMapper.CreateOrder(cp, user, order, 60);
            }
            ctx.sessionAttribute("pendingorders", null);
        } catch (DatabaseException e) {
            System.err.println("ordretilføjelse gik galt?");
        }

        ctx.render("orderconfirm.html");
    }

    private static void renderProfile(@NotNull Context ctx, ConnectionPool cp) {
        String userEmail = ctx.queryParam("email");
        User currentUser = ctx.sessionAttribute("user");
        /* deny access */
        if (userEmail == null || userEmail.isEmpty() || currentUser == null) {
            ctx.attribute("message", "adgang nægtet");
            ctx.render("index.html");
            return;
        }
        /* happy */
        if (currentUser.Email().equals(userEmail) || isAdmin(currentUser)) {
            try {
                User reqUser = CupcakeMapper.GetUserByEmail(cp, userEmail);
                renderProfilePage(ctx, cp, reqUser);
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    private static void renderProfilePage(Context ctx, ConnectionPool cp, User reqUser) {
        User currentUser = ctx.sessionAttribute("user");
        if (reqUser != null) {
            List<Order> activeOrders;
            try {
                activeOrders = CupcakeMapper.ListOrders(cp, reqUser, true);
                List<Order> prevOrders = CupcakeMapper.ListOrders(cp, reqUser, false);

                List<BigDecimal> activeOrdersPrices = CupcakeMapper.ListPrices(cp, activeOrders);
                List<BigDecimal> prevOrdersPrices = CupcakeMapper.ListPrices(cp, prevOrders);

                BigDecimal activeOrdersTotal = new BigDecimal(0);
                BigDecimal prevOrdersTotal = new BigDecimal(0);

                for (BigDecimal price : activeOrdersPrices)
                    activeOrdersTotal = activeOrdersTotal.add(price);
                for (BigDecimal price : prevOrdersPrices)
                    prevOrdersTotal = prevOrdersTotal.add(price);

                List<String> activeOrdersTopNames = new ArrayList<>();
                List<String> activeOrdersBotNames = new ArrayList<>();
                for (Order order : activeOrders){
                    activeOrdersTopNames.add(CupcakeMapper.GetTopName(cp, order.Top()));
                    activeOrdersBotNames.add(CupcakeMapper.GetBotName(cp, order.Bot()));
                }
                List<String> prevOrdersTopNames = new ArrayList<>();
                List<String> prevOrdersBotNames = new ArrayList<>();
                for (Order order : prevOrders){
                    prevOrdersTopNames.add(CupcakeMapper.GetTopName(cp, order.Top()));
                    prevOrdersBotNames.add(CupcakeMapper.GetBotName(cp, order.Bot()));
                }

                ctx.attribute("requser", reqUser);
                ctx.attribute("activeorders", activeOrders);
                ctx.attribute("activeorderstopnames", activeOrdersTopNames);
                ctx.attribute("activeordersbotnames", activeOrdersBotNames);
                ctx.attribute("activeordersprices", activeOrdersPrices);
                ctx.attribute("activeorderstotal", activeOrdersTotal);
                ctx.attribute("prevorders", prevOrders);
                ctx.attribute("prevorderstopnames", prevOrdersTopNames);
                ctx.attribute("prevordersbotnames", prevOrdersBotNames);
                ctx.attribute("prevordersprices", prevOrdersPrices);
                ctx.attribute("prevorderstotal", prevOrdersTotal);
                ctx.attribute("isadmin", isAdmin(currentUser));
                ctx.render("profile.html");
            } catch (DatabaseException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        } else {
            ctx.attribute("message", "tilladelse nægtet");
            ctx.redirect("/");
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

        if (!success) {
            ctx.attribute("message", "kunne ikke oprette bruger");
            ctx.render("create.html");
        } else {
            ctx.attribute("message", "bruger oprettet (" + user.Email() + "), nu kan du logge ind");
            ctx.render("login.html");
        }
    }

    private static void login(@NotNull Context ctx, ConnectionPool cp) {
        User user = null;
        try {
            user = CupcakeMapper.GetUserByEmailPwd(cp, ctx.formParam("email"), ctx.formParam("pwd"));
            if (user != null)
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        if (user != null) {
            ctx.sessionAttribute("user", user);
            ctx.sessionAttribute("admin", isAdmin(user));
            ctx.redirect("/");
        } else {
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

    private static void renderAllOrders(@NotNull Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        if (!isAdmin(user))
            return;
        try {
            List<Order> activeOrders = CupcakeMapper.ListAllOrders(cp, true);
            List<Order> prevOrders = CupcakeMapper.ListAllOrders(cp, false);

            List<String> activeOrdersAccounts = new ArrayList<>();
            List<String> prevOrdersAccounts = new ArrayList<>();

            for (Order order : activeOrders){
                activeOrdersAccounts.add(CupcakeMapper.GetUserById(cp, order.UserId()).Email());
            }
            for (Order order : prevOrders){
                prevOrdersAccounts.add(CupcakeMapper.GetUserById(cp, order.UserId()).Email());
            }

            List<BigDecimal> activeOrdersPrices = CupcakeMapper.ListPrices(cp, activeOrders);
            List<BigDecimal> prevOrdersPrices = CupcakeMapper.ListPrices(cp, prevOrders);

            BigDecimal activeOrdersTotal = new BigDecimal(0);
            BigDecimal prevOrdersTotal = new BigDecimal(0);
            for (BigDecimal price : activeOrdersPrices)
                activeOrdersTotal = activeOrdersTotal.add(price);
            for (BigDecimal price : prevOrdersPrices)
                prevOrdersTotal = prevOrdersTotal.add(price);

            List<String> activeOrdersTopNames = new ArrayList<>();
            List<String> activeOrdersBotNames = new ArrayList<>();
            for (Order order : activeOrders){
                activeOrdersTopNames.add(CupcakeMapper.GetTopName(cp, order.Top()));
                activeOrdersBotNames.add(CupcakeMapper.GetBotName(cp, order.Bot()));
            }
            List<String> prevOrdersTopNames = new ArrayList<>();
            List<String> prevOrdersBotNames = new ArrayList<>();
            for (Order order : prevOrders){
                prevOrdersTopNames.add(CupcakeMapper.GetTopName(cp, order.Top()));
                prevOrdersBotNames.add(CupcakeMapper.GetBotName(cp, order.Bot()));
            }
            ctx.attribute("activeorders", activeOrders);
            ctx.attribute("activeordersaccounts", activeOrdersAccounts);
            ctx.attribute("activeorderstopnames", activeOrdersTopNames);
            ctx.attribute("activeordersbotnames", activeOrdersBotNames);
            ctx.attribute("activeordersprices", activeOrdersPrices);
            ctx.attribute("activeorderstotal", activeOrdersTotal);
            ctx.attribute("prevorders", prevOrders);
            ctx.attribute("prevordersaccounts", prevOrdersAccounts);
            ctx.attribute("prevorderstopnames", prevOrdersTopNames);
            ctx.attribute("prevordersbotnames", prevOrdersBotNames);
            ctx.attribute("prevordersprices", prevOrdersPrices);
            ctx.attribute("prevorderstotal", prevOrdersTotal);
            ctx.render("allorders.html");
        }
        catch (DatabaseException e){
            e.printStackTrace();
        }
    }

    private static void renderMyOrders(@NotNull Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        List<BigDecimal> prices = null;
        BigDecimal total = new BigDecimal(0);
        try {
            List<OrderFrontEnd> orders = ctx.sessionAttribute("pendingorders");
            if (orders != null && !orders.isEmpty()) {
                prices = CupcakeMapper.ListPricesFrontEnd(cp, orders);
                for (BigDecimal price : prices) {
                    total = total.add(price);
                }
            }
        } catch (DatabaseException e) {
        }

        ctx.attribute("pendingordersprices", prices);
        ctx.attribute("pendingorderstotal", total);
        ctx.attribute("isadmin", isAdmin(user));
        ctx.render("myorders.html");
    }

    private static void renderCustomers(@NotNull Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        if (!isAdmin(user)) {
            ctx.attribute("message", "adgang nægtet");
            renderIndex(ctx, cp);
            return;
        }
        try {
            List<User> users = CupcakeMapper.ListAllUsers(cp);
            ctx.attribute("customerlist", users);
            ctx.render("customers.html");
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private static void renderIndex(Context ctx, ConnectionPool cp) {

        List<Top> tops = null;
        List<Bottom> bots = null;
        try {
            tops = CupcakeMapper.ListTops(cp);
            bots = CupcakeMapper.ListBots(cp);
        } catch (DatabaseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        ctx.attribute("tops", tops);
        ctx.attribute("bots", bots);
        ctx.attribute("range", IntStream.range(1, 21).toArray());

        ctx.render("index.html");
    }

    private static void order(Context ctx, ConnectionPool cp) {
        User user = ctx.sessionAttribute("user");
        String topStr = ctx.formParam("top");
        String botStr = ctx.formParam("bot");
        String quantStr = ctx.formParam("quant");
        int top, bot, quant;
        try {
            top = Integer.parseInt(topStr);
            bot = Integer.parseInt(botStr);
            quant = Integer.parseInt(quantStr);
        } catch (NumberFormatException e) {
            top = -1;
            bot = -1;
            quant = 0;
        }
        if (user == null || quant < 1 || top < 0 || bot < 0) {
            ctx.attribute("message", "fejl i bestilling, er du logget på?");
            renderIndex(ctx, cp);
            return;
        }
        /*
         * handle order
         */
        List<Top> tops = null;
        List<Bottom> bots = null;
        try {
            tops = CupcakeMapper.ListTops(cp);
            bots = CupcakeMapper.ListBots(cp);
        } catch (DatabaseException e) {
        }

        OrderFrontEnd order = new OrderFrontEnd(bot, top,
                getBotName(bots, bot),
                getTopName(tops, top), quant);
        List<OrderFrontEnd> orders = ctx.sessionAttribute("pendingorders");
        if (orders == null)
            orders = new ArrayList<>();
        orders.add(order);
        ctx.sessionAttribute("pendingorders", orders);
        renderIndex(ctx, cp);
    }

    private static boolean isAdmin(User user) {
        return (user == null) ? false : user.Role().equals("admin");
    }

    private static String getTopName(List<Top> tops, int id) {
        return tops.get(id - 1).Name();
    }

    private static String getBotName(List<Bottom> bots, int id) {
        return bots.get(id - 1).Name();
    }

}
