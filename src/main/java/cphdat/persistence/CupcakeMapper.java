package cphdat.persistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import cphdat.entities.User;
import cphdat.exceptions.DatabaseException;

public class CupcakeMapper {
    /*
     * User
     * */
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
}
