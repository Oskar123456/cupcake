package cphdat.entities;

import java.math.BigDecimal;

public record Payment (
                      int Id,
                      int OrderId,
                      String RecDate,
                      BigDecimal Amount
) {}
