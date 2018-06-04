package server;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Map;
import pl.zankowski.iextrading4j.api.stocks.Ohlc;

/**
 * This class takes chunks from the queue, writes them to disk and updates the file's metadata.
 *
 * NOTE: make sure that the file interface you choose writes every update to the file's content or metadata
 *       synchronously to the underlying storage device.
 */
public class backupFiles {

    private String backupPortfolioFileName;
    private String backupIDFileName;
    
    backupFiles() {

        this.backupPortfolioFileName = "portfolios.txt";
        this.backupIDFileName = "lastID.txt";
    }

    public void updatePortfoliosBackup(ConcurrentHashMap<Long,ConcurrentHashMap<String, Double>> portfolios) throws InterruptedException, IOException {
            
        FileOutputStream fout = new FileOutputStream(backupPortfolioFileName);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(portfolios);
        oos.close();
    }
    public ConcurrentHashMap<Long,ConcurrentHashMap<String, Double>> readPortfoliosBackup() {
        // read object from file
        FileInputStream fis;
        try {
            fis = new FileInputStream(backupPortfolioFileName);
        } catch (FileNotFoundException e) {
            
            return new ConcurrentHashMap<Long,ConcurrentHashMap<String, Double>>();
        }
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            ConcurrentHashMap<Long,ConcurrentHashMap<String, Double>> portfolios = (ConcurrentHashMap<Long,ConcurrentHashMap<String, Double>>) ois.readObject();
            ois.close();
            return portfolios;
        } catch (ClassNotFoundException | IOException e) {

            return new ConcurrentHashMap<Long,ConcurrentHashMap<String, Double>>();
        }
    }
    public void writeLastID(Long lastID) throws IOException {
        FileOutputStream fout = new FileOutputStream(backupIDFileName);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(lastID);
        oos.close();
    }
    public Long readLastID() {
        // read object from file
        FileInputStream fis;
        try {
            fis = new FileInputStream(backupIDFileName);
        } catch (Exception e) {
            return new Long(0);
        }
        Long lastID;
        try {
            ObjectInputStream ois = new ObjectInputStream(fis);
            lastID = (Long) ois.readObject();
            ois.close();
            return lastID;
        } catch (ClassNotFoundException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new Long(0);
        }
    }
}
