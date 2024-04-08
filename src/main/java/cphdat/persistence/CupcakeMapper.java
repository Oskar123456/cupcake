package cphdat.persistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cphdat.entities.Bottom;
import cphdat.entities.Order;
import cphdat.entities.OrderFrontEnd;
import cphdat.entities.Top;
import cphdat.entities.User;
import cphdat.exceptions.DatabaseException;

public class CupcakeMapper {
    /*
     * User
     * */
    public static List<User> ListAllUsers(ConnectionPool cp) throws DatabaseException{
        List<User> retval = new ArrayList<>();

        String sql = "SELECT * FROM users";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                retval.add(new User(rs.getInt("id"),
                                  rs.getString("email"),
                                  rs.getString("pwd"),
                                  rs.getString("role"),
                                  rs.getBigDecimal("credit")));
            }

        } catch (SQLException e) {
            throw new DatabaseException("fejl ifb. med at hente alle brugere");
        }

        return retval;
    }

    public static int AddCredit(ConnectionPool cp, User user, BigDecimal amount) throws DatabaseException{
        int retval = 0;

        String sql = "UPDATE users SET credit = credit::numeric + ? WHERE id=?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);
            ps.setInt(2, user.Id());
            retval = ps.executeUpdate();

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved indsættelse af penge på konto " + user.Email());
        }

        return retval;

    }


    public static User GetUserById(ConnectionPool cp, int id) throws DatabaseException{
        User retval = null;

        String sql = "SELECT * FROM users WHERE id = ?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                retval = new User(rs.getInt("id"),
                                  rs.getString("email"),
                                  rs.getString("pwd"),
                                  rs.getString("role"),
                                  rs.getBigDecimal("credit"));
            }

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved søgning af bruger med id(" + id + ")");
        }

        return retval;
    }



    public static User GetUserByEmail(ConnectionPool cp, String email) throws DatabaseException{
        User retval = null;

        String sql = "SELECT * FROM users WHERE email = ?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                retval = new User(rs.getInt("id"),
                                  rs.getString("email"),
                                  rs.getString("pwd"),
                                  rs.getString("role"),
                                  rs.getBigDecimal("credit"));
            }

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved søgning af bruger med email(" + email + ")");
        }

        return retval;
    }

    public static User GetUserByEmailPwd(ConnectionPool cp, String email, String pwd) throws DatabaseException{
        User retval = null;

        String sql = "SELECT * FROM users WHERE email = ? AND pwd = ?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, pwd);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){

                retval = new User(rs.getInt("id"),
                                  rs.getString("email"),
                                  rs.getString("pwd"),
                                  rs.getString("role"),
                                  rs.getBigDecimal("credit"));
            }

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved søgning af bruger med email og kodeord(" + email + "," + pwd + ")");
        }

        return retval;
    }

    public static boolean CreateUser(ConnectionPool cp, User user) throws DatabaseException {
        boolean retval = false;

        String sql = "INSERT INTO users(id, email, pwd, role, credit) VALUES (DEFAULT, ?, ?, ?, ?)";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.Email());
            ps.setString(2, user.Pwd());
            ps.setString(3, user.Role());
            ps.setBigDecimal(4, new BigDecimal(0));
            int success = ps.executeUpdate();
            if (success == 1)
                retval = true;

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved oprettelse af bruger");
        }

        return retval;
    }

    /*
     * orders
     */

    public static List<Order> ListAllOrders(ConnectionPool cp, boolean onlyActiveOrders) throws DatabaseException{
        List<Order> orders = new ArrayList<>();

        String sql = "SELECT * FROM orders WHERE finish_date IS ";
        if (onlyActiveOrders)
            sql += "NULL";
        else
            sql += "NOT NULL";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                String createDate = rs.getString("create_date");
                String desiredDate = rs.getString("desired_date");
                String finishDate = rs.getString("finish_date");
                createDate = createDate.split("\\.")[0];
                desiredDate = desiredDate.split("\\.")[0];
                if (finishDate != null && !finishDate.isEmpty())
                    finishDate = finishDate.split("\\.")[0];
                orders.add(new Order(rs.getInt("id"),
                                     rs.getInt("customer_id"),
                                     createDate,
                                     desiredDate,
                                     finishDate,
                                     rs.getInt("bot"),
                                     rs.getInt("top"),
                                     rs.getInt("quant")));
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }

        return orders;
    }




    public static List<Order> ListOrders(ConnectionPool cp, User user, boolean onlyActiveOrders) throws DatabaseException{
        List<Order> orders = new ArrayList<>();

        String sql = "SELECT * FROM orders WHERE customer_id = ? AND finish_date IS ";
        if (onlyActiveOrders)
            sql += "NULL";
        else
            sql += "NOT NULL";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, user.Id());
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                String createDate = rs.getString("create_date");
                String desiredDate = rs.getString("desired_date");
                String finishDate = rs.getString("finish_date");
                createDate = createDate.split("\\.")[0];
                desiredDate = desiredDate.split("\\.")[0];
                if (finishDate != null && !finishDate.isEmpty())
                    finishDate = finishDate.split("\\.")[0];
                orders.add(new Order(rs.getInt("id"),
                                     rs.getInt("customer_id"),
                                     createDate,
                                     desiredDate,
                                     finishDate,
                                     rs.getInt("bot"),
                                     rs.getInt("top"),
                                     rs.getInt("quant")));
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }

        return orders;
    }

    public static Order GetOrderById(ConnectionPool cp, int orderId) throws DatabaseException{

        String sql = "SELECT * FROM orders WHERE id = ?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                String createDate = rs.getString("create_date");
                String desiredDate = rs.getString("desired_date");
                String finishDate = rs.getString("finish_date");
                createDate = createDate.split("\\.")[0];
                desiredDate = desiredDate.split("\\.")[0];
                if (finishDate != null && !finishDate.isEmpty())
                    finishDate = finishDate.split("\\.")[0];
                return new Order(rs.getInt("id"),
                                     rs.getInt("customer_id"),
                                     createDate,
                                     desiredDate,
                                     finishDate,
                                     rs.getInt("bot"),
                                     rs.getInt("top"),
                                     rs.getInt("quant"));
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }

        return null;
    }



    public static boolean CreateOrder(ConnectionPool cp, User user, Order order, int timeInMins) throws DatabaseException {
        boolean retval = false;

        String sql = "INSERT INTO public.orders(id, customer_id, create_date, desired_date, finish_date, bot, top, quant) VALUES (DEFAULT, ?, ?, ?, NULL, ?, ?, ?)";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, user.Id());
            ps.setTimestamp(2, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setTimestamp(3, new java.sql.Timestamp(System.currentTimeMillis() + timeInMins * 60000));
            ps.setInt(4, order.Bot());
            ps.setInt(5, order.Top());
            ps.setInt(6, order.Quant());
            System.err.println(ps);
            int success = ps.executeUpdate();
            if (success == 1)
                retval = true;

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved oprettelse af ordre");
        }

        return retval;
    }

    public static boolean FinishOrder(ConnectionPool cp, Order order) throws DatabaseException {
        boolean retval = false;

        String sql = "UPDATE public.orders SET finish_date=? WHERE id=?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setTimestamp(1, new java.sql.Timestamp(System.currentTimeMillis()));
            ps.setInt(2, order.Id());
            int success = ps.executeUpdate();
            if (success == 1)
                retval = true;

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved færdiggørelse af ordre");
        }

        return retval;
    }

    public static boolean DeleteOrder(ConnectionPool cp, int orderId) throws DatabaseException {
        boolean retval = false;

        String sql = "DELETE FROM public.orders WHERE id=?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            int success = ps.executeUpdate();
            if (success == 1)
                retval = true;

        } catch (SQLException e) {
            throw new DatabaseException("fejl ved færdiggørelse af ordre");
        }

        return retval;
    }



    public static List<BigDecimal> ListPrices(ConnectionPool cp, List<Order> orders) throws DatabaseException{
        List<BigDecimal> prices = new ArrayList<>();
        try {
            List<Top> tops = ListTops(cp);
            List<Bottom> bots = ListBots(cp);
            for (Order order : orders){
                BigDecimal topPrice = tops.get(order.Top() - 1).Price();
                BigDecimal botPrice = bots.get(order.Bot() - 1).Price();
                BigDecimal quant = BigDecimal.valueOf(order.Quant());
                prices.add((topPrice.add(botPrice)).multiply(quant));
            }
        }
        catch (DatabaseException e) {
            throw new DatabaseException("fejl ved prissøgning");
        }
        return prices;
    }


    public static List<BigDecimal> ListPricesFrontEnd(ConnectionPool cp, List<OrderFrontEnd> orders) throws DatabaseException{
        List<BigDecimal> prices = new ArrayList<>();
        try {
            List<Top> tops = ListTops(cp);
            List<Bottom> bots = ListBots(cp);
            for (OrderFrontEnd order : orders){
                BigDecimal topPrice = tops.get(order.TopId() - 1).Price();
                BigDecimal botPrice = bots.get(order.BotId() - 1).Price();
                BigDecimal quant = BigDecimal.valueOf(order.Quant());
                prices.add((topPrice.add(botPrice)).multiply(quant));
            }
        }
        catch (DatabaseException e) {
            throw new DatabaseException("fejl ved prissøgning");
        }
        return prices;
    }



    /*
     * cupcake options
     */
    public static List<Top> ListTops(ConnectionPool cp) throws DatabaseException{
        List<Top> tops = new ArrayList<>();

        String sql = "SELECT * FROM top ORDER BY id ASC";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                tops.add(new Top(rs.getInt("id"),
                                 rs.getString("name"),
                                 rs.getBigDecimal("price")));
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }

        return tops;
    }

    public static List<Bottom> ListBots(ConnectionPool cp) throws DatabaseException{
        List<Bottom> bottoms = new ArrayList<>();

        String sql = "SELECT * FROM bottom ORDER BY id ASC";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                bottoms.add(new Bottom(rs.getInt("id"),
                                 rs.getString("name"),
                                 rs.getBigDecimal("price")));
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }

        return bottoms;
    }

    public static String GetTopName(ConnectionPool cp, int id) throws DatabaseException{
        String sql = "SELECT name FROM top WHERE id = ?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getString("name");
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }
        return "placeholder";
    }

    public static String GetBotName(ConnectionPool cp, int id) throws DatabaseException{
        String sql = "SELECT name FROM bottom WHERE id = ?";

        try (
                Connection c = cp.getConnection();
                PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getString("name");
            }

        }
        catch (SQLException e) {
            throw new DatabaseException("fejl ved ordresøgning");
        }
        return "placeholder";
    }
}
