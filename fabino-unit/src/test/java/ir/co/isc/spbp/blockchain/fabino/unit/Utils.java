package ir.co.isc.spbp.blockchain.fabino.unit;

import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class Utils {

    public static void waitForCommit() {
        try {
            TimeUnit.MILLISECONDS.sleep(2_100L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
