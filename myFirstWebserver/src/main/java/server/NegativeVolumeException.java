package server;

import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
class NegativeVolumeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public NegativeVolumeException(Double volume, String symbol) {
        super("The amount of stock '" + volume + "'" + " for symbol '" + symbol + "' is not valid.");
    }
}