package cphdat.entities;

import java.math.BigDecimal;

public record User (
                    int Id,
                    String Email,
                    String Pwd,
                    String Role,
                    BigDecimal Credit
) {}
