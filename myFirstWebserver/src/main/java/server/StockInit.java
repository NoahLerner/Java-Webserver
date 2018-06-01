package server;

import java.util.Objects;

public class StockInit {

    private String symbol;
    private Double volume;
    
    public StockInit(String name, Double volume) {

        this.symbol = name;
        this.volume = volume;
    }
    
    public StockInit() {
        
    }
    
    public String getSymbol() {
        return this.symbol;   
    }

    public Double getVolume() {
        return this.volume;
    }
    
    public void setSymbol(String name) {
        this.symbol = name;
    }
    
    public void setVolume(Double volume) {
        this.volume = volume;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof StockInit)) {
            return false;
        }
        StockInit stock = (StockInit) o;
        return symbol == stock.symbol &&
                volume == stock.volume &&
                Objects.equals(symbol, stock.symbol) &&
                Objects.equals(volume, stock.volume);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, volume);
    }
}
