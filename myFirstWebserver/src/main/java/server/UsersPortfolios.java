package server;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pl.zankowski.iextrading4j.api.stocks.Chart;
import pl.zankowski.iextrading4j.api.stocks.ChartRange;
import pl.zankowski.iextrading4j.api.stocks.Ohlc;
import pl.zankowski.iextrading4j.client.IEXTradingClient;
import pl.zankowski.iextrading4j.client.rest.request.stocks.ChartRequestBuilder;
import pl.zankowski.iextrading4j.client.rest.request.stocks.OhlcMarketRequestBuilder;

@RestController
public class UsersPortfolios {

    private final backupFiles backup = new backupFiles();
    private final AtomicLong counter = new AtomicLong(backup.readLastID());
    
    // Our database will be a nested map where each user (identified by their ID) has a portfolio
    //      represented by a map of symbols and the volume
    private ConcurrentHashMap<Long, ConcurrentHashMap<String, Double>> portfolios = backup.readPortfoliosBackup();
    private final IEXTradingClient iexTradingClient = IEXTradingClient.create();
    
    // the market snapshot at time timestamp
    private Map<String, Ohlc> marketSnapshot = iexTradingClient.executeRequest(new OhlcMarketRequestBuilder().build());
    LocalDateTime timestamp = LocalDateTime.now().minusMinutes(1);

    
    
    // change to a POST method
    @RequestMapping(value = "/newuser", method = RequestMethod.POST)
    public @ResponseBody Long createNewUser(@RequestBody ArrayList<StockInit> portfolio) {
                        
        Iterator<StockInit> iter = portfolio.iterator();
        
        ConcurrentHashMap<String, Double> stocks = new ConcurrentHashMap<>();
        StockInit stockInit;

        while(iter.hasNext()) {
            stockInit = iter.next();
            stockInit.setSymbol(stockInit.getSymbol().toUpperCase());
            this.validateSymbol(stockInit.getSymbol());
            this.validateVolume(stockInit.getVolume(), stockInit.getSymbol());
            stocks.put(stockInit.getSymbol(), stockInit.getVolume());
        }
        
        // give the user an ID & enter him into the database only after we have his portfolio
        Long newUser = counter.incrementAndGet();
        this.portfolios.put(newUser, stocks);

        try {
            backup.writeLastID(newUser);
            backup.updatePortfoliosBackup(portfolios);
        } catch (IOException | InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Huge error");
        }
        this.validateUser(newUser);
        return newUser;
    }
    
    /*
     * To keep latency down, the getvalue method will consult a previous snapshot of the market and
     *      not consult the IEX API. To update the snapshot, execute a buying recommendation.
     */
    @RequestMapping(value="/existinguser/getvalue", method=RequestMethod.GET)
    public @ResponseBody Object getValuleOfPortfolio(@RequestParam(value = "clientID", required = true) Long clientID) {        
        
        this.validateUser(clientID);
        
        ConcurrentHashMap<String, Double> portfolio = this.portfolios.get(clientID);
        Double value = (double)0;
        
        Iterator<Entry<String, Double>> iter = portfolio.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<String, Double> pair = iter.next();
            String stockSymbol = pair.getKey();
            Double stockVolume = pair.getValue();
            value += marketSnapshot.get(stockSymbol).getClose().getPrice().doubleValue()*stockVolume;
        }
                                
        // TODO: call persistence layer to update

