package cphdat.entities;

import java.math.BigDecimal;

public record Top (
                      int Id,
                      String Name,
                      BigDecimal Price
) {}
