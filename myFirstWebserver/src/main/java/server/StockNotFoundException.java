package server;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
class StockNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StockNotFoundException(String symbol) {
        super("The symbol '" + symbol + "' is not a valid stock on the market.");
    }
}