        return value;
    }
    
    // change to a POST method
    @RequestMapping(value="/existinguser/updatestocks", method=RequestMethod.POST)
    public @ResponseBody Object updateStocks(@RequestParam(value = "clientID", required = true) Long clientID,
                                @RequestBody ArrayList<StockInit> portfolio){
        
        // TODO: call persistence layer to update

        this.validateUser(clientID);
                
        Iterator<StockInit> iter = portfolio.iterator();
        ConcurrentHashMap<String, Double> stocks = new ConcurrentHashMap<>();
        StockInit stockInit;

        while(iter.hasNext()) {
            stockInit = iter.next();
            stockInit.setSymbol(stockInit.getSymbol().toUpperCase());
            this.validateSymbol(stockInit.getSymbol());
            this.validateVolume(stockInit.getVolume(), stockInit.getSymbol());
            stocks.put(stockInit.getSymbol(), stockInit.getVolume());
        }
        
        this.portfolios.replace(clientID, stocks);
        // TODO: call persistence layer to update
        try {
            backup.updatePortfoliosBackup(portfolios);
        } catch (InterruptedException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            System.out.println("Portfolio not updated");
        }

        return clientID;
    }
      
    @RequestMapping(value="/buying/performance", method=RequestMethod.GET)
    public @ResponseBody String buyingRecPerformance(@RequestParam Long clientID) {
        
        this.validateUser(clientID);
        updateMarket();
        
        Double gainOverTime = -Double.MAX_VALUE;
        Double maxGain = -Double.MAX_VALUE;
        String performanceSymbol = "";
        List<Chart> chart;
        
        ConcurrentHashMap<String, Double> portfolio = portfolios.get(clientID);
        Iterator<Entry<String, Double>> iter = portfolio.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<String, Double> pair = iter.next();
            String stockSymbol = pair.getKey();
            
            // chart is the minute by minute value of the stock 5 business days prior
            chart = iexTradingClient.executeRequest(new ChartRequestBuilder()
                    .withSymbol(stockSymbol)
                    .withDate(LocalDate.now().minusDays(5))
                    .build());
            
            if(chart.get(0) == null) {
                return "There was a failure on our end";
            }
//            System.out.println(LocalDate.now().minusDays(6));
//            System.out.println(stockSymbol);
//            System.out.println(chart.get(0));
//            System.out.println(chart.get(0).getMarketOpen());
//            
            gainOverTime = marketSnapshot.get(stockSymbol).getClose().getPrice().doubleValue()
                    - chart.get(0).getMarketOpen().doubleValue();
            
            if(maxGain < gainOverTime) {
                maxGain = gainOverTime;
                performanceSymbol = stockSymbol;
            }
        }
        return performanceSymbol;
    }
    
    @RequestMapping(value="/buying/stability", method=RequestMethod.GET)
    public @ResponseBody String buyingRecStability(@RequestParam Long clientID) {
        
        this.validateUser(clientID);
        updateMarket();
        
        // stabilityFactor tracks the difference between the high and low
        //      a value closer to 0 is more "stable"
        Double stabilityFactor = Double.MAX_VALUE;
        Double maxStability = Double.MAX_VALUE;
        String mostStable = "";
        List<Chart> chart;
        
        ConcurrentHashMap<String, Double> portfolio = portfolios.get(clientID);
        Iterator<Entry<String, Double>> iterPortofolio = portfolio.entrySet().iterator();
        while(iterPortofolio.hasNext()) {
            Map.Entry<String, Double> pair = iterPortofolio.next();
            String stockSymbol = pair.getKey();
            
            // chart is day by day value of the stock for the last 5 business days
            chart = iexTradingClient.executeRequest(new ChartRequestBuilder()
                    .withSymbol(stockSymbol)
                    .withChartRange(ChartRange.ONE_MONTH)
                    .withChartLast(5)
                    .build());
            
            Iterator<Chart> iterChart = chart.iterator();
            Chart day;
            Double minValue = Double.MAX_VALUE;
            Double maxValue = Double.MIN_VALUE;
            
            // finds the min and max values for this stock over the last 5 business days
            while(iterChart.hasNext()) {
                day = iterChart.next();
                if(day.getLow().doubleValue() < minValue) {
                    minValue = day.getLow().doubleValue();
                }
                if(day.getHigh().doubleValue() > maxValue) {
                    maxValue = day.getHigh().doubleValue();
                }
            }
            
            stabilityFactor = Math.abs(maxValue - minValue);
            
            if(stabilityFactor < maxStability) {
                maxStability = stabilityFactor;
                mostStable = stockSymbol;
            }
        }
       
        
        return mostStable;
    }
    
    @RequestMapping(value="/buying/best", method=RequestMethod.GET)
    public @ResponseBody String buyingRecBest(@RequestParam Long clientID) {
        
        this.validateUser(clientID);
        updateMarket();
        
        String bestStock = "";
        Double highestValue = -Double.MAX_VALUE;

        String stock;
        Double stockValue = -Double.MAX_VALUE;

        
        Iterator<String> iter = marketSnapshot.keySet().iterator();
        while(iter.hasNext()) {
            stock = iter.next();
            if(portfolios.get(clientID).containsKey(stock)) {
                // skip
            } else {
                if(stock == "[object Object]") break;
                
                // try/catch is because not all stocks have open&close prices, but all have at least one
                try {
                    stockValue = marketSnapshot.get(stock).getClose().getPrice().doubleValue();
                } catch(NullPointerException e){
                    stockValue = marketSnapshot.get(stock).getOpen().getPrice().doubleValue();
                }
                if(highestValue < stockValue) {
                    highestValue = stockValue;
                    bestStock = stock;
                }
            }
        }
        return bestStock;
    }
    
    
    private void validateUser(Long userId) {
        if(!this.portfolios.containsKey(userId)) {
            throw new UserNotFoundException(userId);
        }
    }
    
    private void validateSymbol(String symbol) {
        if(!this.marketSnapshot.containsKey(symbol)) {
            throw new StockNotFoundException(symbol);
        }
    }
    
    private void validateVolume(Double volume, String symbol) {
        if(volume < 0) {
            throw new NegativeVolumeException(volume, symbol);
        }
    }
    
    /*
     * We will only update our market snapshot if we haven't done so in at least 1 minute
     */
    private void updateMarket() {
        
        if(LocalDateTime.now().minusMinutes(1).isAfter(timestamp)) {
            marketSnapshot = iexTradingClient.executeRequest(new OhlcMarketRequestBuilder().build());
            timestamp = LocalDateTime.now();
        }
    }
    
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid JSON representation of a stock portfolio")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public void handleJSONException() {
        
    }
    
    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Invalid numerical representation of a client ID")
    @ExceptionHandler(NumberFormatException.class)
    public void handleClientIDException() {
        
    }
    
    /*
     * To implement, edit 
     */
    @ResponseStatus(value = HttpStatus.NOT_FOUND, reason = "This is not a valid page")
    @ExceptionHandler(Exception.class)
    public void handleNotAValidPageException() {
        
    }
    
    
    // Whitelabel unsupported mediatype exceptions are relevant and handy as they are
    

}
